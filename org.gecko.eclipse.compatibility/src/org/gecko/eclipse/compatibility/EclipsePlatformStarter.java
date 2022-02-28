/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *      Data In Motion - initial API and implementation
 */
package org.gecko.eclipse.compatibility;

import static org.gecko.eclipse.api.BndEclipseConstants.BSN_ECLIPSE_CORE_RUNTIME;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_ECLIPSE_IGNORE_APP;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_ECLIPSE_INITIALIZE;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.internal.adaptor.EclipseAppLauncher;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.condition.Condition;
import org.osgi.util.promise.PromiseFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer; 

/**
 * Component taking the Job of the EclipsePlattformStarter that starts the configured Application as soon as the 
 * {@link ApplicationDescriptor} becomes available
 * 
 * @author Juergen Albert
 */
@Component(immediate=true , reference = {
		@Reference(name = "equinoxConfig", service = Condition.class, policy=ReferencePolicy.STATIC, cardinality=ReferenceCardinality.MANDATORY, target = "(" + Condition.CONDITION_ID + "=equinoxConfig)")
} )
public class EclipsePlatformStarter implements ServiceTrackerCustomizer<ApplicationDescriptor, ApplicationDescriptor> {

	/** GECKO_RCP_APPLICATION_RUNNER */
	private static final String THREAD_NAME = "Gecko RCP Application Runner";

	private static ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
		
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, THREAD_NAME);
		}
		
	});

	private Logger logger = Logger.getLogger(EclipsePlatformStarter.class.getName());
	
	@Reference
	FrameworkLog log;
	
	@Reference
	EnvironmentInfo envInfo;
	

	private EclipseAppLauncher appLauncher;

	private ServiceRegistration<ApplicationLauncher> appLauncherRegistration;

	private AtomicBoolean shutdown = new AtomicBoolean(true);

	private BundleContext ctx;

	private ServiceTracker<ApplicationDescriptor, ApplicationDescriptor> descriptorTracker;

	private String applicationId;
	
	@Activate
	public void activate(BundleContext ctx) throws InvalidSyntaxException, BundleException {
		this.ctx = ctx;
		Bundle runtimeBundle = findBundle(BSN_ECLIPSE_CORE_RUNTIME, ctx);
		if(runtimeBundle != null) {
			try {
				runtimeBundle.start();
			} catch (BundleException e) {
				logger.log(Level.SEVERE, "Could not start org.eclipse.core.runtime bundle", e);
				throw e;
			}
		}
		
		String initMode = ctx.getProperty(PROP_ECLIPSE_INITIALIZE);
		if(Boolean.parseBoolean(initMode)) {
			
			try {
				logger.info("Started in initialization mode. Headless startup finished. Will shut down.");
				shutdownFramework(null);
			} catch (BundleException e) {
				logger.log(Level.SEVERE, "Could not shotdown framework", e);
				throw e;
			}
			return;
		}
		String ignoreApp = ctx.getProperty(PROP_ECLIPSE_IGNORE_APP);
		if(!Boolean.parseBoolean(ignoreApp)) {
			applicationId = ctx.getProperty("eclipse.application");
			if(applicationId == null) {
				logger.severe("Can't start Equinox Application because no eclipse.application property is defined");
				return;
			}
			Filter filter = FrameworkUtil.createFilter(String.format("(&(objectClass=%s)(service.pid=%s))", ApplicationDescriptor.class.getName(), applicationId));
			descriptorTracker = new ServiceTracker<ApplicationDescriptor, ApplicationDescriptor>(ctx, filter, this);
			descriptorTracker.open();
		}
	}
	
	@Deactivate
	public void deactivate() {
		if(descriptorTracker != null) {
			descriptorTracker.close();
		}
		if(appLauncherRegistration != null) {
			appLauncherRegistration.unregister();
		}
		shutdown.set(false);
		if(appLauncher != null) {
			appLauncher.shutdown();
		}
	}

	@Override
	public ApplicationDescriptor addingService(ServiceReference<ApplicationDescriptor> descriptor) {
		startApplication();
		return ctx.getService(descriptor);
	}

	@Override
	public void modifiedService(ServiceReference<ApplicationDescriptor> arg0, ApplicationDescriptor arg1) {
	}

	@Override
	public void removedService(ServiceReference<ApplicationDescriptor> arg0, ApplicationDescriptor arg1) {
		ctx.ungetService(arg0);
	}

	private Bundle findBundle(String bsn, BundleContext ctx) {
		Bundle[] bundles = ctx.getBundles();
		for(Bundle bundle : bundles) {
			if(bsn.equals(bundle.getSymbolicName())) {
				return bundle;
			}
		}
		return null;
	}

	private void startApplication() {
		PromiseFactory promiseFactory = new PromiseFactory(executorService);
		logger.fine("Registering Gecko Equinox App  Launcher");
		
		// create the ApplicationLauncher and register it as a service
		appLauncher = new EclipseAppLauncher(ctx, false, false, log, (EquinoxConfiguration) envInfo);
		logger.fine("Starting Equinox App Launcher");
		
		// must start the launcher AFTER service registration because this method 
		// blocks and runs the application on the current thread.  This method 
		promiseFactory.submit(() -> {
			// will return only after the application has stopped.
			Object start = appLauncher.start(null);	
			return start;
		}).thenAccept(o -> {
			if(shutdown.get()) {
				shutdownFramework(o);
			} 
		}).onFailure(t -> {
			t.printStackTrace();
			System.exit(42);	
		});
		appLauncherRegistration = ctx.registerService(ApplicationLauncher.class, appLauncher, null);
	}

	private void shutdownFramework(Object o) throws BundleException {
		if(o != null) {
			logger.fine("Shutting down with Application " +  applicationId + " return code: " + o);
		} else {
			logger.fine("Shutting down");
		}
		Bundle bundle = ctx.getBundle(0);
		logger.fine("Stopping Framework");
		bundle.stop();
		logger.fine("Stopped Framework");
	}
}
