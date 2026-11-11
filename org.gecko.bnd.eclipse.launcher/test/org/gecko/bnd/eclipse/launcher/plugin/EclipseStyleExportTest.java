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
package org.gecko.bnd.eclipse.launcher.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

/**
 * Exports minimal products with the launcher plugin of this workspace and checks the product
 * layout. Nothing is launched, so the test needs no display.
 */
public class EclipseStyleExportTest {

	private static Workspace workspace;

	@BeforeAll
	public static void setUp() throws Exception {
		// Gradle runs the tests in the project directory
		workspace = Workspace.getWorkspace(new File("..").getCanonicalFile());
		assertThat(workspace.getErrors()).isEmpty();
	}

	@AfterAll
	public static void tearDown() {
		if (workspace != null) {
			workspace.close();
		}
	}

	@ParameterizedTest
	@CsvSource({ "linux, export_linux, .so", "win32, export_win32.exe, .dll" })
	public void testExport(String os, String executable, String libExtension) throws Exception {
		File bndrun = IO.getFile("testresources/export_" + os + ".bndrun");
		assertThat(bndrun).isFile();

		try (Run run = Run.createRun(workspace, bndrun)) {
			EclipseStyleProjectLauncherImpl launcher = new EclipseStyleProjectLauncherImpl(run);
			// what Project.getProjectLauncher() does after the constructor
			launcher.update();
			try (Jar product = launcher.executable()) {
				assertThat(run.getErrors()).isEmpty();
				assertThat(launcher.getErrors()).isEmpty();
				// e.g. "Cyclic or multiple include" when the plugin re-reads the workspace properties
				assertThat(workspace.getErrors()).isEmpty();
				assertThat(product).isNotNull();

				Set<String> entries = product.getResources().keySet();
				String name = "export_" + os;

				// native launcher (renamed after the bndrun), its library, pre-launcher and config
				assertThat(entries).contains(executable, name + ".ini",
						"plugins/org.gecko.bnd.eclipse.launcher.pre.jar", "configuration/launcher.properties");
				List<String> libs = entries.stream()
						.filter(e -> e.startsWith("libs/eclipse_") && e.endsWith(libExtension))
						.collect(Collectors.toList());
				assertThat(libs).hasSize(1);

				// framework on the runpath, run bundles in plugins/
				assertThat(entries).anyMatch(e -> e.matches("plugins/org\\.eclipse\\.osgi-.*\\.jar"));
				assertThat(entries).anyMatch(e -> e.matches("plugins/org\\.apache\\.felix\\.scr-.*\\.jar"));

				checkIni(read(product.getResource(name + ".ini")), libs.get(0));
				checkLauncherProperties(product.getResource("configuration/launcher.properties"));
			}
		}
	}

	private void checkIni(String ini, String lib) {
		List<String> lines = Arrays.asList(ini.split("\\R"));
		// the native launcher only understands --launcher.library (1.x wrote -library)
		assertThat(lines).startsWith("-startup", "plugins/org.gecko.bnd.eclipse.launcher.pre.jar",
				"--launcher.library", lib);
		assertThat(lines).doesNotContain("-library");
		assertThat(lines).contains("-consoleLog", "-clean");

		int runpath = lines.indexOf("-runpath");
		assertThat(runpath).isPositive();
		assertThat(lines.get(runpath + 1)).contains("plugins/org.eclipse.osgi-");

		int vmargs = lines.indexOf("-vmargs");
		assertThat(vmargs).isGreaterThan(runpath);
		// everything after -vmargs goes to the JVM
		assertThat(lines.subList(vmargs, lines.size())).contains("-Dexport.test=true");
	}

	private void checkLauncherProperties(Resource resource) throws Exception {
		Properties properties = new Properties();
		try (InputStream in = resource.openInputStream()) {
			properties.load(in);
		}
		// run bundles relative to the install location of the product
		assertThat(properties.getProperty("launch.bundles"))
				.startsWith("${launcher.installLocation}/plugins/org.apache.felix.scr-");
		assertThat(properties.getProperty("launch.embedded")).isEqualTo("false");
		assertThat(properties.getProperty("osgi.install.area")).isEqualTo("../");
		assertThat(properties.getProperty("eclipse.application")).isEqualTo("test.application");
	}

	private String read(Resource resource) throws Exception {
		assertThat(resource).isNotNull();
		try (InputStream in = resource.openInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
