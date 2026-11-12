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
package org.gecko.eclipse.compatibility.equinox.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.junit.jupiter.api.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 * The component splits the launcher arguments into framework and application arguments, the
 * way EclipseStarter does it, and sets the matching Equinox configuration properties.
 */
public class EquinoxConfigInitializerImplTest {

	private EquinoxConfiguration activate(String... args) throws Exception {
		EquinoxConfiguration config = mock(EquinoxConfiguration.class);
		@SuppressWarnings("unchecked")
		ServiceReference<Object> launcher = mock(ServiceReference.class);
		when(launcher.getProperty("launcher.arguments")).thenReturn(args);

		EquinoxConfigInitializerImpl component = new EquinoxConfigInitializerImpl();
		set(component, "envInfo", config);
		set(component, "launcher", launcher);
		component.activate(mock(ComponentContext.class));
		return config;
	}

	private static void set(Object target, String name, Object value) throws Exception {
		Field field = EquinoxConfigInitializerImpl.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(target, value);
	}

	@Test
	public void testFrameworkAndApplicationArguments() throws Exception {
		String[] args = { "-os", "linux", "-ws", "gtk", "-arch", "x86_64", "-clean", "-consoleLog", "-data",
				"/tmp/ws", "-configuration", "/tmp/conf", "-nl", "de", "-launcher", "/opt/app/launch", "myFile",
				"-myAppArg", "value" };
		EquinoxConfiguration config = activate(args);

		verify(config).setProperty("osgi.os", "linux");
		verify(config).setProperty("osgi.ws", "gtk");
		verify(config).setProperty("osgi.arch", "x86_64");
		verify(config).setProperty("osgi.clean", "true");
		verify(config).setProperty("eclipse.consoleLog", "true");
		verify(config).setProperty(EquinoxLocations.PROP_INSTANCE_AREA, "/tmp/ws");
		verify(config).setProperty(EquinoxLocations.PROP_CONFIG_AREA, "/tmp/conf");
		verify(config).setProperty("osgi.nl", "de");
		verify(config).setProperty(EquinoxLocations.PROP_LAUNCHER, "/opt/app/launch");

		verify(config).setAllArgs(args);
		verify(config).setFrameworkArgs(new String[] { "-os", "linux", "-ws", "gtk", "-arch", "x86_64", "-clean",
				"-consoleLog", "-data", "/tmp/ws", "-configuration", "/tmp/conf", "-nl", "de", "-launcher",
				"/opt/app/launch" });
		verify(config).setAppArgs(new String[] { "myFile", "-myAppArg", "value" });
	}

	@Test
	public void testFlagsWithoutValue() throws Exception {
		EquinoxConfiguration config = activate("-debug", "-dev", "-console", "-noExit");

		verify(config).setProperty("osgi.debug", "");
		verify(config).setProperty("osgi.dev", "");
		verify(config).setProperty("osgi.console", "");
		verify(config).setProperty("osgi.noShutdown", "true");
		verify(config).setAppArgs(new String[0]);
	}

	@Test
	public void testConsoleWithPortAndDebugOptions() throws Exception {
		EquinoxConfiguration config = activate("-console", "1234", "-debug", "/tmp/.options");

		verify(config).setProperty("osgi.console", "1234");
		verify(config).setProperty("osgi.debug", "/tmp/.options");
	}

	@Test
	public void testOnlyApplicationArguments() throws Exception {
		String[] args = { "-myAppArg", "value", "file.txt" };
		EquinoxConfiguration config = activate(args);

		verify(config, never()).setProperty(anyString(), anyString());
		verify(config).setFrameworkArgs(new String[0]);
		verify(config).setAppArgs(args);
	}

	@Test
	public void testNoArgumentsResetsPreviousRun() throws Exception {
		// the arguments are kept in static fields; a second activation must not see the first one's
		activate("-myAppArg");
		EquinoxConfiguration config = activate();

		verify(config).setAllArgs(new String[0]);
		verify(config).setFrameworkArgs(new String[0]);
		verify(config).setAppArgs(new String[0]);
	}
}
