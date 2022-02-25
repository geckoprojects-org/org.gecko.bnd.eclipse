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
package org.gecko.eclipse.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.annotation.bundle.Requirement;

/**
 * Requirement annotation for Eclipse IDE dependencies. 
 * @author Mark Hoffmann
 * @since 23.09.2019
 */
@Requirement(namespace="osgi.identity", name="org.eclipse.sdk")
@Requirement(namespace="osgi.identity", name="org.eclipse.ui.ide")
@Requirement(namespace="osgi.identity", name="org.eclipse.ui.ide.application")
@Requirement(namespace="osgi.identity", name="org.apache.ant")
@Requirement(namespace="osgi.identity", name="org.eclipse.ant.ui")
@Requirement(namespace="osgi.identity", name="org.eclipse.ant.core")
@Target(value={ElementType.PACKAGE, ElementType.TYPE})
@Retention(value=RetentionPolicy.CLASS)
@Documented
public @interface RequireEclipseIDE {

}
