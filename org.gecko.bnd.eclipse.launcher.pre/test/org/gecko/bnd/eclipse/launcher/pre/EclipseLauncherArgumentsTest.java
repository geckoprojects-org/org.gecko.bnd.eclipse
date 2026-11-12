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
package org.gecko.bnd.eclipse.launcher.pre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gecko.eclipse.api.BndEclipseConstants.*;

import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests how the arguments the native Eclipse launcher hands to the pre-launcher are processed:
 * which are consumed, which end up as system properties and which are passed through to the
 * application.
 */
public class EclipseLauncherArgumentsTest {

	@TempDir
	Path installDir;

	private Properties saved;

	@BeforeEach
	public void setUp() throws Exception {
		saved = (Properties) System.getProperties().clone();
		System.setProperty(PROP_INSTALL_AREA, installDir.toUri().toURL().toString());
	}

	@AfterEach
	public void tearDown() {
		System.setProperties(saved);
	}

	/** What the native launcher passes for a product exported with the bnd launcher plugin. */
	@Test
	public void testNativeLauncherArguments() {
		EclipseLauncherConstants constants = new EclipseLauncherConstants(new String[] { //
				"-os", "win32", "-ws", "win32", "-arch", "x86_64", //
				"-showsplash", "splash/splash.bmp", //
				"-launcher", "C:\\app\\launch_win32.exe", //
				"-name", "Launch_win32", //
				"--launcher.library", "libs/eclipse_11924.dll", //
				"-startup", "plugins/org.gecko.bnd.eclipse.launcher.pre.jar", //
				"--launcher.overrideVmargs", //
				"-clean", //
				"-runpath", "plugins/a.jar,plugins/b.jar", //
				"-vm", "C:\\jdk\\bin\\javaw.exe", //
				"-vmargs", "-Dfoo=bar", "-Xmx1g" });

		assertThat(constants.library).isEqualTo("libs/eclipse_11924.dll");
		assertThat(constants.vm).isEqualTo("C:\\jdk\\bin\\javaw.exe");
		assertThat(constants.propBasedRunPath).containsExactly("plugins/a.jar", "plugins/b.jar");
		assertThat(constants.vmargs).containsExactly("-Dfoo=bar", "-Xmx1g");

		assertThat(System.getProperty(SHOWSPLASH)).isEqualTo("true");
		assertThat(System.getProperty(PROP_SPLASHLOCATION)).isEqualTo("splash/splash.bmp");
		assertThat(System.getProperty(PROP_LAUNCHER)).isEqualTo("C:\\app\\launch_win32.exe");
		assertThat(System.getProperty(PROP_LAUNCHER_NAME)).isEqualTo("Launch_win32");
		// -clean switches the bnd launcher's keep mode off
		assertThat(System.getProperty("launch.keep")).isEqualTo("false");

		// The launcher internals are consumed; what Eclipse/the application needs is passed through.
		assertThat(constants.passThrough).containsExactly("-os", "win32", "-ws", "win32", "-arch", "x86_64",
				"-launcher", "C:\\app\\launch_win32.exe", "-clean");
	}

	@Test
	public void testApplicationArgumentsArePassedThrough() {
		EclipseLauncherConstants constants = new EclipseLauncherConstants(
				new String[] { "-console", "-consoleLog", "-data", "/tmp/ws", "myFile.txt", "-nl", "de_DE" });

		assertThat(constants.nl).isEqualTo("de_DE");
		assertThat(constants.passThrough).containsExactly("-console", "-consoleLog", "-data", "/tmp/ws",
				"myFile.txt", "-nl", "de_DE");
		// no -clean: keep the framework storage
		assertThat(System.getProperty("launch.keep")).isEqualTo("true");
	}

	@Test
	public void testGeckoNoSplash() {
		EclipseLauncherConstants constants = new EclipseLauncherConstants(new String[] { "--nosplash" });
		assertThat(System.getProperty(NOSPLASH)).isEqualTo("true");
		assertThat(constants.passThrough).isEmpty();
	}

	@Test
	public void testEclipseNoSplash() {
		EclipseLauncherConstants constants = new EclipseLauncherConstants(new String[] { "-nosplash" });
		assertThat(System.getProperty(NOSPLASH)).isEqualTo("true");
		assertThat(constants.passThrough).isEmpty();
	}

	@Test
	public void testNoExit() {
		EclipseLauncherConstants constants = new EclipseLauncherConstants(new String[] { "-noExit" });
		assertThat(System.getProperty(PROP_NOSHUTDOWN)).isEqualTo("true");
		// also passed on to Eclipse
		assertThat(constants.passThrough).containsExactly("-noExit");
	}

	@Test
	public void testShowSplashWithoutLocation() {
		EclipseLauncherConstants constants = new EclipseLauncherConstants(new String[] { "-showsplash", "-clean" });
		assertThat(System.getProperty(SHOWSPLASH)).isEqualTo("true");
		assertThat(System.getProperty(PROP_SPLASHLOCATION)).isNull();
		assertThat(constants.passThrough).containsExactly("-clean");
	}

	@Test
	public void testParameterArgumentWithoutValueIsPassedThrough() {
		// -name expects a value; followed by another argument it is left alone
		EclipseLauncherConstants constants = new EclipseLauncherConstants(new String[] { "-name", "-clean" });
		assertThat(System.getProperty(PROP_LAUNCHER_NAME)).isNull();
		assertThat(constants.passThrough).containsExactly("-name", "-clean");
	}

	@Test
	public void testInstallAndInitialize() throws Exception {
		Path app = java.nio.file.Files.createDirectories(installDir.resolve("app"));
		EclipseLauncherConstants constants = new EclipseLauncherConstants(
				new String[] { "-initialize", "-install", app.toString() });
		assertThat(constants.initialize).isTrue();
		// normalized to a directory URL
		assertThat(System.getProperty(PROP_INSTALL_AREA)).isEqualTo(app.toUri().toURL().toString());
		assertThat(constants.installationLocation.toString()).isEqualTo(app.toUri().toURL().toString());
		assertThat(constants.passThrough).containsExactly("-initialize");
	}
}
