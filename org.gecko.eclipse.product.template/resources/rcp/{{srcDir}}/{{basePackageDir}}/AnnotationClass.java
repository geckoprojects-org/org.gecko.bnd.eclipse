package {{basePackageName}};

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.gecko.bnd.eclipse.rcp.annotations.RequireEclipseConsole;
import org.gecko.bnd.eclipse.rcp.annotations.RequireEclipseE4;
import org.gecko.bnd.eclipse.rcp.annotations.RequireEclipseEMF;
import org.gecko.bnd.eclipse.rcp.annotations.RequireEclipseIDE;
import org.gecko.bnd.eclipse.rcp.annotations.RequireEclipsePDE;
import org.osgi.service.event.Event;

@RequireEclipseIDE
@RequireEclipseConsole
@RequireEclipseEMF
@RequireEclipseE4
@RequireEclipsePDE
public class AnnotationClass {
	@Inject
	@Optional
	public void handleBringToTop(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
		System.out.println(event);
	}	
}
