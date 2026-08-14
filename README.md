# Wormhole X-Treme v1.1.6

**Tested on 1.21.11 and 26.2**

## New Install:

0. Download the jar containing plugin, hsqldb, and gate shape files.

1. Place the file into the plugins/ folder. Once the server fully started, the plugin should
   place all the necessary files into plugins/WormholeXTreme.
   
2.   (Optional) Stop the server and edit the newly generated /plugins/WormholeXTreme/Settings.txt 
     file as needed.

3.1. (Optional) Edit gate shapes in /plugins/WormholeXTreme/GateShapes/

3.2. (Optional) Edit gate shape prices in /plugins/WormholeXTreme/GateShapePrices.txt

3.3. (Optional) If using the Permissions plugin based plugin, set up the appropriate permissions.
     If using SIMPLE_PERMISSIONS = true remember to use permissions from the Simple Mode list, 
     otherwise use nodes from Complex Node.
   
3.4. (Optional) Start Server again


## Update:

0.   Download the jar containing plugin, hsqldb, and gate shape files.

1.   Place the WormholeXTreme-1.1.6.jar to your plugins/ folder, overwriting existing file.

2.   (Optional, if upgrading to 3d shapes) If upgrading from v0.833 or earlier, start server before 
     updating gate shapes. 
     
2.1. (Optional, if upgrading to 3d shapes) Shut down, Replace gate shapes with new shapes from 3d
     GateShapes/3d/ folder. Edit shapes as needed.
   
3.   Start server.

4.   If gates act weird, remove them and re-add them.



## Permissions Plugin Nodes: (Controlled by plugins/WormholeXTreme/settings.txt - SIMPLE_PERMISSIONS)

    SIMPLE_PERMISSIONS = false
    Complex Mode:
    wormhole.use.sign - lets a user use sign gates.
    wormhole.use.dialer - lets a user use '/dial' gates
    wormhole.use.compass - lets a user use '/wxcompass' command.
    wormhole.remove.own - lets a user remove a gate that they own
    wormhole.remove.all - lets a user remove any gate
    wormhole.build - Able to build new wormholes
    wormhole.config - Able to configure settings like material and timeout
    wormhole.list.all - Able to use '/wxlist all' command to list all wormholes
    wormhole.list.self - Able to use '/wxlist self' command to list own wormholes
    wormhole.top - Able to use '/wxtop' command to list most used wormholes
    wormhole.network.use.NETWORKNAME - Able to use wormholes on NETWORKNAME
    wormhole.network.build.NETWORKNAME - Able to build wormholes on NETWORKNAME
    wormhole.go - allows user to use '/wxgo' command.
    wormhole.build.groupone - Part of build restriction group one.
    wormhole.build.grouptwo - Part of build restriction group two.
    wormhole.build.groupthree - Part of build restriction group three.
    wormhole.cooldown.groupone - Part of wormhole use cooldown group one.
    wormhole.cooldown.grouptwo - Part of wormhole use cooldown group two.
    wormhole.cooldown.groupthree - Part of wormhole use cooldown group three.

    SIMPLE_PERMISSIONS = true
    Simple Mode:
    wormhole.simple.use - Lets a user use wormholes.
    wormhole.simple.build - Lets a user build wormholes.
    wormhole.simple.remove - Lets a user remove wormholes.
    wormhole.simple.config - Lets a user configure WormholeXTreme settings


## Building from source:

 0. Install Maven and all of its dependencies. 

 1.   Git clone the WX repo.

 1.1. Wormhole X-Treme git repo: https://github.com/Daktoo/Wormhole-X-Treme.git

 2.   Run a `mvn clean up` in the directory you ran the git clone of WX into.
  
 3.   Enjoy plugin that is now in target/. It will be a jar or a package zip.   
