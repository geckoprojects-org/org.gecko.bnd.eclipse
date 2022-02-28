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
package org.gecko.bnd.eclipse.launcher.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.gecko.bnd.eclipse.launcher.pre.EclipseLauncherConstants;
import static org.gecko.eclipse.api.BndEclipseConstants.*; 

/**
 * @author Juergen Albert
 * @since 12 Aug 2019
 * 
 */
public class CommonUtil {

	/**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
	public static String[] getArrayFromList(String prop) {
		if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		ArrayList<String> list = new ArrayList<>();
		StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.isEmpty()) {
				list.add(token);
			}
		}
		return list.isEmpty() ? new String[0] : list.toArray(new String[list.size()]);
	}

	/*
	 * resolve platform:/base/ URLs
	 */
	public static String resolve(String urlString, EclipseLauncherConstants props) {
		// handle the case where people mistakenly spec a refererence: url.
		if (urlString.startsWith(REFERENCE))
			urlString = urlString.substring(10);
		if (urlString.startsWith(PLATFORM_URL)) {
			String path = urlString.substring(PLATFORM_URL.length());
			return props.installationLocation.toString() + path;
		}
		return urlString;
	}

	public static URL buildURL(String spec, boolean trailingSlash) {
		if (spec == null)
			return null;
		if (File.separatorChar == '\\')
			spec = spec.trim();
		boolean isFile = spec.startsWith(FILE_SCHEME);
		try {
			if (isFile) {
				File toAdjust = new File(spec.substring(5));
				toAdjust = resolveFile(toAdjust);
				if (toAdjust.isDirectory())
					return adjustTrailingSlash(toAdjust.toURI().toURL(), trailingSlash);
				return toAdjust.toURI().toURL();
			}
			return new URL(spec);
		} catch (MalformedURLException e) {
			// if we failed and it is a file spec, there is nothing more we can do
			// otherwise, try to make the spec into a file URL.
			if (isFile)
				return null;
			try {
				File toAdjust = new File(spec);
				if (toAdjust.isDirectory())
					return adjustTrailingSlash(toAdjust.toURI().toURL(), trailingSlash);
				return toAdjust.toURI().toURL();
			} catch (MalformedURLException e1) {
				return null;
			}
		}
	}

	/**
	 * Searches for the given target directory starting in the "plugins" subdirectory
	 * of the given location.  If one is found then this location is returned; 
	 * otherwise an exception is thrown.
	 * 
	 * @return the location where target directory was found
	 * @param start the location to begin searching
	 */
	public static String searchFor(final String target, String start) {
		return searchFor(target, null, start);
	}

	public static String searchFor(final String target, final String targetSuffix, String start) {
		File root = new File(start);

		// Note that File.list only gives you file names not the complete path from start
		String[] candidates = root.list();
		if (candidates == null)
			return null;

		ArrayList<String> matches = new ArrayList<>(2);
		for (int i = 0; i < candidates.length; i++) {
			if (isMatchingCandidate(target, candidates[i], root))
				matches.add(candidates[i]);
		}
		String[] names = matches.toArray(new String[matches.size()]);
		int result = findMax(target, names);
		if (result == -1)
			return null;
		File candidate = new File(start, names[result]);
		return candidate.getAbsolutePath().replace(File.separatorChar, '/') + (candidate.isDirectory() ? "/" : ""); //$NON-NLS-1$//$NON-NLS-2$
	}

	public static void log(Class<?> clazz, String message, Object... args) {
		System.out.println("DEBUG [" + clazz.getSimpleName() + "] " + String.format(message, args));
	}

	public static void error(Class<?> clazz, String message, Object... args) {
		System.err.println("ERROR [" + clazz.getSimpleName() + "] " + String.format(message, args));
	}

	/*
	 * Look for the specified spash file in the given JAR and extract it to the config 
	 * area for caching purposes.
	 */
	
	
	/*
	 * Return a boolean value indicating whether or not the given
	 * path represents a JAR file.
	 */
	public static boolean isJAR(String path) {
		return new File(path).isFile();
	}

	/**
	 * Returns a string representation of the given URL String.  This converts
	 * escaped sequences (%..) in the URL into the appropriate characters.
	 * NOTE: due to class visibility there is a copy of this method
	 *       in InternalBootLoader
	 */
	public static String decode(String urlString) {
		try {
			//first encode '+' characters, because URLDecoder incorrectly converts 
			//them to spaces on certain class library implementations.
			if (urlString.indexOf('+') >= 0) {
				int len = urlString.length();
				StringBuilder buf = new StringBuilder(len);
				for (int i = 0; i < len; i++) {
					char c = urlString.charAt(i);
					if (c == '+')
						buf.append("%2B"); //$NON-NLS-1$
					else
						buf.append(c);
				}
				urlString = buf.toString();
			}
			return URLDecoder.decode(urlString, "UTF-8"); //$NON-NLS-1$
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	protected static int findMax(String prefix, String[] candidates) {
		int result = -1;
		Object maxVersion = null;
		for (int i = 0; i < candidates.length; i++) {
			String name = (candidates[i] != null) ? candidates[i] : ""; //$NON-NLS-1$
			String version = ""; //$NON-NLS-1$ // Note: directory with version suffix is always > than directory without version suffix
			if (prefix == null)
				version = name; //webstart just passes in versions
			else if (name.startsWith(prefix + "_")) //$NON-NLS-1$
				version = name.substring(prefix.length() + 1); //prefix_version
			Object currentVersion = getVersionElements(version);
			if (maxVersion == null) {
				result = i;
				maxVersion = currentVersion;
			} else {
				if (compareVersion((Object[]) maxVersion, (Object[]) currentVersion) < 0) {
					result = i;
					maxVersion = currentVersion;
				}
			}
		}
		return result;
	}

	/**
	 * Resolve the given file against  osgi.install.area.
	 * If osgi.install.area is not set, or the file is not relative, then
	 * the file is returned as is. 
	 */
	private static File resolveFile(File toAdjust) {
		if (!toAdjust.isAbsolute()) {
			String installArea = System.getProperty(PROP_INSTALL_AREA);
			if (installArea != null) {
				if (installArea.startsWith(FILE_SCHEME))
					toAdjust = new File(installArea.substring(5), toAdjust.getPath());
				else if (new File(installArea).exists())
					toAdjust = new File(installArea, toAdjust.getPath());
			}
		}
		return toAdjust;
	}

	private static URL adjustTrailingSlash(URL url, boolean trailingSlash) throws MalformedURLException {
		String file = url.getFile();
		if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
			return url;
		file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
		return new URL(url.getProtocol(), url.getHost(), file);
	}

	/**
	 * Compares version strings. 
	 * @return result of comparison, as integer;
	 * <code><0</code> if left < right;
	 * <code>0</code> if left == right;
	 * <code>>0</code> if left > right;
	 */
	private static int compareVersion(Object[] left, Object[] right) {

		int result = ((Integer) left[0]).compareTo((Integer) right[0]); // compare major
		if (result != 0)
			return result;

		result = ((Integer) left[1]).compareTo((Integer) right[1]); // compare minor
		if (result != 0)
			return result;

		result = ((Integer) left[2]).compareTo((Integer) right[2]); // compare service
		if (result != 0)
			return result;

		return ((String) left[3]).compareTo((String) right[3]); // compare qualifier
	}

	/**
	 * Do a quick parse of version identifier so its elements can be correctly compared.
	 * If we are unable to parse the full version, remaining elements are initialized
	 * with suitable defaults.
	 * @return an array of size 4; first three elements are of type Integer (representing
	 * major, minor and service) and the fourth element is of type String (representing
	 * qualifier). Note, that returning anything else will cause exceptions in the caller.
	 */
	private static Object[] getVersionElements(String version) {
		if (version.endsWith(".jar")) //$NON-NLS-1$
			version = version.substring(0, version.length() - 4);
		Object[] result = {Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0), ""}; //$NON-NLS-1$
		StringTokenizer t = new StringTokenizer(version, "."); //$NON-NLS-1$
		String token;
		int i = 0;
		while (t.hasMoreTokens() && i < 4) {
			token = t.nextToken();
			if (i < 3) {
				// major, minor or service ... numeric values
				try {
					result[i++] = Integer.valueOf(token);
				} catch (Exception e) {
					// invalid number format - use default numbers (0) for the rest
					break;
				}
			} else {
				// qualifier ... string value
				result[i++] = token;
			}
		}
		return result;
	}

	private static boolean isMatchingCandidate(String target, String candidate, File root) {
		if (candidate.equals(target))
			return true;
		if (!candidate.startsWith(target + "_")) //$NON-NLS-1$
			return false;
		int targetLength = target.length();
		int lastUnderscore = candidate.lastIndexOf('_');

		//do we have a second '_', version (foo_1.0.0.v1_123) or id (foo.x86_64) ?
		//files are assumed to have an extension (zip or jar only), remove it
		//NOTE: we only remove .zip and .jar extensions because we still need to accept libraries with
		//simple versions (e.g. eclipse_1234.dll)
		File candidateFile = new File(root, candidate);
		if (candidateFile.isFile() && (candidate.endsWith(".jar") || candidate.endsWith(".zip"))) { //$NON-NLS-1$//$NON-NLS-2$
			int extension = candidate.lastIndexOf('.');
			candidate = candidate.substring(0, extension);
		}

		int lastDot = candidate.lastIndexOf('.');
		if (lastDot < targetLength) {
			// no dots after target, the '_' is not in a version (foo.x86_64 case), not a match
			return false;
		}

		//get past all '_' that are part of the qualifier
		while (lastUnderscore > lastDot)
			lastUnderscore = candidate.lastIndexOf('_', lastUnderscore - 1);

		if (lastUnderscore == targetLength)
			return true; //underscore at the end of target (foo_1.0.0.v1_123 case)
		return false; //another underscore between target and version (foo_64_1.0.0.v1_123 case)
	}

	/*
	 * Look for the specified spash file in the given JAR and extract it to the config 
	 * area for caching purposes.
	 */
}
