This project is a simple Example that represents the default Eclipse E4 Application the PDE provides

if you want to modify it, only add runrequire bundles and resolve only he launch_base.bndrun. To start or export use the launch_linux or launch_win32 because they contain the system specific fragments.

What OS Specific launcher to use, will be determined via the `-runsystemcapabilities'. If you use the `native_capability` Macro without any parameters, it will use the ones of your current system.  