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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflective access to the splash related methods of
 * {@code org.eclipse.equinox.launcher.JNIBridge}.
 * <p>
 * Since Equinox launcher 1.8 the {@code JNIBridge} class is package private.
 * Its native methods are bound to that exact class name, so it can neither be
 * copied nor subclassed. The class is embedded in the pre launcher and lives
 * on the launcher class path (unnamed module), which allows reflective access.
 */
public class SplashBridge {

	private static final String JNI_BRIDGE = "org.eclipse.equinox.launcher.JNIBridge";

	private final Object bridge;
	private final Method showSplash;
	private final Method updateSplash;
	private final Method takeDownSplash;

	/**
	 * Creates a new bridge for the given native launcher library.
	 * @param launcherLibrary the path of the native launcher library
	 * @throws ReflectiveOperationException if the JNIBridge is not available
	 */
	public SplashBridge(String launcherLibrary) throws ReflectiveOperationException {
		Class<?> bridgeClass = Class.forName(JNI_BRIDGE, true, SplashBridge.class.getClassLoader());
		Constructor<?> constructor = bridgeClass.getDeclaredConstructor(String.class);
		constructor.setAccessible(true);
		bridge = constructor.newInstance(launcherLibrary);
		showSplash = accessible(bridgeClass, "showSplash", String.class);
		updateSplash = accessible(bridgeClass, "updateSplash");
		takeDownSplash = accessible(bridgeClass, "takeDownSplash");
	}

	private static Method accessible(Class<?> type, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
		Method method = type.getDeclaredMethod(name, parameterTypes);
		method.setAccessible(true);
		return method;
	}

	public boolean showSplash(String splashLocation) {
		return invoke(showSplash, splashLocation);
	}

	public boolean updateSplash() {
		return invoke(updateSplash);
	}

	public boolean takeDownSplash() {
		return invoke(takeDownSplash);
	}

	private boolean invoke(Method method, Object... args) {
		try {
			return (Boolean) method.invoke(bridge, args);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			throw new IllegalStateException(cause);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
