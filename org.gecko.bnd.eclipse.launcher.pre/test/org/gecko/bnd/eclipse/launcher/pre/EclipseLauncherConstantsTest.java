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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;

public class EclipseLauncherConstantsTest {

	private File installDir;
	
	@Before
	public void setUp() throws Exception {
		installDir = File.createTempFile("install", "dir");
		System.setProperty(PROP_INSTALL_AREA, installDir.getAbsoluteFile().toURI().toURL().toString());
		installDir.deleteOnExit();
	}
	
	@After
	public void tearDown() {
		System.getProperties().remove(PROP_CONFIG_AREA);
		System.getProperties().remove(Constants.FRAMEWORK_STORAGE);
		System.getProperties().remove(PROP_LAUNCH_STORAGE_DIR);
	}

	@Test
	public void testConfigDefault() {
		new EclipseLauncherConstants(new String[] {});

		File toTest = new File(installDir, "configuration/framework");
		String configArea = System.getProperty(PROP_CONFIG_AREA);
		String frameworkStorage = System.getProperty(Constants.FRAMEWORK_STORAGE);
		String storageDir = System.getProperty(PROP_LAUNCH_STORAGE_DIR);
		assertThat(configArea).isEqualTo(toTest.getAbsolutePath());
		assertThat(frameworkStorage).isEqualTo(toTest.getAbsolutePath());
		assertThat(storageDir).isEqualTo(toTest.getAbsolutePath());
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

}
