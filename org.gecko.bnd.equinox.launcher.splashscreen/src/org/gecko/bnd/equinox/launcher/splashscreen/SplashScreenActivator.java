/**
 * Copyright (c) 2012 - 2019 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.bnd.equinox.launcher.splashscreen;

import java.io.File;
import java.net.URI;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.logging.Logger;

import org.eclipse.equinox.launcher.JNIBridge;
import org.eclipse.osgi.service.runnable.StartupMonitor;
import org.gecko.bnd.eclipse.launcher.util.DefaultStartupMonitor;
import org.gecko.bnd.eclipse.launcher.util.SplashHandler;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import static org.gecko.eclipse.api.BndEclipseConstants.*;
/**
 * 
 * @author Juergen Albert
 * @since 30 Sep 2019
 */
public class SplashScreenActivator implements BundleActivator {

	private final static Logger	logger = Logger.getLogger("BndSplashScreen");
	public static final String IMMEDIATE = "AFTER_FRAMEWORK_INIT";
	
	private SplashHandler splashHandler;
	private JNIBridge bridge;

	private ServiceRegistration<Runnable> splashHandlerRegistry;

	private ServiceRegistration<StartupMonitor> startupMonitorRegistration;

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		boolean noSplash = "true".equals(context.getProperty(NOSPLASH));
		if(noSplash) {
			logger.fine("Ommitting SplashScreen because -nosplash option is set");
			return;
		}
		logger.fine("Pulling up Splash");
		String launcherLib = context.getProperty(PROP_LAUNCHER_LIBRARY);
		if(launcherLib != null) {
			logger.fine("Using launcher library " + launcherLib);
			bridge = new JNIBridge(launcherLib);
			String splashLocation = context.getProperty(PROP_SPLASHLOCATION);
			if(splashLocation != null) {
				String realSplashLocation = splashLocation;
				
				File f = new File(realSplashLocation);
				if(!f.exists()) {
					String installArea = System.getProperty(PROP_INSTALL_AREA);
					if(installArea != null) {
						File installAreaFile = new File(new URI(installArea));
						if(installAreaFile.exists()) {
							f = new File(installAreaFile, splashLocation);
							if(f.exists()) {
								realSplashLocation = f.getAbsolutePath();
							}
						}
					}
				}

				if(!f.exists()) {
					logger.warning("Splash does not exist at " + f.getAbsolutePath());
					return;
				}

					
				if(realSplashLocation != null) {
					splashHandler = new SplashHandler(bridge);
					
					Dictionary<String, String> props = new Hashtable<String, String>();
					props.put(PROP_SPLASH, Boolean.TRUE.toString());
					props.put(PROP_LAUNCHER_LIBRARY, launcherLib);
					props.put(PROP_SPLASHLOCATION, realSplashLocation);
					
					// determine the splash location
					splashHandler.showsplash(realSplashLocation);
					splashHandlerRegistry = context.registerService(Runnable.class, splashHandler, props);
					startupMonitorRegistration = context.registerService(StartupMonitor.class, new DefaultStartupMonitor(splashHandler), props);
					
					
					// Register the endSplashHandler to be run at VM shutdown. This hook will be 
					// removed once the splash screen has been taken down.
					try {
						Runtime.getRuntime().addShutdownHook(splashHandler);
					} catch (Throwable ex) {
						// Best effort to register the handler
					}
				}
			}
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		if(splashHandler != null) {
			splashHandler.run();
			if(splashHandlerRegistry != null) {
				splashHandlerRegistry.unregister();
				splashHandlerRegistry = null;
			}
			if(startupMonitorRegistration != null) {
				startupMonitorRegistration.unregister();
				startupMonitorRegistration = null;
			}
		}

	}

}
