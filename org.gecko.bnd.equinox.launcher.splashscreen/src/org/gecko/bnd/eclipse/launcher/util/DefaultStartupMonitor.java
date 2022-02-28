/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *      BND - initial API and implementation as ProjectLauncher
 *      Data In Motion - initial API and implementation
 */
package org.gecko.bnd.eclipse.launcher.util;

import org.eclipse.osgi.service.runnable.StartupMonitor;

public class DefaultStartupMonitor implements StartupMonitor {

	private final SplashHandler splashHandler;
	/**
	 * Create a new startup monitor using the given splash handler.  The splash handle must
	 * have an updateSplash method.
	 * 
	 * @param splashHandler
	 * @throws IllegalStateException
	 */
	public DefaultStartupMonitor(SplashHandler splashHandler) throws IllegalStateException {
		this.splashHandler = splashHandler;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.adaptor.StartupMonitor#update()
	 */
	public void update() {
		splashHandler.updateSplash();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.runnable.StartupMonitor#applicationRunning()
	 */
	public void applicationRunning() {
		splashHandler.run();
	}
}
