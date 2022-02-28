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
package org.gecko.eclipse.api;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * <p>
 * This is an example of an interface that is expected to be implemented by Consumers of the API; for example this
 * interface may define a listener or a callback. Adding methods to this interface is a MAJOR change, because ALL
 * clients are affected.
 * </p>
 * 
 * @see ConsumerType
 * @since 1.0
 */
public interface BndEclipseConstants {

	public static final String BSN_ECLIPSE_CORE_RUNTIME = "org.eclipse.core.runtime";
	
	public static final String CONDITION_EQUINOX_CONFIG = "equinoxConfig"; //$NON-NLS-1$
	
	// this is more of an Eclipse argument but this OSGi implementation stores its 
	// metadata alongside Eclipse's.
	public static final String DATA = "-data"; //$NON-NLS-1$
	
	// command line arguments
	public static final String CLEAN = "-clean"; //$NON-NLS-1$
	public static final String CONSOLE = "-console"; //$NON-NLS-1$
	public static final String CONSOLE_LOG = "-consoleLog"; //$NON-NLS-1$
	public static final String DEBUG = "-debug"; //$NON-NLS-1$
	public static final String DEV = "-dev"; //$NON-NLS-1$
	public static final String WS = "-ws"; //$NON-NLS-1$
	public static final String OS = "-os"; //$NON-NLS-1$
	public static final String ARCH = "-arch"; //$NON-NLS-1$
	public static final String NL = "-nl"; //$NON-NLS-1$
	public static final String NAME = "-name"; //$NON-NLS-1$
	public static final String NL_EXTENSIONS = "-nlExtensions"; //$NON-NLS-1$
	public static final String CONFIGURATION = "-configuration"; //$NON-NLS-1$	
	public static final String USER = "-user"; //$NON-NLS-1$
	public static final String NOEXIT = "-noExit"; //$NON-NLS-1$
	public static final String LAUNCHER = "-launcher"; //$NON-NLS-1$
	public static final String INITIALIZE = "-initialize"; //$NON-NLS-1$
	public static final String VMARGS = "-vmargs"; //$NON-NLS-1$
	public static final String NOSPLASH = "--nosplash"; //$NON-NLS-1$
	public static final String SHOWSPLASH = "-showsplash"; //$NON-NLS-1$
	public static final String LIBRARY = "--launcher.library"; //$NON-NLS-1$
	public static final String LAUNCHER_PROPERTIES = "launcher.properties"; //$NON-NLS-1$
	public static final String ENDSPLASH = "-endsplash"; //$NON-NLS-1$
	public static final String SPLASH_IMAGE = "splash.bmp"; //$NON-NLS-1$
	public static final String STARTUP = "-startup"; //$NON-NLS-1$
	
	public static final String PROP_BUNDLES = "osgi.bundles"; //$NON-NLS-1$
	public static final String PROP_BUNDLES_STARTLEVEL = "osgi.bundles.defaultStartLevel"; //$NON-NLS-1$ //The start level used to install the bundles
	public static final String PROP_EXTENSIONS = "osgi.framework.extensions"; //$NON-NLS-1$
	public static final String PROP_INITIAL_STARTLEVEL = "osgi.startLevel"; //$NON-NLS-1$ //The start level when the fwl start
	public static final String PROP_DEBUG = "osgi.debug"; //$NON-NLS-1$
	public static final String PROP_DEV = "osgi.dev"; //$NON-NLS-1$
	public static final String PROP_CLEAN = "osgi.clean"; //$NON-NLS-1$
	public static final String PROP_CONSOLE = "osgi.console"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_CLASS = "osgi.consoleClass"; //$NON-NLS-1$
	public static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration"; //$NON-NLS-1$
	public static final String PROP_OS = "osgi.os"; //$NON-NLS-1$
	public static final String PROP_WS = "osgi.ws"; //$NON-NLS-1$
	public static final String PROP_NL = "osgi.nl"; //$NON-NLS-1$
	public static final String PROP_NL_EXTENSIONS = "osgi.nl.extensions"; //$NON-NLS-1$
	public static final String PROP_ARCH = "osgi.arch"; //$NON-NLS-1$
	public static final String PROP_ADAPTOR = "osgi.adaptor"; //$NON-NLS-1$
	public static final String PROP_SYSPATH = "osgi.syspath"; //$NON-NLS-1$
	public static final String PROP_LOGFILE = "osgi.logfile"; //$NON-NLS-1$
	public static final String PROP_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$
	public static final String PROP_INSTALL_AREA = "osgi.install.area"; //$NON-NLS-1$
	public static final String PROP_CONFIG_AREA = "osgi.configuration.area"; //$NON-NLS-1$
	public static final String PROP_CONFIG_AREA_DEFAULT = "osgi.configuration.area.default"; //$NON-NLS-1$
	public static final String PROP_FRAMEWORK_SHAPE = "osgi.framework.shape"; //$NON-NLS-1$ //the shape of the fwk (jar, or folder)
	public static final String PROP_NOSHUTDOWN = "osgi.noShutdown"; //$NON-NLS-1$
	
	public static final String PROP_EXITCODE = "eclipse.exitcode"; //$NON-NLS-1$
	public static final String PROP_EXITDATA = "eclipse.exitdata"; //$NON-NLS-1$
	public static final String PROP_CONSOLE_LOG = "eclipse.consoleLog"; //$NON-NLS-1$
	public static final String PROP_IGNOREAPP = "eclipse.ignoreApp"; //$NON-NLS-1$
	public static final String PROP_REFRESH_BUNDLES = "eclipse.refreshBundles"; //$NON-NLS-1$
	
	public static final String PROP_LAUNCHER = "eclipse.launcher"; //$NON-NLS-1$
	public static final String PROP_LAUNCHER_NAME = "eclipse.launcher.name"; //$NON-NLS-1$
	public static final String PROP_SPLASHPATH = "osgi.splashPath"; //$NON-NLS-1$
	public static final String PROP_OSGI_SPLASHLOCATION = "-osgi.splashLocation"; //$NON-NLS-1$
	public static final String PROP_SPLASHLOCATION = "splash.location"; //$NON-NLS-1$
	public static final String PROP_SPLASH = "splash"; //$NON-NLS-1$
	public static final String PROP_ECLIPSE_IGNORE_APP = "eclipse.ignoreApp";//$NON-NLS-1$
	public static final String PROP_ECLIPSE_INITIALIZE = "eclipse.initialize";//$NON-NLS-1$
	public static final String PROP_LAUNCHER_ARGUMENTS = "launcher.arguments";//$NON-NLS-1$
	public static final String PROP_LAUNCH_STORAGE_DIR = "launch.storage.dir";//$NON-NLS-1$
	public static final String PROP_LAUNCHER_LIBRARY = "launcher.library";//$NON-NLS-1$
	
	public static final String PROP_USER_HOME = "user.home"; //$NON-NLS-1$
	public static final String PROP_USER_DIR = "user.dir"; //$NON-NLS-1$
	
	public static final String SPLASH_HANDLE = "org.eclipse.equinox.launcher.splash.handle"; //$NON-NLS-1$
	public static final String SPLASH_LOCATION = "org.eclipse.equinox.launcher.splash.location"; //$NON-NLS-1$

	//	URLs
	public static final String PLATFORM_URL = "platform:/base/"; //$NON-NLS-1$
	public static final String FILE_SCHEME = "file:"; //$NON-NLS-1$    
	
	public static final String REFERENCE = "reference:";
	// Data mode constants for user, configuration and data locations.
	public static final String NONE = "@none"; //$NON-NLS-1$
	public static final String NO_DEFAULT = "@noDefault"; //$NON-NLS-1$
	public static final String USER_HOME = "@user.home"; //$NON-NLS-1$
	public static final String USER_DIR = "@user.dir"; //$NON-NLS-1$
	// Placeholder for hashcode of installation directory
	public static final String INSTALL_HASH_PLACEHOLDER = "@install.hash"; //$NON-NLS-1$
	
	public static final String PROP_RUNPATH = "-runpath";
	public static final String LAUNCH_TRACE = "launch.trace";

}
