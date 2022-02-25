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
 * Requirement annotation that brings Eclipse E4 Dependencies 
 * @author Mark Hoffmann
 * @since 23.09.2019
 */
@Requirement(namespace="osgi.identity", name="com.google.guava")
@Requirement(namespace="osgi.identity", name="org.eclipse.equinox.event")
@Requirement(namespace="osgi.identity", name="org.eclipse.e4.ui.workbench")
@Target(value={ElementType.PACKAGE, ElementType.TYPE})
@Retention(value=RetentionPolicy.CLASS)
@Documented
public @interface RequireEclipseE4 {

}
