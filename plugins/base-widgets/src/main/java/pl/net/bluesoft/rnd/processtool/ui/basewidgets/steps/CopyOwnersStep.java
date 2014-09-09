package pl.net.bluesoft.rnd.processtool.ui.basewidgets.steps;


import pl.net.bluesoft.rnd.processtool.model.BpmStep;
import pl.net.bluesoft.rnd.processtool.model.ProcessInstance;
import pl.net.bluesoft.rnd.processtool.steps.ProcessToolProcessStep;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.AliasName;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.AutoWiredProperty;

import java.util.Map;
import java.util.logging.Logger;

@AliasName(name = "CopyOwnersStep")
public class CopyOwnersStep implements ProcessToolProcessStep {

	
	@AutoWiredProperty
	private String root = "false";

    @AutoWiredProperty
    private String toParentProcess = "false";

	private final static Logger logger = Logger.getLogger(CopyOwnersStep.class.getName());

    @Override
    public String invoke(BpmStep step, Map<String, String> params) throws Exception
    {
        ProcessInstance parent = step.getProcessInstance().getParent();
    	ProcessInstance pi = step.getProcessInstance();

    	if(Boolean.parseBoolean(root))
            parent = pi.getRootProcessInstance();


        if(Boolean.parseBoolean(toParentProcess))
            copyOwners(pi, parent);
        else
            copyOwners(parent, pi);
    	return STATUS_OK;
    }

    private void copyOwners(ProcessInstance fromProcessInstance, ProcessInstance toProcessInstance)
    {
        for(String ownerLogin : fromProcessInstance.getOwners())
            toProcessInstance.addOwner(ownerLogin);
    }


}
