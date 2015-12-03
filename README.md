# ore-infinium

Ore Infinium, an Open Source Terraria-inspired Sci-fi game with a special focus
on in-game tech devices energy generation/wiring and the survival through using
these things.

Written in Java 8, LibGDX, Ashley, KryoNet, Scene2D.ui. Cross platform.

reddit: http://reddit.com/r/ore-infinium

![Screenshot](http://i.imgur.com/iCaUnQZ.png "Screenshot1")


**Extremely early development!**

It is planned and written for multiplayer but so far has only been tested local
machine only.

It also will not have any lag compensation/prediction etc
whatsoever for a while until I get more functionality (functionality being more
important, and lag compensation can be a bit of an after thought, as it is more
time consuming and requires a lot of tuning)..not to mention the protocol will
need to be optimized, eg things like chunk sending are very inefficient, as I
work on getting other featuresr in.

#Platforms
All desktop platforms (which can run the JVM), presently (Linux, with OpenJDK,
Windows, Mac OS X).

# Requirements
**You will need java 1.8 (java 8).** Tested with intellij IDEA. 

# Builds
Regular builds (jar files) can be downloaded from: https://copy.com/q84maUflQnyT2C8Z
Simply execute it by 'java -jar pathtojar'

# Debugging/development running

If you wish to test and such, you'll likely want to enable assertions, as I use
them all over the place to ensure consistent game state, and sane behavior. You
must pass '-ea' (which means "enable assertions") to VM Options.

For IDEA, see: http://www.jetbrains.com/idea/help/setting-configuration-options.html

# Command line commands
There are some command line commands that can be passed to it.

* --pack
** used for automatically packing assets into spritesheets, necessary for
debugging or any testing..I'm not sure if it actually works at runtime, outside
of a build/dev environment.

The goal is to make it easily repack them so people can test without
needing to fire up anything java developmenty.



# License
Code is licensed under GPL, assets are licensed under various more permissive licenses. (CC0 etc)

# Contributing
Email me, create issues, join the irc channel on freenode (#ore-infinium), and get involved!

