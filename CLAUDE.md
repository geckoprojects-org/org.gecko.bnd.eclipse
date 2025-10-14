# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Gecko BND RCP Exporter/Launcher project that enables pure OSGi projects and BND to work with Eclipse Equinox and the Eclipse RCP Framework. It provides:

- A BND Plugin for product exports using the native Equinox launcher
- Splashscreen support for RCP applications
- In-framework startup of Eclipse Product/Application
- Eclipse E3/E4 RCP application templates

The project uses BND workspace model and creates exports that work differently from standard PDE product exports, using a more OSGi-standard approach.

## Build Commands

### Primary Build Commands
```bash
# Clean and build all modules
./gradlew clean build

# Build and create release artifacts
./gradlew clean build release

# Release to specific directory (used by CI)
./gradlew clean build release -Drelease.dir=<path>

# Build with Maven local repository
./gradlew build -Dmaven.repo.local=<path>
```

### Development Commands
```bash
# Run tests
./gradlew test

# Check for dependency updates
./gradlew dependencyUpdates

# Clean workspace
./gradlew clean
```

## Architecture

### Core Modules

1. **org.gecko.bnd.eclipse.launcher** - Main BND launcher that integrates with Eclipse Equinox launcher, replacing the standard BND launcher for Eclipse RCP applications

2. **org.gecko.bnd.eclipse.launcher.pre** - Pre-launcher (EclipseStyleEmbeddedLauncher) that handles the initial bootstrap and can locate bundles in external locations, translating Eclipse-specific properties

3. **org.gecko.bnd.eclipse.library** - Core library components and utilities for the Eclipse integration

4. **org.gecko.bnd.equinox.executable** - Platform-specific executable components for Linux and Windows

5. **org.gecko.bnd.equinox.launcher.splashscreen** - Splashscreen support that works with any OSGi framework

6. **org.gecko.eclipse.api** - General Eclipse API abstractions and interfaces

7. **org.gecko.eclipse.compatibility** - Compatibility layer for Eclipse/Equinox integration

8. **org.gecko.eclipse.product.template** - BND project templates for Eclipse E3 and E4 RCP applications

9. **test.product** - Example/test Eclipse E4 RCP application demonstrating the launcher capabilities

### Key Configuration Files

- **cnf/build.bnd** - Main BND workspace configuration with repositories (Eclipse 2020-06, Maven Central)
- **settings.gradle** - Gradle workspace configuration with BND plugin
- **gradle.properties** - BND version (7.1.0) and snapshot repository configuration
- **launch_base.bndrun** - Base launch configuration for RCP applications

### Launch Configuration

The project uses `.bndrun` files for application configuration:
- `-runpath` includes the Eclipse launcher plugin and splashscreen support
- `-runfw` specifies OSGi framework (Eclipse Equinox)
- `-runproperties` configure Eclipse-specific properties like product ID and application
- Support for custom launcher icons (Windows .ico format)
- Configurable splash screens and configuration areas

### Repository Structure

The project integrates with multiple repositories:
- Eclipse 2020-06 OSGi repository for Eclipse platform bundles
- Maven Central for general dependencies
- Local indexed repository for build artifacts
- Gecko-specific BOM and OBR repositories for extended functionality

## Development Notes

- Uses Java 8 (javac.source/target: 1.8) but CI builds with OpenJDK 17
- BND workspace model with OSGi bundle development
- Templates use Mustache templating engine
- Supports both Eclipse E3 and E4 RCP application development
- Native launcher library support for splash screens
- Configuration area flexibility with multiple override mechanisms