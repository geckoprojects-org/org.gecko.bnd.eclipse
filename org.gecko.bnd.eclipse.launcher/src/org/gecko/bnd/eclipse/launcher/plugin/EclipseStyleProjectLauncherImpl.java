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

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.eclipse.pde.internal.swt.tools.IconExe;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectLauncher;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.instructions.BuilderInstructions;
import aQute.bnd.help.instructions.LauncherInstructions;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.EmbeddedResource;
import aQute.bnd.osgi.FileResource;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Jar.Compression;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.osgi.WriteResource;
import aQute.bnd.osgi.repository.AggregateRepository;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.Strategy;
import aQute.launcher.constants.LauncherConstants;
import aQute.lib.collections.MultiMap;
import aQute.lib.io.ByteBufferDataInput;
import aQute.lib.io.ByteBufferOutputStream;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.glob.Glob;
import biz.aQute.resolve.WorkspaceResourcesRepository;

import static org.gecko.eclipse.api.BndEclipseConstants.*; 

/**
 * 
 * @author Juergen Albert
 * @since 17 Sep 2019
 */
@SuppressWarnings("restriction")
public class EclipseStyleProjectLauncherImpl extends ProjectLauncher {

	private static final String 	PRE_LAUNCHER = "-preLauncher";
	private static final String 	LAUNCHERICONS = "-launchericons";

	private final static Logger		logger = LoggerFactory.getLogger(EclipseStyleProjectLauncherImpl.class);
	private static final String 	PRELAUNCHER_JAR = "org.gecko.bnd.eclipse.launcher.pre.jar";
	private BuilderInstructions	 	builderInstrs;
	private LauncherInstructions	launcherInstrs;

	final private File				launchPropertiesFile;
	private File					nativeLibraryFile = null;
	boolean							prepared;

	DatagramSocket					listenerComms;
	private List<String>			modifiedRunProgramArgs = null;
	private Map<String, String> 	modifiedRunProperties = null;
	private File 					embeddedLauncherJar;
	private LinkedList<String> 		modifiedRunpath;
	private File 					installArea;

	public EclipseStyleProjectLauncherImpl(Project project) throws Exception {
		super(project);

		builderInstrs = project.getInstructions(BuilderInstructions.class);
		launcherInstrs = project.getInstructions(LauncherInstructions.class);

		logger.debug("created a aQute launcher plugin");
		launchPropertiesFile = File.createTempFile("launch", ".properties", project.getTarget());
		logger.debug("launcher plugin using temp launch file {}", launchPropertiesFile.getAbsolutePath());
		addRunVM("-D" + LauncherConstants.LAUNCHER_PROPERTIES + "=\"" + launchPropertiesFile.getAbsolutePath() + "\"");

		if (project.getRunProperties()
				.get("noframework") != null) {
			setRunFramework(NONE);
			project.warning(
					"The noframework property in -runproperties is replaced by a project setting: '-runframework: none'");
		}

		super.addDefault(Constants.DEFAULT_LAUNCHER_BSN);
		try {
			if (project != null) {
				if (!project.isStandalone() && project.getWorkspace() != null) {
					project.getWorkspace()
					.addBasicPlugin(new WorkspaceResourcesRepository(project.getWorkspace()));
					project.getWorkspace().addBasicPlugin(project.getWorkspace().getWorkspaceRepository());
					project.getWorkspace().propertiesChanged();
				}
			}
		} catch (Throwable e) {
			getErrors().add("Something went wron during initialization of the EclipseSytleProjectLauncherImpl " + e.getClass().getName() + " " + e.getMessage() + " " + e.getStackTrace()[0].toString());
		}

		doChecks();
	}



	/**
	 * Looks if the Project is valid, of if there are some things missing, we would need.
	 */
	private void doChecks() {
		Optional<String> showSplash = getProject().getRunProgramArgs().stream().filter(arg -> arg.contains("-showsplash") || arg.contains("-osgi.splashLocation")).findFirst();
		showSplash.ifPresent((s) -> {
			if(getProject().getRunSystemCapabilities().isEmpty()) {
				getProject().getErrors().add("No -runsystemcapabilities found. This is required to find the native lib for the splash screen");
			}
			try {
				getEclipseLibrary(this.getProject(), "lib");
			} catch (Exception e) {
				getProject().getErrors().add("Something went wrong while looking for the ");            
			}
		});

		if(getProject().getRunSystemCapabilities().isEmpty()) {
			getProject().getWarnings().add("No -runsystemcapabilities found. This will cause an error when the project is exported. Use ${native_capability} to set you system capabilties");
		}


	}

	//
	// Initialize the main class for a local launch start
	//
	@Override
	protected int invoke(Class<?> main, String args[]) throws Exception {
		LauncherConstants lc = getConstants(getRunBundles(), false);

		Method mainMethod = main.getMethod("main", args.getClass(), Properties.class);
		Object o = mainMethod.invoke(null, args, lc.getProperties(new UTF8Properties()));
		if (o == null)
			return 0;

		return (Integer) o;
	}

	/**
	 * Cleanup the properties file. Is called after the process terminates.
	 */

	@Override
	public void cleanup() {
		if(launchPropertiesFile != null) {
			IO.delete(launchPropertiesFile);
		}
		if (listenerComms != null) {
			listenerComms.close();
			listenerComms = null;
		}
		if(nativeLibraryFile != null) {
			IO.delete(nativeLibraryFile);
			logger.debug("Deleted {}", nativeLibraryFile.getAbsolutePath());
			nativeLibraryFile = null;
		}
		if(embeddedLauncherJar != null) {
			getClasspath().remove(embeddedLauncherJar.getAbsolutePath());
			IO.delete(embeddedLauncherJar);
			logger.debug("Deleted {}", embeddedLauncherJar.getAbsolutePath());
			embeddedLauncherJar = null;
		}
		if(installArea != null) {
			IO.delete(installArea);
			installArea = null;
		}
		modifiedRunProgramArgs = null;
		modifiedRunpath = null;
		prepared = false;
		logger.debug("Deleted {}", launchPropertiesFile.getAbsolutePath());
		super.cleanup();
	}

	@Override
	public String getMainTypeName() {
		return "org.gecko.bnd.eclipse.launcher.pre.EclipseStyleEmbeddedLauncher";
	}

	@Override
	public void update() throws Exception {
		updateFromProject();
		writeProperties();
	}

	@Override
	public void prepare() throws Exception {
		if (prepared)
			return;
		modifiedRunProgramArgs = new LinkedList<String>(super.getRunProgramArgs());
		modifiedRunpath = new LinkedList<String>(super.getRunpath());
		modifiedRunProperties = new HashMap<String, String>(super.getRunProperties());
		prepared = true;
		extractLibIfAvailable();
		preparePrelauncher();
		writeProperties();
		installArea = Files.createTempDirectory(getProject().getTarget().toPath(), "installArea").toFile();
		installArea.mkdirs();
		getRunVM().add("-Dosgi.install.area=" + installArea.getAbsolutePath());
	}

	@Override
	public List<String> getRunpath() {
		if(!prepared) {
			return super.getRunpath();
		}
		return modifiedRunpath;
	}

	@Override
	public Map<String, String> getRunProperties() {
		if(modifiedRunProperties == null) {
			return super.getRunProperties();
		} 
		return modifiedRunProperties;
	}

	/**
	 * extracts the prelauncehr jar 
	 */
	private void preparePrelauncher() {
		try {
			embeddedLauncherJar = File.createTempFile("org.gecko.bnd.eclipse.launcher.pre", ".jar", this.getProject().getTarget());
			InputStream in = getClass().getResourceAsStream("/org.gecko.bnd.eclipse.launcher.pre.jar");
			IO.copy(in, embeddedLauncherJar);
			modifiedRunpath.add(embeddedLauncherJar.getAbsolutePath());
			getClasspath().add(embeddedLauncherJar.getAbsolutePath());
		} catch (Exception e) {
			getProject().getErrors().add("Could not extract " + PRELAUNCHER_JAR + ". Cause: " + e.getMessage());
		}

	}

	@Override
	public Collection<String> getRunProgramArgs() {
		if(!prepared) {
			return super.getRunProgramArgs();
		} else {
			return modifiedRunProgramArgs ;
		}
	}

	/**
	 * 
	 */
	private void extractLibIfAvailable() {
		try {
			Entry<String, Resource> eclipseLibrary = getEclipseLibrary(this.getProject(), "lib");
			if(eclipseLibrary == null) {
				getProject().warning("No equinox native library found, your RCP will not be able to show a Splashscreen.");
				return;
			}

			String[] fileName = eclipseLibrary.getKey().split("\\.");
			nativeLibraryFile = File.createTempFile(fileName[0], "." + fileName[1], getProject().getTarget());
			try(OutputStream outputStream = IO.outputStream(nativeLibraryFile)){
				eclipseLibrary.getValue().write(outputStream);
			};
			if(modifiedRunProperties != null) {
				modifiedRunProperties.put("launcher.library" , nativeLibraryFile.getAbsolutePath());
			}
		} catch (Exception e) {
			getProject().error("No equinox native library found - ", e);
		}

	}

	void writeProperties() throws Exception {
		LauncherConstants lc = getConstants(getRunBundles(), false);
		try (OutputStream out = IO.outputStream(launchPropertiesFile)) {
			lc.getProperties(new UTF8Properties())
			.store(out, "Launching " + getProject());
		}
	}

	/**
	 * @throws Exception
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private LauncherConstants getConstants(Collection<String> runbundles, boolean exported)
			throws Exception, FileNotFoundException, IOException {
		logger.debug("preparing the aQute launcher plugin");

		LauncherConstants lc = new LauncherConstants();
		lc.noreferences = getProject().is(Constants.RUNNOREFERENCES);
		lc.runProperties = getRunProperties();
		lc.storageDir = getStorageDir();
		lc.keep = isKeep();
		lc.runbundles.addAll(runbundles);
		lc.trace = getTrace();
		lc.timeout = getTimeout();
		lc.services = super.getRunFramework() == SERVICES ? true : false;
		lc.activators.addAll(getActivators());
		lc.name = getProject().getName();

		if (!exported && !getNotificationListeners().isEmpty()) {
			if (listenerComms == null) {
				listenerComms = new DatagramSocket(new InetSocketAddress(InetAddress.getByName(null), 0));
				new Thread(new Runnable() {
					@Override
					public void run() {
						DatagramSocket socket = listenerComms;
						DatagramPacket packet = new DatagramPacket(new byte[65536], 65536);
						while (!socket.isClosed()) {
							try {
								socket.receive(packet);
								DataInput dai = ByteBufferDataInput.wrap(packet.getData(), packet.getOffset(),
										packet.getLength());
								NotificationType type = NotificationType.values()[dai.readInt()];
								String message = dai.readUTF();
								for (NotificationListener listener : getNotificationListeners()) {
									listener.notify(type, message);
								}
							} catch (IOException e) {}
						}
					}
				}).start();
			}
			lc.notificationPort = listenerComms.getLocalPort();
		} else {
			lc.notificationPort = -1;
		}

		try {
			// If the workspace contains a newer version of biz.aQute.launcher
			// than the version of bnd(tools) used
			// then this could throw NoSuchMethodError. For now just ignore it.
			Map<String, ? extends Map<String, String>> systemPkgs = getSystemPackages();
			if (systemPkgs != null && !systemPkgs.isEmpty())
				lc.systemPackages = Processor.printClauses(systemPkgs);
		} catch (Throwable e) {}

		try {
			// If the workspace contains a newer version of biz.aQute.launcher
			// than the version of bnd(tools) used
			// then this could throw NoSuchMethodError. For now just ignore it.
			String systemCaps = getSystemCapabilities();
			if (systemCaps != null) {
				systemCaps = systemCaps.trim();
				if (systemCaps.length() > 0)
					lc.systemCapabilities = systemCaps;
			}
		} catch (Throwable e) {}
		return lc;

	}

	private static final String BUNDLE_AREA = "plugins";
	private static final String CONFIGURATION_AREA = "configuration";

	/**
	 * Creates a Product build
	 */

	@Override
	public Jar executable() throws Exception {
		try {

			Optional<Compression> rejar = launcherInstrs.executable()
					.rejar();
			logger.debug("rejar {}", rejar);
			Map<Glob, List<Glob>> strip = extractStripMapping(launcherInstrs.executable()
					.strip());
			logger.debug("strip {}", strip);

			String name = getProject().getName()
					.replace(".bndrun", "");

			Jar jar = new Jar(name);
			jar.setDoNotTouchManifest();
			builderInstrs.compression()
			.ifPresent(jar::setCompression);

			Parameters ir = getProject().getIncludeResource();
			if (!ir.isEmpty()) {
				try (Builder b = new Builder()) {
					b.setIncludeResource(ir.toString());
					b.setProperty(Constants.RESOURCEONLY, "true");
					b.build();
					if (b.isOk()) {
						Jar resources = b.getJar();
						jar.addAll(resources);
						// make sure copied resources are not closed
						// when Builder and its Jar are closed
						resources.getResources()
						.clear();
					}
					getProject().getInfo(b);
				}
			}

			List<String> runpath = getRunpath();

			List<String> classpath = new ArrayList<>();

			for (String path : runpath) {
				logger.debug("embedding runpath {}", path);
				File file = new File(path);
				if (file.isFile()) {
					String newPath = nonCollidingPath(file, jar);
					jar.putResource(newPath, getJarFileResource(file, rejar, strip));
					classpath.add(newPath);
				}
			}

			// Copy the bundles to the JAR

			List<String> runbundles = (List<String>) getRunBundles();
			List<String> actualPaths = new ArrayList<>();

			for (String path : runbundles) {
				logger.debug("embedding run bundles {}", path);
				File file = new File(path);
				if (!file.isFile())
					getProject().error("Invalid entry in -runbundles %s", file);
				else {
					String newPath = nonCollidingPath(file, jar);
					jar.putResource(newPath, getJarFileResource(file, rejar, strip));
					actualPaths.add("${launcher.installLocation}/" + newPath);
				}
			}


			//Add the pre launcher jar
			doStart(jar, name, BUNDLE_AREA + "/org.gecko.bnd.eclipse.launcher.pre.jar", classpath);

			LauncherConstants lc = getConstants(actualPaths, true);
			lc.embedded = false;
			lc.runProperties.putIfAbsent("osgi.install.area", "../");
			try (ByteBufferOutputStream bout = new ByteBufferOutputStream()) {
				lc.getProperties(new UTF8Properties())
				.store(bout, "");
				jar.putResource(CONFIGURATION_AREA + "/" + LauncherConstants.DEFAULT_LAUNCHER_PROPERTIES,
						new EmbeddedResource(bout.toByteBuffer(), 0L));
			}

			String preLauncherJar = getProject().getProperty(PRE_LAUNCHER);
			Resource preLauncher = null;
			if(preLauncherJar != null) {
				preLauncher = Resource.fromURL(new File(preLauncherJar).toURI().toURL());
			} else {
				preLauncher = Resource.fromURL(this.getClass()
						.getResource("/org.gecko.bnd.eclipse.launcher.pre.jar"));
			}
			jar.putResource(BUNDLE_AREA + "/org.gecko.bnd.eclipse.launcher.pre.jar", preLauncher);

			Properties flattenedProperties = getProject().getFlattenedProperties();
			Manifest m = new Manifest();
			Attributes main = m.getMainAttributes();
			for (Entry<Object, Object> e : flattenedProperties.entrySet()) {
				String key = (String) e.getKey();
				if (key.length() > 0 && Character.isUpperCase(key.charAt(0)))
					main.putValue(key, (String) e.getValue());
			}
			jar.setManifest(m);

			cleanup();
			return jar;
		} catch(Throwable t) {
			getLogger().error("Error while creating executable " + t.getMessage() + "\n" + createStacktrace(t));
			getProject().getErrors().add("Error while creating executable " + t.getMessage() + "\n" + createStacktrace(t));
			return null;
		}
	}

	private String createStacktrace(Throwable t) throws IOException {
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
			PrintWriter writer = new PrintWriter(baos);
			t.printStackTrace(writer);
			writer.flush();
			return baos.toString();
		} 
	}


	/*
	 * Useful for when exported as folder or unzipped
	 */
	void doStart(Jar jar, String name, String preLauncherPath, List<String> runPath) throws Exception {
		Parameters runSystemCapabilities = getProject().getRunSystemCapabilities();
		Attrs nativeAttr = runSystemCapabilities.get(NativeNamespace.NATIVE_NAMESPACE);

		String osString = nativeAttr.get(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE);
		String archString = nativeAttr.get(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE);

		Map<String, Resource> executable = getEclipseExecutable(getProject(), name,  archString.split(","), osString.split(","), false);
		Entry<String, Resource> lib = getEclipseLibrary(getProject(), name,  archString.split(","), osString.split(","));

		executable.forEach((k,v) -> jar.putResource(k, v));
		jar.putResource(lib.getKey(), lib.getValue());

		if(getProject().isRunTrace()) {
			Map<String, Resource> debugExecutable = getEclipseExecutable(getProject(), name,  archString.split(","), osString.split(","), true);
			debugExecutable.forEach((k,v) -> jar.putResource(k, v));
		}

		ByteArrayOutputStream baos = createLauncherProperties(getProject(), getProject().getName(), preLauncherPath, lib.getKey(), runPath);

		jar.putResource(name + ".ini", new WriteResource() {

			@Override
			public void write(OutputStream out) throws Exception {
				out.write(baos.toByteArray());
			}

			@Override
			public long lastModified() {
				return 0;
			}
		});

	}

	private Entry<String, Resource> getEclipseLibrary(final Project project, String name)
			throws Exception {
		Parameters runSystemCapabilities = getProject().getRunSystemCapabilities();
		Attrs nativeAttr = runSystemCapabilities.get(NativeNamespace.NATIVE_NAMESPACE);

		if(nativeAttr == null) {
			project.warning("No " + NativeNamespace.NATIVE_NAMESPACE + " Capability found in " + Constants.RUNSYSTEMCAPABILITIES + " instructions: %s", runSystemCapabilities);
			return null;
		}

		String osString = nativeAttr.get(NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE);
		String archString = nativeAttr.get(NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE);

		return getEclipseLibrary(getProject(), name,  archString.split(","), osString.split(","));
	}

	private ByteArrayOutputStream createLauncherProperties(Project project, String name, String preLauncherPath, String libLocation, List<String> runPath) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(baos);
		writer.println(STARTUP);
		writer.println(preLauncherPath);
		writer.println(LIBRARY);
		writer.println(libLocation);
		Collection<String> programArgs = project.getRunProgramArgs();
		boolean cleanSet = false;
		for(String arg : programArgs) {
			StringTokenizer tokenizer = new StringTokenizer(arg, "\n");
			while(tokenizer.hasMoreTokens()){
				String o = tokenizer.nextToken();
				if(CLEAN.equals(o)) {
					cleanSet = true;
				}
				System.out.println("Adding Program Arg " + o);
				writer.println(o);
			}
		}
		if(!cleanSet && !isKeep()) {
			writer.println(CLEAN);
		}
		writer.println("-runpath");
		StringJoiner joiner = new StringJoiner(",");
		runPath.forEach(joiner::add);
		writer.println(joiner.toString());
		writer.println("-vmargs");
		if(project.isRunTrace()) {
			writer.println("-Dlaunch.trace=true");
		}
		Collection<String> vmArgs = project.getRunVM();
		for(String arg : vmArgs) {
			StringTokenizer tokenizer = new StringTokenizer(arg, "\n");
			while(tokenizer.hasMoreTokens()){
				String o = tokenizer.nextToken();
				System.out.println("Adding VM Arg " + o);
				writer.println(o);
			}
		}

		writer.flush();
		return baos;
	}

	private Map<Glob, List<Glob>> extractStripMapping(List<String> strip) {
		MultiMap<Glob, Glob> map = new MultiMap<>();

		for (String s : strip) {
			int n = s.indexOf(':');
			Glob key = Glob.ALL;
			if (n > 0) {
				key = new Glob(s.substring(0, n));
			}
			Glob value = new Glob(s.substring(n + 1));
			map.add(key, value);
		}
		return map;
	}

	private Resource getJarFileResource(File file, Optional<Compression> compression, Map<Glob, List<Glob>> strip)
			throws IOException {
		if (strip.isEmpty() && !compression.isPresent()) {
			return new FileResource(file);
		}

		Jar jar = new Jar(file);
		jar.setDoNotTouchManifest();

		compression.ifPresent(jar::setCompression);
		logger.debug("compression {}", compression);

		stripContent(strip, jar);

		JarResource resource = new JarResource(jar, true);
		return resource;
	}

	private void stripContent(Map<Glob, List<Glob>> strip, Jar jar) {
		Set<String> remove = new HashSet<>();

		for (Map.Entry<Glob, List<Glob>> e : strip.entrySet()) {
			Glob fileMatch = e.getKey();
			if (!fileMatch.matcher(jar.getName())
					.matches()) {
				continue;
			}

			logger.debug("strip {}", e.getValue());
			List<Glob> value = e.getValue();

			for (String path : jar.getResources()
					.keySet()) {
				if (Glob.in(value, path)) {
					logger.debug("strip {}", path);
					remove.add(path);
				}
			}
		}
		remove.forEach(jar::remove);
		logger.debug("resources {}", Strings.join("\n", jar.getResources()
				.keySet()));
	}

	String nonCollidingPath(File file, Jar jar) {
		String fileName = file.getName();
		String path = BUNDLE_AREA + "/" + fileName;
		String[] parts = Strings.extension(fileName);
		if (parts == null) {
			parts = new String[] {
					fileName, ""
			};
		}
		int i = 1;
		while (jar.exists(path)) {
			path = String.format("bundles/%s[%d].%s", parts[0], i++, parts[1]);
		}
		return path;
	}


	private static final String LAUNCHER_FILTER_TEMPLATE = "(&(|%s)(|%s)(type=%s))";

	private static final String LAUNCHER_FILTER_OS = "(" + NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE + "=%s)";
	private static final String LAUNCHER_FILTER_PROCESSOR = "(" + NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE + "=%s)";

	private Map<String, Resource> getEclipseExecutable(final Project project, String name,  String archs[], String os[], boolean debug)
			throws Exception {
		Jar eclipseBundle = getEclipseBundle(project, archs, os, "executable");
		String pathAttrib = debug ? "Executable-Path-Debug" : "Executable-Path";
		String execNameAttrib = debug ? "Executable-Name-Debug" : "Executable-Name";

		String path = eclipseBundle.getManifest().getMainAttributes().getValue(pathAttrib);
		if(path == null) {
			return Collections.emptyMap();
		}
		String execName = eclipseBundle.getManifest().getMainAttributes().getValue(execNameAttrib);
		if(execName == null) {
			project.getWarnings().add(String.format("%s set, but no %s in Manifest for bundle %s", pathAttrib, execNameAttrib, eclipseBundle.getBsn()));
			return Collections.emptyMap();
		}

		Resource resource = eclipseBundle.getResource(path);

		if(resource == null) {
			getProject().error("%s claimes to contain an executable for eclipse, but is does not have it at the given location", eclipseBundle.getBsn());
			return Collections.emptyMap();
		}
		Map<String, Resource> result = new HashMap<String, Resource>();
		Parameters icons = project.getParameters(LAUNCHERICONS);
		if(!icons.isEmpty() && isWindows(os)) {
			getLogger().debug("found Launcher Icons");
			Set<Entry<String,Attrs>> iconsSet = icons.entrySet();

			File tempFile = File.createTempFile("tempLauncher", ".exe");
			List<String> paths = new ArrayList<String>();
			paths.add(tempFile.getAbsolutePath());
			if(iconsSet.size() == 1) {
				getLogger().debug("1 icon found, checking if it is a .ico");
				String icon = iconsSet.iterator().next().getKey();
				if(!icon.toLowerCase().endsWith(".ico")) {
					getLogger().error("Can't create launcehr. If only one icon is given, it must be a .ico file containing 7 bmps for Windows.");
					project.error("Can't create launcehr. If only one icon is given, it must be a .ico file containing 7 bmps for Windows.");
					return Collections.emptyMap();
				}
				File iconFile = new File(icon);
				if(!iconFile.exists()) {
					getLogger().error("Can't create launcehr. Icon " + iconFile.getAbsolutePath() + " does not exist");
					project.error("Can't create launcehr. Icon " + iconFile.getAbsolutePath() + " does not exist");
					return Collections.emptyMap();
				}
				paths.add(iconFile.getAbsolutePath());
			} else if(iconsSet.size() == 7){
				for (Entry<String, Attrs> iconEntry : iconsSet) {
					String icon = iconEntry.getKey();
					if(icon.toLowerCase().endsWith(".bmp")) {
						project.error("Can't create launcehr. %s must be a .bmp file", icon);
						return Collections.emptyMap();
					}
					paths.add(new File(icon).getAbsolutePath());
				}
			} else {
				getLogger().error("Can't create Launcher, because the icon doesn't fit. We currently only support windows and thus the icon needs to be a .ico file that contains 7 bmp files.");
				project.error("Can't create Launcher, because the icon doesn't fit. We currently only support windows and thus the icon needs to be a .ico file that contains 7 bmp files.");
				return Collections.emptyMap();
			}
			getLogger().debug("copying launcher to temp File: " + tempFile.getAbsolutePath());
			IO.copy(resource.openInputStream(), tempFile);

			getLogger().debug("Calling IconExe with: " + paths);

			PrintStream defaultErrorStream = System.err;
			ByteArrayOutputStream errStream = new ByteArrayOutputStream();
			System.setErr(new PrintStream(defaultErrorStream));
			try {
				IconExe.main(paths.toArray(new String[0]));
			} finally {
				System.setErr(defaultErrorStream);
			}

			String error = new String(errStream.toByteArray());
			if(!error.isEmpty()) {
				getLogger().error("IconExe Error: " + error);
				project.getErrors().add("Icon replacement failed with " + error);
				return Collections.emptyMap();
			}

			result.put(path.replace(execName, name), new FileResource(tempFile));
		} else if(!icons.isEmpty() && !isWindows(os)) {
			getLogger().warn("icons are currently only supported for windows. Ignoring the current instruction.");
			project.warning("icons are currently only supported for windows. Ignoring the current instruction.");
			result.put(path.replace(execName, name), resource);
		} else {
			result.put(path.replace(execName, name), resource);
		}
		return result;
	}

	private boolean isWindows(String[] os) {
		for(String o : os) {
			if(o.toLowerCase().startsWith("win")) {
				return true;
			}
		}
		return false;
	}

	private Entry<String, Resource> getEclipseLibrary(final Project project, String name,  String archs[], String os[])
			throws Exception {
		Jar eclipseBundle = getEclipseBundle(project, archs, os, "lib");

		if(eclipseBundle == null) {
			getProject().getErrors().add("No Bundle found that provides the equinox.launcher Capabilty.");
			return  null;
		}

		//		eclipseBundle.getManifest().getMainAttributes().forEach((k,v) -> System.out.println(k + " - " +v));

		String path = eclipseBundle.getManifest().getMainAttributes().getValue("Equinox-Lib");

		if(path == null) {
			getProject().getErrors().add("Bundle " + eclipseBundle.toString() + " provides the equinox.launcher Capabilty but does not have a Equinox-Lib specified  in its Manifest");
			return null;
		}

		Resource resource = eclipseBundle.getResource(path);

		if(resource == null) {
			getProject().getErrors().add("Bundle " + eclipseBundle.toString() + " provides the equinox.launcher Capabilty but does not have a lib at " + path);
			return null;
		}

		return new Entry<String, Resource>() {

			@Override
			public String getKey() {
				return "libs/" + path;
			}

			@Override
			public Resource getValue() {
				return resource;
			}

			@Override
			public Resource setValue(Resource value) {
				return null;
			}
		};
	}

	private Jar getEclipseBundle(final Project project, String arch[], String os[], String type)
			throws IOException {
		Container container = getEclipseContainer(project, arch, os, type);
		return new Jar(container.getFile());
	}

	private Container getEclipseContainer(final Project project, String arches[], String oss[], String type)
			throws IOException {

		List<Repository> repos = project.getWorkspace()
				.getPlugins(Repository.class);
		AggregateRepository aggregateRepo = new AggregateRepository(repos);

		StringBuilder osBuilder =  new StringBuilder();
		Arrays.asList(oss).stream().map(o -> String.format(LAUNCHER_FILTER_OS, o)).forEach(osBuilder::append);

		StringBuilder archBuilder =  new StringBuilder();
		Arrays.asList(arches).stream().map(arch -> String.format(LAUNCHER_FILTER_PROCESSOR, arch)).forEach(archBuilder::append);

		String filter = String.format(LAUNCHER_FILTER_TEMPLATE, osBuilder.toString(), archBuilder.toString(), type);

		Requirement launcherCapability = aggregateRepo.newRequirementBuilder("equinox.launcher")
				.addDirective("filter", filter)
				.build();

		Collection<Capability> findProviders = aggregateRepo.findProviders(launcherCapability);

		Container container = findProviders.stream()
				.map(Capability::getResource)
				.collect(Collectors.toList())
				.stream()
				.sorted(ResourceUtils.IDENTITY_VERSION_COMPARATOR)
				.findFirst()
				.map(ResourceUtils::getIdentityCapability)
				.map(ci -> {
					try {
						return project.getBundle(ci.osgi_identity(), ci.version()
								.toString(), Strategy.HIGHEST, null);
					} catch (Exception e) {
						project.getLogger()
						.warn("Could not read bundle for capability" + ci.toString(), e);
						return null;
					}
				})
				.orElse(null);
		if(container == null) {
			project.getLogger()
			.error("Could not find bundle fitting: " + launcherCapability.toString());
			throw new RuntimeException("Could not find bundle fitting: " + launcherCapability.toString());
		}
		return container;
	}
	/*
	 * Useful for when exported as folder or unzipped
	 */
	void doStart(Jar jar, String fqn) throws UnsupportedEncodingException {
		String nix = "#!/bin/sh\njava -cp . " + fqn + "\n";
		String pc = "java -cp . " + fqn + "\r\n";
		jar.putResource("start", new EmbeddedResource(nix, 0L));
		jar.putResource("start.bat", new EmbeddedResource(pc, 0L));
	}

}
