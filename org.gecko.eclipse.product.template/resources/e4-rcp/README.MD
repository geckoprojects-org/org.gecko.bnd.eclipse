This project is a simple Example that represents the default Eclipse E4 Application with sample content the PDE also provides.

If you want to modify it, only add -runrequire bundles in the **launch_base.bndrun** and resolve only the **launch_base.bndrun**. 

To start or export use *launch_linux* or *launch_win32* (or *launch_mac* to start on macOS), because they contain the system specific fragments.

What OS Specific launcher to use, will be determined via the `-runsystemcapabilities'. If you use the `native_capability` Macro without any parameters, it will use the ones of your current system.

Exporting *launch_win32* or *launch_linux* as an executable JAR creates the product: unzip it and start the executable named after the bndrun. It requires Java 21 on the `PATH`. A product export for macOS is not supported yet.

You may realize that the splash screen appears only for a blink of an eye. This is because the application starts so quickly :-)