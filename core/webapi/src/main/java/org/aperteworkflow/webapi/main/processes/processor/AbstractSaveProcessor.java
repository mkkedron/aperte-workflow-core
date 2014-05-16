package org.aperteworkflow.webapi.main.processes.processor;

import org.aperteworkflow.webapi.main.processes.action.domain.SaveResultBean;
import org.aperteworkflow.webapi.main.processes.action.domain.ValidateResultBean;
import org.aperteworkflow.webapi.main.processes.domain.HtmlWidget;
import org.codehaus.jackson.map.ObjectMapper;
import pl.net.bluesoft.rnd.processtool.model.*;
import pl.net.bluesoft.rnd.processtool.ui.widgets.*;
import pl.net.bluesoft.rnd.util.i18n.I18NSource;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static pl.net.bluesoft.rnd.processtool.plugins.ProcessToolRegistry.Util.getRegistry;

/**
 * Task save processor class
 *
 * @author mpawlak@bluesoft.net.pl
 */
public abstract class AbstractSaveProcessor {
    protected Collection<HtmlWidget> widgets;
    protected I18NSource messageSource;

    protected static final ObjectMapper mapper = new ObjectMapper();
    protected static final Logger logger = Logger.getLogger(AbstractSaveProcessor.class.getName());

    protected abstract IAttributesProvider getProvider();

    protected abstract IAttributesConsumer getConsumer();

    public AbstractSaveProcessor(I18NSource messageSource, Collection<HtmlWidget> widgets) {
        this.widgets = widgets;
        this.messageSource = messageSource;
    }

    /**
     * Validate vaadin and html widgets. Validation is performed to all widgets and
     * one widgets error does not stop validation processes
     */
    public ValidateResultBean validateWidgets() {
        ValidateResultBean validateResult = new ValidateResultBean();
        validateHtmlWidgets(validateResult);

        return validateResult;
    }

    /**
     * Save vaadin and html widgets
     */
    public SaveResultBean saveWidgets() {
        SaveResultBean saveResult = new SaveResultBean();
        saveHtmlWidgets(saveResult);

        return saveResult;
    }

    private void validateHtmlWidgets(ValidateResultBean validateResult) {
        for (HtmlWidget widgetToValidate : widgets) {
            /** Get widget definition to retrive validator class */
            ProcessHtmlWidget processWidget = getRegistry().getGuiRegistry().getHtmlWidget(widgetToValidate.getWidgetName());
            if (processWidget == null)
                throw new RuntimeException(messageSource.getMessage("process.widget.name.unknown", widgetToValidate.getWidgetName()));

            IWidgetValidator widgetValidator = processWidget.getValidator();

            WidgetData widgetData = new WidgetData();
            widgetData.addWidgetData(widgetToValidate.getData());

            Collection<String> errors = widgetValidator.validate(getProvider(), widgetData);
            for (String error : errors)
                validateResult.addError(widgetToValidate.getWidgetId().toString(), error);
        }
    }

    private void saveHtmlWidgets(SaveResultBean saveResult) {
        Collection<HandlingResult> results = new LinkedList<HandlingResult>();
        for (HtmlWidget widgetToSave : widgets) {
            /** Get widget definition to retrive data handler class */
            ProcessHtmlWidget processWidget = getRegistry().getGuiRegistry().getHtmlWidget(widgetToSave.getWidgetName());
            if (processWidget == null)
                throw new RuntimeException(messageSource.getMessage("process.widget.name.unknown", widgetToSave.getWidgetName()));

            for (IWidgetDataHandler widgetDataHandler : processWidget.getDataHandlers()) {
                WidgetData widgetData = new WidgetData();
                widgetData.addWidgetData(widgetToSave.getData());

                results.addAll(widgetDataHandler.handleWidgetData(getProvider(), widgetData));
            }

        }
        ProcessInstance process = getProvider().getProcessInstance();

        if (process != null && !results.isEmpty()) {
            String json = null;
            try {
                json = mapper.writeValueAsString(results);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
            ProcessInstanceLog log = new ProcessInstanceLog();
            log.setState(null);
            log.setEntryDate(new Date());
            log.setEventI18NKey("process.log.process-change");
            // todo
            if (getProvider() instanceof BpmTask)
                log.setUserLogin(((BpmTask) getProvider()).getAssignee());
            log.setLogType(ProcessInstanceLog.LOG_TYPE_PROCESS_CHANGE);
            log.setOwnProcessInstance(process);
            log.setLogValue(json);
            process.getRootProcessInstance().addProcessLog(log);
        }
    }


}
