package pl.net.bluesoft.rnd.pt.ext.jbpm.service;

import static pl.net.bluesoft.rnd.processtool.ProcessToolContext.Util.getThreadProcessToolContext;
import static pl.net.bluesoft.util.lang.Strings.hasText;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.io.IOUtils;
import org.drools.KnowledgeBase;
import org.drools.SystemEventListenerFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;
import org.drools.impl.EnvironmentFactory;
import org.drools.io.ResourceFactory;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.process.audit.JPAWorkingMemoryDbLogger;
import org.jbpm.process.workitem.wsht.LocalHTWorkItemHandler;
import org.jbpm.task.Task;
import org.jbpm.task.User;
import org.jbpm.task.event.TaskEventListener;
import org.jbpm.task.event.entity.TaskUserEvent;
import org.jbpm.task.identity.UserGroupCallbackManager;
import org.jbpm.task.service.ContentData;
import org.jbpm.task.service.local.LocalTaskService;
import org.jbpm.task.utils.OnErrorAction;

import pl.net.bluesoft.rnd.processtool.IProcessToolSettings;
import pl.net.bluesoft.rnd.pt.ext.jbpm.JbpmStepAction;
import pl.net.bluesoft.rnd.pt.ext.jbpm.ProcessResourceNames;
import pl.net.bluesoft.rnd.pt.ext.jbpm.service.query.TaskQuery;
import pl.net.bluesoft.rnd.pt.ext.jbpm.service.query.UserQuery;
import bitronix.tm.TransactionManagerServices;

public class JbpmService implements ProcessEventListener, TaskEventListener {

    protected Logger log = Logger.getLogger(JbpmService.class.getName());

    private static final int MAX_PROC_DEF_LENGTH = 1024;
    private static final IProcessToolSettings KSESSION_ID = new IProcessToolSettings() {
        @Override
        public String toString() {
            return "ksession.id";
        }
    };
    private static final IProcessToolSettings JBPM_REPOSITORY_DIR = new IProcessToolSettings() {
        @Override
        public String toString() {
            return "jbpm.repository.dir";
        }
    };

    private EntityManagerFactory emf;
    private Environment env;
    private org.jbpm.task.service.TaskService taskService;
    private org.jbpm.task.TaskService client;
    private StatefulKnowledgeSession ksession;
    private JbpmRepository repository;

    private static JbpmService instance;

    public static JbpmService getInstance() {
        if (instance == null) {
            synchronized (JbpmService.class) {
                if (instance == null) {
                    instance = new JbpmService();
                }
            }
        }
        return instance;
    }

    public void init() {
        initEntityManager();
        initEnvironment();
        initClient();
    }

    public void destroy() {
        if (ksession != null) {
            ksession.dispose();
        }
    }

    private void initEntityManager() {
        emf = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
    }

    private void initEnvironment() {
        env = EnvironmentFactory.newEnvironment();
        env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
        env.set(EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager());
    }

    private void initClient() {
        taskService = new org.jbpm.task.service.TaskService(emf, SystemEventListenerFactory.getSystemEventListener());
        UserGroupCallbackManager.getInstance().setCallback(new AwfUserCallback());

        LocalTaskService localTaskService = new LocalTaskService(taskService);
        localTaskService.setEnvironment(env);
        localTaskService.addEventListener(this);
        client = localTaskService;
    }

    public synchronized String addProcessDefinition(InputStream definitionStream) {
        String result = null;
        byte[] bytes;

        try {
            bytes = IOUtils.toByteArray(definitionStream);
            definitionStream = new ByteArrayInputStream(bytes);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (isValidResource(bytes)) {
            // add to jbpm repository
            if (getRepository() != null) {
                result = getRepository().addResource(ProcessResourceNames.DEFINITION, definitionStream);
            }

            // update session
            if (ksession != null) {
                KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
                kbuilder.add(ResourceFactory.newByteArrayResource(bytes), ResourceType.BPMN2);
                ksession.getKnowledgeBase().addKnowledgePackages(kbuilder.getKnowledgePackages());
            }
        }

        return result;
    }

    private KnowledgeBase getKnowledgeBase() {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

        try {
			Thread.currentThread().getContextClassLoader().loadClass(JbpmStepAction.class.getName());
		} catch (ClassNotFoundException e) {
			log.warning("JbpmStepAction.class was not found");
		}
        
        if (getRepository() != null) {
            for (byte[] resource : getValidResources()) {
                kbuilder.add(ResourceFactory.newByteArrayResource(resource), ResourceType.BPMN2);
            }
        }

        return kbuilder.newKnowledgeBase();
    }
    
    private List<byte[]> getValidResources() {
    	List<byte[]> validResources = new ArrayList<byte[]>();
        for (byte[] resource : getRepository().getAllResources("bpmn")) {
    		if (isValidResource(resource)) validResources.add(resource);
        }
        return validResources;
    }

    private boolean isValidResource(byte[] resource) {
    	KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
    	kbuilder.add(ResourceFactory.newByteArrayResource(resource), ResourceType.BPMN2);
    	boolean isOK = true;
    	try {
			kbuilder.newKnowledgeBase();
		} catch (Exception e) {
			isOK = false;
			log.info("The following process definition contains errors and was not loaded:\n" + new String(resource).substring(0, Math.min(MAX_PROC_DEF_LENGTH, resource.length-1)) + "...");
		}
		return isOK;
    }
    
    private void loadSession(int sessionId) {
        KnowledgeBase kbase = getKnowledgeBase();

        if (sessionId == -1) {
            ksession = JPAKnowledgeService.newStatefulKnowledgeSession(kbase, null, env);
        } else {
            ksession = JPAKnowledgeService.loadStatefulKnowledgeSession(sessionId, kbase, null, env);
            //ksession.signalEvent("Trigger", null); // may be necessary for pushing processes after server restart
        }

        LocalHTWorkItemHandler handler = new LocalHTWorkItemHandler(client, ksession, OnErrorAction.LOG);
        handler.connect();
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", handler);
        new JPAWorkingMemoryDbLogger(ksession);
        ksession.addEventListener(this);
    }

    private StatefulKnowledgeSession getSession() {
        if (ksession == null) {
            synchronized(JbpmService.class) {
                if (ksession == null) {
                    String ksessionIdStr = getThreadProcessToolContext().getSetting(KSESSION_ID);
                    int ksessionId = hasText(ksessionIdStr) ? Integer.parseInt(ksessionIdStr) : -1;

                    loadSession(ksessionId);

                    if (ksessionId <= 0) {
                        getThreadProcessToolContext().setSetting(KSESSION_ID, String.valueOf(ksession.getId()));
                    }
                }
            }
        }
        return ksession;
    }

    private org.jbpm.task.TaskService getSessionTaskService() {
        getSession(); // ensure session is created before task service
        return client;
    }

    private org.jbpm.task.TaskService getLocalTaskService() {
        LocalTaskService localTaskService = new LocalTaskService(taskService);
        localTaskService.setEnvironment(env);
        localTaskService.addEventListener(this);
        return localTaskService;
    }

    // process operations

    public Task getTask(long taskId) {
        return getSessionTaskService().getTask(taskId);
    }

    public void claimTask(long taskId, String userLogin) {
        getSessionTaskService().claim(taskId, userLogin);
    }

    public void endTask(long taskId, String userLogin, ContentData outputData, boolean startNeeded) {
        if (startNeeded) {
            getSessionTaskService().start(taskId, userLogin);
        }
        getSessionTaskService().complete(taskId, userLogin, outputData);
    }

    public ProcessInstance getProcessInstance(long processId) {
        return getSession().getProcessInstance(processId);
    }

    public void startProcess(String processId, Map<String,Object> parameters) {
        getSession().startProcess(processId, parameters);
    }

    public void abortProcessInstance(long processId) {
        getSession().abortProcessInstance(processId);
    }

    // queries

    public void refreshDataForNativeQuery() {
        // this call forces JBPM to flush awaiting task data
        getLocalTaskService().query("SELECT task.id FROM Task task ORDER BY task.id DESC", 1, 0);
    }

    private TaskQuery<Task> createTaskQuery() {
        return new TaskQuery<Task>(getLocalTaskService());
    }

    private UserQuery<User> createUserQuery() {
        return new UserQuery<User>(getLocalTaskService());
    }

    public Task getTaskForAssign(String queueName, long taskId) {
        return createTaskQuery()
                .groupId(queueName)
                .taskId(taskId)
                .assigneeIsNull()
                .first();
    }

    public Task getLatestTask(long processId) {
        return createTaskQuery()
                .processInstanceId(processId)
                .completed()
                .orderByCompleteDateDesc()
                .first();
    }

    public Task getMostRecentProcessHistoryTask(long processId, String userLogin, Date completedAfter) {
        return createTaskQuery()
                .assignee(userLogin)
                .processInstanceId(processId)
                .completedAfter(completedAfter)
                .orderByCompleteDateDesc()
                .first();
    }

    public Task getPastOrActualTask(long processId, String userLogin, String taskName, Date completedAfter) {
        return createTaskQuery()
                .assignee(userLogin)
                .processInstanceId(processId)
                .completedAfter(completedAfter)
                .activityName(taskName)
                .orderByCompleteDate()
                .first();
    }

    public List<Task> getTasks(long processId, String userLogin, Collection<String> taskNames) {
        return createTaskQuery()
                .processInstanceId(processId)
                .active()
                .assignee(userLogin)
                .activityNames(taskNames)
                .list();
    }

    public List<Task> getTasks(long processId, String userLogin) {
        return createTaskQuery()
                .processInstanceId(processId)
                .assignee(userLogin)
                .active()
                .list();
    }

    public List<Task> getTasks(String userLogin, Integer offset, Integer limit) {
        return createTaskQuery()
                .assignee(userLogin)
                .active()
                .page(offset, limit)
                .list();
    }

    public List<Task> getTasks() {
        return createTaskQuery().orderByTaskIdDesc().list();
    }

    public List<Object[]> getTaskCounts(List<String> groupNames) {
        return (List<Object[]>)(List)createTaskQuery()
                .selectGroupId()
                .selectCount()
                .assigneeIsNull()
                .groupIds(groupNames)
                .groupByGroupId()
                .list();
    }

    public List<String> getAvailableUserLogins(String filter, Integer offset, Integer limit) {
        return createUserQuery()
                .selectId()
                .whereIdLike(filter != null ? '%' + filter + '%' : null)
                .page(offset, limit)
                .list();
    }

    @Override
    public void beforeProcessStarted(ProcessStartedEvent event) {
        getProcessEventListener().beforeProcessStarted(event);
    }

    @Override
    public void afterProcessStarted(ProcessStartedEvent event) {
        getProcessEventListener().afterProcessStarted(event);
    }

    @Override
    public void beforeProcessCompleted(ProcessCompletedEvent event) {
        getProcessEventListener().beforeProcessCompleted(event);
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        getProcessEventListener().afterProcessCompleted(event);
    }

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        getProcessEventListener().beforeNodeTriggered(event);
    }

    @Override
    public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
        getProcessEventListener().afterNodeTriggered(event);
    }

    @Override
    public void beforeNodeLeft(ProcessNodeLeftEvent event) {
        getProcessEventListener().beforeNodeLeft(event);
    }

    @Override
    public void afterNodeLeft(ProcessNodeLeftEvent event) {
        getProcessEventListener().afterNodeLeft(event);
    }

    @Override
    public void beforeVariableChanged(ProcessVariableChangedEvent event) {
        getProcessEventListener().beforeVariableChanged(event);
    }

    @Override
    public void afterVariableChanged(ProcessVariableChangedEvent event) {
        getProcessEventListener().afterVariableChanged(event);
    }

    @Override
    public void taskCreated(TaskUserEvent event) {
        getTaskEventListener().taskCreated(event);
    }

    @Override
    public void taskClaimed(TaskUserEvent event) {
        getTaskEventListener().taskClaimed(event);
    }

    @Override
    public void taskStarted(TaskUserEvent event) {
        getTaskEventListener().taskStarted(event);
    }

    @Override
    public void taskStopped(TaskUserEvent event) {
        getTaskEventListener().taskStopped(event);
    }

    @Override
    public void taskReleased(TaskUserEvent event) {
        getTaskEventListener().taskReleased(event);
    }

    @Override
    public void taskCompleted(TaskUserEvent event) {
        getTaskEventListener().taskCompleted(event);
    }

    @Override
    public void taskFailed(TaskUserEvent event) {
        getTaskEventListener().taskFailed(event);
    }

    @Override
    public void taskSkipped(TaskUserEvent event) {
        getTaskEventListener().taskSkipped(event);
    }

    @Override
    public void taskForwarded(TaskUserEvent event) {
        getTaskEventListener().taskForwarded(event);
    }

    private static final ProcessEventListener NULL_PROCESS_LISTENER = new ProcessEventListener() {
        @Override
        public void beforeProcessStarted(ProcessStartedEvent processStartedEvent) {
        }

        @Override
        public void afterProcessStarted(ProcessStartedEvent processStartedEvent) {
        }

        @Override
        public void beforeProcessCompleted(ProcessCompletedEvent processCompletedEvent) {
        }

        @Override
        public void afterProcessCompleted(ProcessCompletedEvent processCompletedEvent) {
        }

        @Override
        public void beforeNodeTriggered(ProcessNodeTriggeredEvent processNodeTriggeredEvent) {
        }

        @Override
        public void afterNodeTriggered(ProcessNodeTriggeredEvent processNodeTriggeredEvent) {
        }

        @Override
        public void beforeNodeLeft(ProcessNodeLeftEvent processNodeLeftEvent) {
        }

        @Override
        public void afterNodeLeft(ProcessNodeLeftEvent processNodeLeftEvent) {
        }

        @Override
        public void beforeVariableChanged(ProcessVariableChangedEvent event) {
        }

        @Override
        public void afterVariableChanged(ProcessVariableChangedEvent event) {
        }
    };

    private static final TaskEventListener NULL_TASK_LISTENER = new TaskEventListener() {
        @Override
        public void taskCreated(TaskUserEvent event) {
        }

        @Override
        public void taskClaimed(TaskUserEvent event) {
        }

        @Override
        public void taskStarted(TaskUserEvent event) {
        }

        @Override
        public void taskStopped(TaskUserEvent event) {
        }

        @Override
        public void taskReleased(TaskUserEvent event) {
        }

        @Override
        public void taskCompleted(TaskUserEvent event) {
        }

        @Override
        public void taskFailed(TaskUserEvent event) {
        }

        @Override
        public void taskSkipped(TaskUserEvent event) {
        }

        @Override
        public void taskForwarded(TaskUserEvent event) {
        }
    };

    private static final ThreadLocal<ProcessEventListener> processListenerTL = new ThreadLocal<ProcessEventListener>();
    private static final ThreadLocal<TaskEventListener> taskListenerTL = new ThreadLocal<TaskEventListener>();

    public static void setProcessEventListener(ProcessEventListener eventListener) {
        processListenerTL.set(eventListener);
    }

    public static void setTaskEventListener(TaskEventListener eventListener) {
        taskListenerTL.set(eventListener);
    }

    private static ProcessEventListener getProcessEventListener() {
        ProcessEventListener eventListener = processListenerTL.get();
        return eventListener != null ? eventListener : NULL_PROCESS_LISTENER;
    }

    private static TaskEventListener getTaskEventListener() {
        TaskEventListener eventListener = taskListenerTL.get();
        return eventListener != null ? eventListener : NULL_TASK_LISTENER;
    }

    public synchronized JbpmRepository getRepository() {
        if (repository == null) {
            String jbpmRepositoryDir = getThreadProcessToolContext().getSetting(JBPM_REPOSITORY_DIR);
            repository = new DefaultJbpmRepository(hasText(jbpmRepositoryDir) ? jbpmRepositoryDir : null);
        }
        return repository;
    }


}