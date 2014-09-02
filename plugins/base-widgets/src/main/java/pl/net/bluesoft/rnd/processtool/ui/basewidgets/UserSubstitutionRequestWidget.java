package pl.net.bluesoft.rnd.processtool.ui.basewidgets;

import pl.net.bluesoft.rnd.processtool.plugins.IBundleResourceProvider;
import pl.net.bluesoft.rnd.processtool.ui.widgets.ProcessHtmlWidget;
import pl.net.bluesoft.rnd.processtool.ui.widgets.annotations.*;
import pl.net.bluesoft.rnd.processtool.ui.widgets.impl.SimpleWidgetDataHandler;
import pl.net.bluesoft.rnd.processtool.web.widgets.impl.FileContentProvider;


@AliasName(name = "UserSubstitutionRequestWidget", type = WidgetType.Html)
@WidgetGroup("common")
@AperteDoc(humanNameKey="widget.substitution.request.name", descriptionKey="widget.substitution.request.description")
@ChildrenAllowed(false)
public class UserSubstitutionRequestWidget extends ProcessHtmlWidget
{
    @AutoWiredProperty
    private boolean requestMode = true;

    public UserSubstitutionRequestWidget(IBundleResourceProvider bundleResourceProvider) {
        setContentProvider(new FileContentProvider("process-substitution-request.html", bundleResourceProvider));
        addDataHandler(new SimpleWidgetDataHandler());
    }
}
