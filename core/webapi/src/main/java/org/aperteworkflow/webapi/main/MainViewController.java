package org.aperteworkflow.webapi.main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.bind.annotation.RenderMapping;

import pl.net.bluesoft.rnd.processtool.BasicSettings;
import pl.net.bluesoft.rnd.processtool.ProcessToolContext;
import pl.net.bluesoft.rnd.processtool.ProcessToolContextCallback;
import pl.net.bluesoft.rnd.processtool.bpm.ProcessToolBpmSession;
import pl.net.bluesoft.rnd.processtool.di.ObjectFactory;
import pl.net.bluesoft.rnd.processtool.model.UserData;
import pl.net.bluesoft.rnd.processtool.plugins.ProcessToolRegistry;
import pl.net.bluesoft.rnd.processtool.usersource.IPortalUserSource;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import static pl.net.bluesoft.rnd.processtool.bpm.ProcessToolBpmConstants.DEFAULT_QUEUE_INTERVAL;

@Controller(value = "MainViewController")
@RequestMapping("VIEW")
public class MainViewController extends AbstractMainController
{
	@Autowired
	private ProcessToolRegistry processToolRegistry;
	
	@RenderMapping ()
	public ModelAndView handleMainRenderRequest(RenderRequest request,RenderResponse response,Model model)
    {
		System.out.println("MainViewController.handleMainRenderRequest... ");
		
        ModelAndView modelView = new ModelAndView();
        modelView.setViewName("main");
        modelView.addObject(IS_STANDALONE, false);

        processRequest(modelView, request);

        return modelView;
    }

    private void processRequest(final ModelAndView modelView, final RenderRequest request)
	{
		processToolRegistry.withProcessToolContext(new ProcessToolContextCallback() {

			@Override
			public void withContext(ProcessToolContext ctx) 
			{
                IPortalUserSource userSource = ObjectFactory.create(IPortalUserSource.class);
                UserData user = userSource.getUserByRequest(request);

		        /* No user to process, abort */
                if(user == null)
                    return;

                modelView.addObject(USER_PARAMETER_NAME, user);


				ProcessToolBpmSession bpmSession = (ProcessToolBpmSession)request.getAttribute(ProcessToolBpmSession.class.getName());
				if(bpmSession == null)
				{
					bpmSession = processToolRegistry.getProcessToolSessionFactory().createSession(user);
					request.setAttribute(ProcessToolBpmSession.class.getName(), bpmSession);
				}
				
                modelView.addObject(PROCESS_START_LIST, addProcessStartList(ctx, bpmSession));
                
				Integer interval = DEFAULT_QUEUE_INTERVAL;
                String refreshInterval = ctx.getSetting(BasicSettings.REFRESHER_INTERVAL_SETTINGS_KEY);
                if (refreshInterval!=null && !refreshInterval.trim().isEmpty()) {
    				try {
						interval = Integer.parseInt(refreshInterval+"000");
					} catch (NumberFormatException e) {}
                }
        		modelView.addObject(QUEUE_INTERVAL,interval);
				
			}
		});
	}
}