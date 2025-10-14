/**
 * Copyright (c) 2012 - 2025 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     BND - initial API and implementation as ProjectLauncher
 *     Data In Motion - initial API and implementation
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
