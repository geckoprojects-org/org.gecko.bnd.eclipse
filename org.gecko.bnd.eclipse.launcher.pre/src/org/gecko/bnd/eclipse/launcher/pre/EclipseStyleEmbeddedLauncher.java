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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.gecko.bnd.eclipse.launcher.util.CommonUtil;
import static org.gecko.eclipse.api.BndEclipseConstants.*; 

import aQute.lib.io.IOConstants;

public class EclipseStyleEmbeddedLauncher {

	private static final String BND_LAUNCHER = "aQute.launcher.Launcher";
	static final int BUFFER_SIZE = IOConstants.PAGE_SIZE * 16;

	public static final String EMBEDDED_RUNPATH	= "Embedded-Runpath";
	public static Manifest MANIFEST;

	public static void main(String[] args) throws Throwable {
		boolean isVerbose = isTrace();
		findAndExecute(isVerbose, "main", void.class, args);
	}

	/**
	 * Runs the Launcher like the main method, but returns an usable exit Code.
	 * This Method was introduced to enable compatibility with the Equinox
	 * native executables.
	 * 
	 * @param args the arguments to run the Launcher with
	 * @return an exit code
	 * @throws Throwable
	 */
	public int run(String... args) throws Throwable {

		boolean isVerbose = isTrace();

		if (isVerbose) {
			log("The following arguments are given:");
			for (String arg : args) {
				log(arg);
			}
		}

		String methodName = "run";
		Class<Integer> returnType = int.class;

		return findAndExecute(isVerbose, methodName, returnType, args);
	}

	/**
	 * @param isVerbose should we log debug messages
	 * @param methodName the method name to look for
	 * @param returnType the expected return type
	 * @param args the arguments for the method
	 * @return what ever the method returns
	 * @throws Throwable
	 */
	private static <T> T findAndExecute(boolean isVerbose, String methodName, Class<T> returnType, String... args)
		throws Throwable, InvocationTargetException {
		ClassLoader cl = EclipseStyleEmbeddedLauncher.class.getClassLoader();

		EclipseLauncherConstants props = new EclipseLauncherConstants(args);

		if(props.library != null) {
			System.setProperty(LIBRARY , props.library);	
		}
		
		if(props.initialize) {
			System.setProperty(PROP_ECLIPSE_INITIALIZE , Boolean.TRUE.toString());	
		}
		
		//the launcher constants remove some constants. We have to do the following to avoid null values
		args = Arrays.asList(args).stream().filter(s -> s != null).collect(Collectors.toList()).toArray(new String[0]);
		
		if (isVerbose) {
			log("The following arguments after props:");
			for (String arg : args) {
				log(arg);
			}
		}
		
		List<URL> classpath = new ArrayList<>();
		
		if (isVerbose)
			log("looking for + " + EMBEDDED_RUNPATH + " in META-INF/MANIFEST.MF");
		
		Enumeration<URL> manifests = cl.getResources("META-INF/MANIFEST.MF");
		while (manifests.hasMoreElements()) {
			URL murl = manifests.nextElement();

			if (isVerbose)
				log("found a Manifest %s", murl.toString());

			Manifest m = new Manifest(murl.openStream());
			String runpath = m.getMainAttributes()
				.getValue(EMBEDDED_RUNPATH);
			if (isVerbose)
				log("Going through the following runpath %s", runpath);
			if (runpath != null) {
				MANIFEST = m;
				
				for (String path : runpath.split("\\s*,\\s*")) {
					URL url = toFileURL(cl.getResource(path));
					if (isVerbose)
						log("Adding to classpath %s", url.toString());
					classpath.add(url);
				}
				break;
			}
		} 
		
		addPropertyBasedRunPath(classpath, props);
		applyLauncherProperites(props);
		
		if (isVerbose)
			log("creating URLClassLoader");
		try (URLClassLoader urlc = new URLClassLoader(classpath.toArray(new URL[0]), cl)) {
			
			log("Try to load %s", BND_LAUNCHER);
			
			Class<?> launcher = urlc.loadClass(BND_LAUNCHER);
			if(launcher == null) {
				throw new RuntimeException("Found Nothing to launch. Maybe no " + EMBEDDED_RUNPATH + " was set");
			}
			if (isVerbose)
				log("looking for method %s with return type %s and %s capable of handling the splash", methodName, returnType.toString(), Runnable.class.getName());
			MethodHandle mh = MethodHandles.publicLookup()
				.findStatic(launcher, methodName, MethodType.methodType(returnType, String[].class));
			if(mh == null) {
				throw new RuntimeException("Found Nothing to launch. Maybe no " + EMBEDDED_RUNPATH + " was set");
			}
			try {
				if (isVerbose)
					log("found method and start executing with args " );
				return (T) mh.invoke(args);
			} catch (Error | Exception e) {
				throw e;
			} catch (Throwable e) {
				throw new InvocationTargetException(e);
			} finally {
			}
		} 
	}
	
	private static void applyLauncherProperites(EclipseLauncherConstants props) throws Exception {
		String launcherProperties = System.getProperty(LAUNCHER_PROPERTIES);
		if(launcherProperties == null) {
			File propsFile = new File(new File(props.installationLocation.toURI()), "configuration" + File.separator + LAUNCHER_PROPERTIES);
			System.setProperty(LAUNCHER_PROPERTIES, propsFile.getAbsolutePath());
		}
	}

	/**
	 * @param classpath
	 */
	private static void addPropertyBasedRunPath(List<URL> classpath, EclipseLauncherConstants props) {
		props.propBasedRunPath.stream().map(pathElement -> convertToBundleUrl(pathElement, props)).map(url -> {
			log("Adding to classpath %s", url);
			return url;
		}
		).filter(u -> u != null).forEach(classpath::add);
	}

	private static URL convertToBundleUrl(String path, EclipseLauncherConstants props) {
		File f = new File(path);
		try {
			if(f.exists()) {
				return f.toURI().toURL();
			} else {
				//Mybe it is a relative Path
				URL url = new URL(props.installationLocation.toString() + path);	
				f = new File(url.toURI());
				if(f.exists()) {
					return f.toURI().toURL();
				}
			}
		} catch (Exception e) {
			error("Could not convert path %s to URL. Message was %s", path, e.getMessage());
		}
		return null;
	}
	
	private static void log(String message, Object... args) {
		CommonUtil.log(EclipseStyleEmbeddedLauncher.class, message, args);
	}

	private static void error(String message, Object... args) {
		CommonUtil.error(EclipseStyleEmbeddedLauncher.class, message, args);
	}

	private static boolean isTrace() {
		return Boolean.getBoolean(LAUNCH_TRACE);
	}

	private static URL toFileURL(URL resource) throws IOException {
		//
		// Don't bother copying file urls
		//
		if (resource.getProtocol()
			.equalsIgnoreCase("file"))
			return resource;

		//
		// Need to make a copy to a temp file
		//

		File f = File.createTempFile("resource", ".jar");
		Files.createDirectories(f.getParentFile()
			.toPath());
		try (InputStream in = resource.openStream(); OutputStream out = Files.newOutputStream(f.toPath())) {
			byte[] buffer = new byte[BUFFER_SIZE];
			for (int size; (size = in.read(buffer, 0, buffer.length)) > 0;) {
				out.write(buffer, 0, size);
			}
		}
		f.deleteOnExit();
		return f.toURI()
			.toURL();
	}

}
