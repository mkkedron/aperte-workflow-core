package pl.net.bluesoft.rnd.processtool.ui.basewidgets.queues;

import pl.net.bluesoft.rnd.processtool.ui.basewidgets.queues.beans.DefaultQueueBeanFactory;
import pl.net.bluesoft.rnd.processtool.web.domain.IContentProvider;
import pl.net.bluesoft.rnd.processtool.web.view.AbstractTaskListView;
import pl.net.bluesoft.rnd.processtool.web.view.ITasksListViewBeanFactory;
import pl.net.bluesoft.rnd.processtool.web.view.ProcessInstanceFilter;
import pl.net.bluesoft.rnd.processtool.web.view.TaskListView;

import java.util.Map;

/**
 * Created by mpawlak@bluesoft.net.pl on 2014-09-02.
 */
@TaskListView(
        queueId = "defaultQueue",
        queueDisplayedName = "",
        queueDisplayedDescription = "",
        fileName = "default-queue-view.html",
        mainFactory = DefaultQueueBeanFactory.class,
        queueType = AbstractTaskListView.QueueTypes.CUSTOM_QUEUE,
        priority = 3,
        processFactories = {})
public class DefaultQueueView extends AbstractTaskListView
{
    public DefaultQueueView(IContentProvider contentProvider, ITasksListViewBeanFactory providedMainFactory) {
        super(contentProvider, providedMainFactory);
    }

    @Override
    public ProcessInstanceFilter getProcessInstanceFilter(Map<String, Object> parameters) {
        ProcessInstanceFilter processInstanceFilter = new ProcessInstanceFilter();
        processInstanceFilter.setFilterOwnerLogin("controlling");
        processInstanceFilter.addQueue("controlling");
        return processInstanceFilter;
    }
}
