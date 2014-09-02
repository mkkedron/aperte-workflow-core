<%@ page import="org.springframework.web.servlet.support.RequestContextUtils"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>

<script type="text/javascript">

  	var queueViewManager = new QueueViewManager();
	
	function QueueView(tableObject, viewName)
	{
		this.tableObject = tableObject;
		this.viewName = viewName;
	}
	
	function QueueViewManager()
	{
		this.views = {};
		
		this.defaultQueueId = '';

		this.currentQueue = '';
		this.currentQueueType = '';
		this.currentOwnerLogin = '${aperteUser.login}';
		this.currentQueueDesc = '';
		
		this.loadQueue = function(newQueueName, queueType, ownerLogin, queueDesc)
		{
			var oldView = this.views[this.currentQueueType];
			var newView = this.views[queueType];
			
			$('#'+oldView.viewName).hide();
			$('#'+newView.viewName).show();
			
			
			this.currentQueue = newQueueName;
			this.currentQueueType = queueType;
			this.currentOwnerLogin = ownerLogin;
			this.currentQueueDesc = queueDesc;

			var requestUrl = '<portlet:resourceURL id="loadProcessesList"/>';
			requestUrl += "&<portlet:namespace/>queueName=" + newQueueName;
			requestUrl += "&<portlet:namespace/>queueType=" + queueType;
			requestUrl += "&<portlet:namespace/>ownerLogin=" + ownerLogin;
			
			newView.tableObject.reloadTable(requestUrl);
			
			windowManager.showProcessList();
			
			$("#process-queue-name-id").text('<spring:message code="processes.currentqueue" />'+" "+queueDesc);
		}
		
		this.reloadCurrentQueue = function()
		{
			this.loadQueue(this.currentQueue, this.currentQueueType, this.currentOwnerLogin, this.currentQueueDesc);
		}
		
		this.addTableView = function(queueId, tableObject, viewName)
		{
			this.views[queueId] = new QueueView(tableObject, viewName);
		}
		
		this.removeQueue = function(queueId)
		{
			var queue = this.views[queueId];
			if(queue)
			{
				queue.tableObject.fnDestroy();
			}	
		}
		
		this.removeCurrentQueue = function()
		{
			if(this.currentQueue != '')
			{
				removeQueue(this.currentQueue);
			}
		}
		
		this.toggleColumn = function(viewName, columnName)
		{
			this.views[viewName].tableObject.toggleColumn(columnName);
		}
		
		this.enableMobileMode = function()
		{
			$.each(this.views, function(viewName, view)
			{
				if(view.tableObject.initialized == true)
				{
					view.tableObject.enableMobileMode();
				}
			});
		}
		
		this.enableTabletMode = function()
		{
			$.each(this.views, function(viewName, view)
			{
				if(view.tableObject.initialized == true)
				{
					view.tableObject.enableTabletMode();
				}
			});
		}
		
		this.disableMobileMode = function()
		{
			$.each(this.views, function(viewName, view)
			{
				if(view.tableObject.initialized == true)
				{
					view.tableObject.disableMobileMode();
				}
			});
		}
		
		this.disableTabletMode = function()
		{
			$.each(this.views, function(viewName, view)
			{
				if(view.tableObject.initialized == true)
				{
					view.tableObject.disableTabletMode();
				}
			});
		}
	}
</script>