package {{basePackageName}};

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.osgi.service.event.Event;
import org.gecko.eclipse.annotations.RequireEclipseCompatibility;
import org.gecko.eclipse.annotations.RequireEclipseConsole;
import org.gecko.eclipse.annotations.RequireEclipseE4;

@RequireEclipseConsole
@RequireEclipseE4
@RequireEclipseCompatibility
public class AnnotationClass {
	@Inject
	@Optional
	public void handleBringToTop(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
		// Nothing to do here
	}	
}
