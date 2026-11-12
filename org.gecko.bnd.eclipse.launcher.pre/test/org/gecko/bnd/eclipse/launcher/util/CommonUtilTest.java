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
package org.gecko.bnd.eclipse.launcher.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gecko.eclipse.api.BndEclipseConstants.PROP_INSTALL_AREA;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CommonUtilTest {

	@TempDir
	Path tmp;

	@AfterEach
	public void tearDown() {
		System.getProperties().remove(PROP_INSTALL_AREA);
	}

	@Test
	public void testGetArrayFromList() {
		assertThat(CommonUtil.getArrayFromList(null)).isEmpty();
		assertThat(CommonUtil.getArrayFromList("  ")).isEmpty();
		assertThat(CommonUtil.getArrayFromList(" a, b ,,c ,")).containsExactly("a", "b", "c");
	}

	@Test
	public void testFindMaxPicksHighestVersion() {
		String[] candidates = { "org.eclipse.osgi_3.9.0.jar", "org.eclipse.osgi_3.24.300.v2026.jar",
				"org.eclipse.osgi_3.24.10.jar", "org.eclipse.osgi" };
		assertThat(CommonUtil.findMax("org.eclipse.osgi", candidates)).isEqualTo(1);
	}

	@Test
	public void testFindMaxComparesQualifier() {
		String[] candidates = { "foo_1.0.0.a", "foo_1.0.0.c", "foo_1.0.0.b" };
		assertThat(CommonUtil.findMax("foo", candidates)).isEqualTo(1);
		assertThat(CommonUtil.findMax("foo", new String[0])).isEqualTo(-1);
	}

	@Test
	public void testSearchForVersionedFileAndDirectory() throws IOException {
		Files.createFile(tmp.resolve("org.eclipse.osgi_3.20.0.jar"));
		Files.createFile(tmp.resolve("org.eclipse.osgi_3.24.300.jar"));
		// a different bundle whose name only starts with the target
		Files.createFile(tmp.resolve("org.eclipse.osgi.services_9.0.0.jar"));
		// '_' that is not followed by a version (foo.x86_64 case)
		Files.createDirectories(tmp.resolve("org.eclipse.equinox.launcher_win32.x86_64"));
		Files.createDirectories(tmp.resolve("org.eclipse.equinox.launcher_1.8.0"));

		String osgi = CommonUtil.searchFor("org.eclipse.osgi", tmp.toString());
		assertThat(osgi).endsWith("/org.eclipse.osgi_3.24.300.jar");
		assertThat(osgi).doesNotContain("\\");

		String launcher = CommonUtil.searchFor("org.eclipse.equinox.launcher", tmp.toString());
		assertThat(launcher).endsWith("/org.eclipse.equinox.launcher_1.8.0/");

		assertThat(CommonUtil.searchFor("does.not.exist", tmp.toString())).isNull();
		assertThat(CommonUtil.searchFor("org.eclipse.osgi", tmp.resolve("missing").toString())).isNull();
	}

	@Test
	public void testDecodeKeepsPlus() {
		assertThat(CommonUtil.decode("file:/C:/my%20dir/a+b.jar")).isEqualTo("file:/C:/my dir/a+b.jar");
	}

	@Test
	public void testIsJAR() throws IOException {
		Path jar = Files.createFile(tmp.resolve("a.jar"));
		assertThat(CommonUtil.isJAR(jar.toString())).isTrue();
		assertThat(CommonUtil.isJAR(tmp.toString())).isFalse();
	}

	@Test
	public void testBuildURL() throws Exception {
		assertThat(CommonUtil.buildURL(null, true)).isNull();
		assertThat(CommonUtil.buildURL("https://example.org/x", false)).isEqualTo(new URL("https://example.org/x"));

		File dir = tmp.toFile();
		URL withSlash = CommonUtil.buildURL("file:" + dir.getAbsolutePath(), true);
		assertThat(withSlash.getFile()).endsWith("/");
		URL withoutSlash = CommonUtil.buildURL("file:" + dir.getAbsolutePath(), false);
		assertThat(withoutSlash.getFile()).doesNotEndWith("/");

		// no scheme: treated as a file
		assertThat(CommonUtil.buildURL(dir.getAbsolutePath(), true).getProtocol()).isEqualTo("file");
	}

	@Test
	public void testBuildURLResolvesRelativeFilesAgainstInstallArea() throws Exception {
		Path config = Files.createDirectories(tmp.resolve("configuration"));
		System.setProperty(PROP_INSTALL_AREA, tmp.toString());

		URL url = CommonUtil.buildURL("file:configuration", true);
		assertThat(new File(url.toURI()).getCanonicalFile()).isEqualTo(config.toFile().getCanonicalFile());
		assertThat(url.getFile()).endsWith("/");
	}
}
