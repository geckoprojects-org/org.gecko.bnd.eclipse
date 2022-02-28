/**
 * Copyright (c) 2012 - 2022 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors:
 *      BND - initial API and implementation as ProjectLauncher
 *      Data In Motion - initial API and implementation
 */
package org.gecko.bnd.eclipse.launcher.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Jar;

public class ProjectLaunchImplTest {

	private Workspace	ws;
	private File		wsDir;

	File						base					= new File("").getAbsoluteFile();
	private File generatedDir;
	private File result;

//	@After
	public void after() {
		ws.close();
//		if(result != null) {
//			result.delete();
//		}
	}
	
//	@Before
	public void setUp() throws Exception {
		wsDir = new File("../").getAbsoluteFile();
		System.out.println("Workspace dir " + wsDir.toString());
		if(wsDir.exists()) {
			System.out.println("Workspace dir " + wsDir.toString() + " exists");
		}
		ws = Workspace.getWorkspace(wsDir);
		generatedDir = new File(base, "generated/");
	}

	protected void tearDown() throws Exception {
		ws.close();
	}

//	@Test
	public void testPluginRun() throws Exception {
		setUp();
		File bndrun = new File("testresources/eclipseStyleTest.bndrun");
		
		assertTrue(bndrun.exists());
				
		Run run = Run.createRun(ws,bndrun);
		
		try {
			ProjectLauncher projectLauncher = run.getProjectLauncher();
			assertTrue(projectLauncher instanceof EclipseStyleProjectLauncherImpl);
			int result = projectLauncher.launch();
			projectLauncher.close();
			assertEquals(42, result);
		} finally {
			run.close();
			after();
		}
	}

//	@Test
	public void testExport() throws Exception {
		
		File bndrun = new File("testresources/eclipseStyleTestLinux.bndrun");
		
		assertTrue(bndrun.exists());
		
		Run run = Run.createRun(ws,bndrun);
		
		try {
			ProjectLauncher projectLauncher = run.getProjectLauncher();
			assertTrue(projectLauncher instanceof EclipseStyleProjectLauncherImpl);
			Jar executable = projectLauncher.executable();
			assertNotNull(executable);
			
			result = new File(generatedDir, "test.zip");
			executable.write(result);
			
			assertTrue(result.exists());
		} finally {
			run.close();
			ws.close();
		}
	}
}
