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
package org.gecko.bnd.eclipse.launcher.pre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gecko.eclipse.api.BndEclipseConstants.*; 

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Constants;


public class EclipseLauncherConstantsTest {

	private File installDir;
	
	@BeforeEach
	public void setUp() throws Exception {
		installDir = File.createTempFile("install", "dir");
		System.setProperty(PROP_INSTALL_AREA, installDir.getAbsoluteFile().toURI().toURL().toString());
		installDir.deleteOnExit();
	}
	
	@AfterEach
	public void tearDown() {
		System.getProperties().remove(PROP_CONFIG_AREA);
		System.getProperties().remove(Constants.FRAMEWORK_STORAGE);
		System.getProperties().remove(PROP_LAUNCH_STORAGE_DIR);
		System.getProperties().remove(PROP_LAUNCHER_NAME);
	}

	@Test
	public void testConfigDefault() {
		EclipseLauncherConstants eclipseLauncherConstants = new EclipseLauncherConstants(new String[] {});

		File toTest = new File(installDir, "configuration/framework");
		String configArea = System.getProperty(PROP_CONFIG_AREA);
		String frameworkStorage = System.getProperty(Constants.FRAMEWORK_STORAGE);
		String storageDir = System.getProperty(PROP_LAUNCH_STORAGE_DIR);
		assertThat(configArea).isEqualTo(toTest.getAbsolutePath());
		assertThat(frameworkStorage).isEqualTo(toTest.getAbsolutePath());
		assertThat(storageDir).isEqualTo(toTest.getAbsolutePath());
		assertThat(eclipseLauncherConstants.passThrough).isEmpty();
	}
	
	@Test
	public void testConfigRelative() {
		new EclipseLauncherConstants(new String[] {CONFIGURATION, "test"});
		File toTest = new File(installDir, "test");
		
		String configArea = System.getProperty(PROP_CONFIG_AREA);
		String frameworkStorage = System.getProperty(Constants.FRAMEWORK_STORAGE);
		String storageDir = System.getProperty(PROP_LAUNCH_STORAGE_DIR);
		assertThat(configArea).isEqualTo(toTest.getAbsolutePath());
		assertThat(frameworkStorage).isEqualTo(toTest.getAbsolutePath());
		assertThat(storageDir).isEqualTo(toTest.getAbsolutePath());
	}

	@Test
	public void testConfigAbsolut() throws IOException {
		File conf = File.createTempFile("config", "dir");
		conf.deleteOnExit();
		new EclipseLauncherConstants(new String[] {CONFIGURATION, conf.getAbsolutePath()});
		
		String configArea = System.getProperty(PROP_CONFIG_AREA);
		String frameworkStorage = System.getProperty(Constants.FRAMEWORK_STORAGE);
		String storageDir = System.getProperty(PROP_LAUNCH_STORAGE_DIR);
		assertThat(configArea).isEqualTo(conf.getAbsolutePath());
		assertThat(frameworkStorage).isEqualTo(conf.getAbsolutePath());
		assertThat(storageDir).isEqualTo(conf.getAbsolutePath());
	}

	@Test
	public void testConfigSystemPropRelative() {
		System.setProperty(PROP_CONFIG_AREA, "test");
		new EclipseLauncherConstants(new String[] {});
		
		File toTest = new File(installDir, "test");
		String configArea = System.getProperty(PROP_CONFIG_AREA);
		String frameworkStorage = System.getProperty(Constants.FRAMEWORK_STORAGE);
		String storageDir = System.getProperty(PROP_LAUNCH_STORAGE_DIR);
		assertThat(configArea).isEqualTo("test");
		assertThat(frameworkStorage).isEqualTo(toTest.getAbsolutePath());
		assertThat(storageDir).isEqualTo(toTest.getAbsolutePath());
	}
	
	@Test
	public void testConfigSystemPropAbsolut() throws IOException {
		File conf = File.createTempFile("config", "dir");
		conf.deleteOnExit();
		System.setProperty(PROP_CONFIG_AREA, conf.getAbsolutePath());
		new EclipseLauncherConstants(new String[] {});
		String configArea = System.getProperty(PROP_CONFIG_AREA);
		String frameworkStorage = System.getProperty(Constants.FRAMEWORK_STORAGE);
		String storageDir = System.getProperty(PROP_LAUNCH_STORAGE_DIR);
		assertThat(configArea).isEqualTo(conf.getAbsolutePath());
		assertThat(frameworkStorage).isEqualTo(conf.getAbsolutePath());
		assertThat(storageDir).isEqualTo(conf.getAbsolutePath());
	}

	@Test
	public void testPassThroughArgsAndFields() {
		EclipseLauncherConstants eclipseLauncherConstants = new EclipseLauncherConstants(new String[] {
				"-debug",
				"-clean",
				"-vm", "/path/to/my/java",
				"-startup", "org.gecko.bnd.eclipse.launcher.pre.jar",
				"-runpath", "path/to/my.launcher.jar,path/to/my.library.jar",
				"-os", "linux",
				"-exitdata", "DECAFC0FFEE15BAD",
				"-name", "My App",
				"-some-flag",
				"-some-setting", "some value",
				"extra arg", "another extra arg"
		});
		assertThat(eclipseLauncherConstants.vm).isEqualTo("/path/to/my/java");
		assertThat(eclipseLauncherConstants.propBasedRunPath).isEqualTo(Arrays.asList("path/to/my.launcher.jar", "path/to/my.library.jar"));
		assertThat(eclipseLauncherConstants.exitData).isEqualTo("DECAFC0FFEE15BAD");
		assertThat(System.getProperty(PROP_LAUNCHER_NAME)).isEqualTo("My App");
		assertThat(eclipseLauncherConstants.passThrough).isEqualTo(Arrays.asList(
				"-debug", "-clean", "-os", "linux",
				"-some-flag",
				"-some-setting", "some value",
				"extra arg", "another extra arg"
		));
	}
}
