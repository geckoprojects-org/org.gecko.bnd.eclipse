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
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.lib.io.IO;

/**
 * Exports a headless product with the launcher plugin and starts it the way a user starts an
 * exported product: native executable and its library, pre-launcher, bnd launcher, Equinox, the
 * compatibility components and finally an Eclipse application. The application
 * ({@code SmokeTestApplication}) uses no SWT; it writes a marker file and returns, which shuts the
 * product down. So the test needs no display. It runs on the operating system it runs on (Linux
 * or Windows, x86_64) and is skipped elsewhere.
 */
public class EclipseStyleLaunchSmokeTest {

	private static final String APP_BSN = "org.gecko.bnd.eclipse.launcher.smoke";

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
	@ValueSource(ints = { 0, 7 })
	public void testLaunchExportedProduct(int exitCode, @TempDir Path tmp) throws Exception {
		String os = currentOs();
		Assumptions.assumeTrue(os != null, "no native launcher for " + System.getProperty("os.name") + " "
				+ System.getProperty("os.arch") + " in this workspace");
		boolean windows = "win32".equals(os);

		Path marker = tmp.resolve("marker.txt");
		Path productDir = tmp.resolve("product");
		exportProduct(os, buildApplicationBundle(tmp.resolve("smoke-app.jar").toFile()), marker, productDir);

		File executable = productDir.resolve(windows ? "smoke_win32.exe" : "smoke_linux").toFile();
		assertThat(executable).isFile();
		// a jar keeps no file permissions
		assertThat(executable.setExecutable(true)).isTrue();

		String java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
		Path log = tmp.resolve("launch.log");
		Process process = new ProcessBuilder(executable.getAbsolutePath(),
				// no message boxes, no splash: there is no display
				"--launcher.suppressErrors", "-nosplash",
				// the JVM running this test; the product brings none
				"-vm", java,
				// an application argument
				"exit=" + exitCode)
				.directory(productDir.toFile())
				.redirectErrorStream(true)
				.redirectOutput(log.toFile())
				.start();
		if (!process.waitFor(2, TimeUnit.MINUTES)) {
			process.descendants().forEach(ProcessHandle::destroyForcibly);
			process.destroyForcibly();
			fail("The product did not shut down within 2 minutes:%n%s", read(log));
		}
		String output = read(log);

		// the .ini -vmargs reached the JVM (smoke.marker) and the application ran
		assertThat(marker).as("marker of the application; launch output:%n%s", output).exists();
		List<String> lines = Files.readAllLines(marker, StandardCharsets.UTF_8);
		assertThat(lines).as("launch output:%n%s", output).startsWith("started",
				// the command line reaches the application
				"args=exit=" + exitCode);
		// the install area is the product folder, the launcher is the native executable
		assertThat(value(lines, "install.area=").toLowerCase(Locale.ROOT))
				.endsWith("/product/");
		assertThat(new File(value(lines, "launcher=")).getCanonicalFile())
				.isEqualTo(executable.getCanonicalFile());
		// what the application returns is the exit code of the product
		assertThat(process.exitValue()).as("exit code; launch output:%n%s", output).isEqualTo(exitCode);
	}

	private void exportProduct(String os, File app, Path marker, Path productDir) throws Exception {
		try (Run run = Run.createRun(workspace, IO.getFile("testresources/smoke_" + os + ".bndrun"))) {
			// bnd properties: forward slashes on Windows, too
			run.setProperty("smoke.app", app.getAbsolutePath().replace(File.separatorChar, '/'));
			run.setProperty("smoke.marker", marker.toAbsolutePath().toString().replace(File.separatorChar, '/'));
			EclipseStyleProjectLauncherImpl launcher = new EclipseStyleProjectLauncherImpl(run);
			// what Project.getProjectLauncher() does after the constructor
			launcher.update();
			try (Jar product = launcher.executable()) {
				assertThat(run.getErrors()).isEmpty();
				assertThat(launcher.getErrors()).isEmpty();
				assertThat(product).isNotNull();
				product.expand(productDir.toFile());
			}
		}
	}

	/**
	 * The bundle with {@code SmokeTestApplication} and its plugin.xml, from the compiled test classes.
	 */
	private File buildApplicationBundle(File target) throws Exception {
		Project project = workspace.getProject("org.gecko.bnd.eclipse.launcher");
		assertThat(project).isNotNull();
		try (Builder builder = new Builder()) {
			builder.setBase(project.getBase());
			builder.addClasspath(project.getTestOutput());
			// singleton: the extension registry ignores the plugin.xml of other bundles
			builder.setProperty("Bundle-SymbolicName", APP_BSN + ";singleton:=true");
			builder.setProperty("Bundle-Version", "1.0.0");
			builder.setProperty("-includepackage", APP_BSN);
			builder.setProperty("-includeresource", "plugin.xml=testresources/smoke/plugin.xml");
			builder.setProperty("Import-Package", "*");
			try (Jar jar = builder.build()) {
				assertThat(builder.getErrors()).isEmpty();
				jar.write(target);
			}
		}
		return target;
	}

	private static String value(List<String> lines, String prefix) {
		return lines.stream()
				.filter(l -> l.startsWith(prefix))
				.map(l -> l.substring(prefix.length()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no " + prefix + " in " + lines));
	}

	private static String currentOs() {
		String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch");
		if (!"amd64".equals(arch) && !"x86_64".equals(arch)) {
			return null;
		}
		if (name.startsWith("windows")) {
			return "win32";
		}
		if (name.startsWith("linux")) {
			return "linux";
		}
		return null;
	}

	private static String read(Path file) throws Exception {
		return Files.exists(file) ? Files.readString(file, StandardCharsets.ISO_8859_1) : "";
	}
}
