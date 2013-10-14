package pl.net.bluesoft.rnd.processtool.ui.basewidgets;

import com.vaadin.ui.TextArea;

import pl.net.bluesoft.rnd.processtool.plugins.IBundleResourceProvider;
import pl.net.bluesoft.rnd.processtool.ui.basewidgets.editor.ScriptCodeEditor;
import pl.net.bluesoft.rnd.processtool.ui.widgets.ProcessHtmlWidget;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.AliasName;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.AperteDoc;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.AutoWiredProperty;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.AutoWiredPropertyConfigurator;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.ChildrenAllowed;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.WidgetGroup;
import pl.net.bluesoft.rnd.processtool.web.widgets.impl.FileWidgetContentProvider;

@AperteDoc(humanNameKey = "widget.process_data_block_html.name", descriptionKey = "widget.process_data_block.description")
@ChildrenAllowed(false)
@WidgetGroup("base-widgets")
@AliasName(name = "ProcessDataHtml")
public class ProcessDataBlockHtmlWidget extends ProcessHtmlWidget /*BaseProcessToolVaadinWidget implements ProcessToolDataWidget, ProcessToolVaadinRenderable*/ {

	public ProcessDataBlockHtmlWidget(IBundleResourceProvider bundleResourceProvider)
    {
        setWidgetName("ProcessDataHtml");
        setContentProvider(new FileWidgetContentProvider("html-process.html", bundleResourceProvider));
    }
	
    @AutoWiredProperty(required = true)
    @AutoWiredPropertyConfigurator(fieldClass = TextArea.class)
    @AperteDoc(
            humanNameKey = "widget.process_data_block.property.widgetsDefinition.name",
            descriptionKey = "widget.process_data_block.property.widgetsDefinition.description"
    )
    private String widgetsDefinition;
    
}