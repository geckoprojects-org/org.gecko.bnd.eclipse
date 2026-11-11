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
package org.gecko.eclipse.compatibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceListener;
import org.osgi.service.condition.Condition;

/**
 * Tests what the starter does on activation, depending on the Eclipse properties. The application
 * itself is not started (that needs a running Equinox), only the tracking of its descriptor.
 */
public class EclipsePlatformStarterTest {

	private BundleContext ctx;
	private EquinoxConfiguration envInfo;
	private Bundle systemBundle;
	private Bundle runtime;

	@BeforeEach
	public void setUp() {
		ctx = mock(BundleContext.class);
		envInfo = mock(EquinoxConfiguration.class);
		systemBundle = mock(Bundle.class);
		runtime = mock(Bundle.class);
		when(runtime.getSymbolicName()).thenReturn("org.eclipse.core.runtime");
		Bundle other = mock(Bundle.class);
		when(other.getSymbolicName()).thenReturn("some.bundle");
		when(ctx.getBundles()).thenReturn(new Bundle[] { systemBundle, other, runtime });
		when(ctx.getBundle(0)).thenReturn(systemBundle);
	}

	private EclipsePlatformStarter start() throws Exception {
		return new EclipsePlatformStarter(ctx, mock(FrameworkLog.class), envInfo, mock(Condition.class));
	}

	@Test
	public void testTracksTheConfiguredApplication() throws Exception {
		when(ctx.getProperty("eclipse.application")).thenReturn("my.app");

		EclipsePlatformStarter starter = start();

		verify(runtime).start();
		ArgumentCaptor<String> filter = ArgumentCaptor.forClass(String.class);
		verify(ctx).addServiceListener(any(ServiceListener.class), filter.capture());
		assertThat(filter.getValue()).contains("(objectClass=org.osgi.service.application.ApplicationDescriptor)")
				.contains("(service.pid=my.app)");
		verify(systemBundle, never()).stop();

		starter.deactivate();
		verify(ctx).removeServiceListener(any(ServiceListener.class));
	}

	@Test
	public void testApplicationFromEnvironmentInfo() throws Exception {
		// the framework properties win, the Equinox configuration is the fallback
		when(envInfo.getProperty("eclipse.application")).thenReturn("env.app");

		start();

		ArgumentCaptor<String> filter = ArgumentCaptor.forClass(String.class);
		verify(ctx).addServiceListener(any(ServiceListener.class), filter.capture());
		assertThat(filter.getValue()).contains("(service.pid=env.app)");
	}

	@Test
	public void testInitializeModeStopsTheFramework() throws Exception {
		when(ctx.getProperty("eclipse.initialize")).thenReturn("true");
		when(ctx.getProperty("eclipse.application")).thenReturn("my.app");

		start();

		verify(systemBundle).stop();
		verify(ctx, never()).addServiceListener(any(ServiceListener.class), anyString());
	}

	@Test
	public void testIgnoreApp() throws Exception {
		when(ctx.getProperty("eclipse.ignoreApp")).thenReturn("true");
		when(ctx.getProperty("eclipse.application")).thenReturn("my.app");

		start();

		verify(ctx, never()).addServiceListener(any(ServiceListener.class), anyString());
		verify(systemBundle, never()).stop();
	}

	@Test
	public void testNoApplicationConfigured() throws Exception {
		start();

		verify(runtime).start();
		verify(ctx, never()).addServiceListener(any(ServiceListener.class), anyString());
		verify(systemBundle, never()).stop();
	}

	@Test
	public void testRuntimeBundleFailsToStart() throws Exception {
		doThrow(new BundleException("broken")).when(runtime).start();

		assertThatThrownBy(this::start).isInstanceOf(BundleException.class).hasMessageContaining("broken");
		verify(ctx, never()).addServiceListener(any(ServiceListener.class), anyString());
	}
}
