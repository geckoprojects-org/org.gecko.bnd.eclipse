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
package org.gecko.bnd.eclipse.launcher.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.eclipse.equinox.launcher.JNIBridge;

public class SplashHandler extends Thread {

	private final static Logger	logger = Logger.getLogger("BndSplashScreen");
	private final JNIBridge bridge;

	private final AtomicBoolean splashDown = new AtomicBoolean(false);
	private InternalThread internalThread;

	/**
	 * @param thread
	 * 
	 */
	public SplashHandler(JNIBridge bridge) {
		this.bridge = bridge;
	}

	/**
	 * The splash must be taken up and down by the same Thread.
	 * 
	 * @author Juergen Albert
	 * @since 16 Oct 2019
	 */
	private static final class InternalThread implements Runnable {

		private SplashHandler handler;
		private JNIBridge bridge;
		private String splashLocation;

		private CountDownLatch latch = new CountDownLatch(1);

		/**
		 * Creates a new instance.
		 */
		public InternalThread(SplashHandler handler, JNIBridge bridge, String splashLocation) {
			this.handler = handler;
			this.bridge = bridge;
			this.splashLocation = splashLocation;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			logger.fine("Showing splash ...");
			bridge.showSplash(splashLocation);
			try {
				latch.await();
			} catch (Exception e) {
				if (e instanceof InterruptedException) {
					Thread.currentThread().interrupt();
				}
			}
			logger.fine("Taking down splash");
			handler.takeDownSplash();
		}

		public void terminate() {
			latch.countDown();
		}

		public void updateSplash() {
			if (this.bridge != null && latch.getCount() != 0) {
				this.bridge.updateSplash();
			}
		}
	}

	@Override
	public void run() {
		if (internalThread != null) {
			internalThread.terminate();
		}
	}

	public void updateSplash() {
		if (internalThread != null) {
			internalThread.updateSplash();
		}
	}

	public void showsplash(String splashLocation) {
		internalThread = new InternalThread(this, bridge, splashLocation);
		Executors.newSingleThreadExecutor(new ThreadFactory() {
			
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "Gecko Bnd Eclipse Splash Handler");
			}
		}).execute(internalThread);
	}

	/*
	 * Take down the splash screen.
	 */
	public void takeDownSplash() {
		if (splashDown.get() || this.bridge == null) // splash is already down
			return;
		synchronized (bridge) {
			try {
				splashDown.set(this.bridge.takeDownSplash());
			} catch (Throwable e) {
				logger.severe(()->"An error occured during splash shutdown: "+ e.getClass().getSimpleName() + " - " + e.getMessage());
				logger.severe(()->"This may due to Eclipse issue https://bugs.eclipse.org/bugs/show_bug.cgi?id=572060");
			}
		}
		try {
			Runtime.getRuntime().removeShutdownHook(this);
		} catch (Throwable e) {
			// OK to ignore this, happens when the VM is already shutting down
		}
	}
}