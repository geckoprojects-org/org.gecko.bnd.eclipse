/**
 * Copyright (c) 2012 - 2026 Data In Motion and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.bnd.eclipse.launcher.smoke;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Headless Eclipse application for {@code EclipseStyleLaunchSmokeTest}, packed into a bundle by the
 * test. It writes what it got to the file named by the system property {@code smoke.marker} and
 * returns at once, which shuts the product down. The application argument {@code exit=<n>} sets
 * the value it returns, which becomes the exit code of the product. No SWT, no display.
 */
public class SmokeTestApplication implements IApplication {

	private static final String EXIT = "exit=";

	@Override
	public Object start(IApplicationContext context) throws Exception {
		context.applicationRunning();
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		if (args == null) {
			args = new String[0];
		}
		Integer exit = IApplication.EXIT_OK;
		for (String arg : args) {
			if (arg.startsWith(EXIT)) {
				exit = Integer.valueOf(arg.substring(EXIT.length()));
			}
		}

		List<String> lines = new ArrayList<>();
		lines.add("started");
		lines.add("args=" + String.join(" ", args));
		lines.add("install.area=" + System.getProperty("osgi.install.area"));
		lines.add("launcher=" + System.getProperty("eclipse.launcher"));
		Files.write(Path.of(System.getProperty("smoke.marker")), lines, StandardCharsets.UTF_8);
		return exit;
	}

	@Override
	public void stop() {
		// returns on its own
	}
}
