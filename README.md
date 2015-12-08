# ore-infinium

Click build status for viewing the jenkins result (including tests run etc)

[![Build Status](http://sreich.net:8090/job/ore%20infinium/badge/icon)]
(http://sreich.net:8090/job/ore%20infinium/)

Sonarqube is a pretty nice floss site that will analyze source code for duplicates, scan for issues (static analysis), and even tell you the complexity of lines, functions, files. Lots more things, very nice to view:

[**Sonarqube code analysis**] (http://sreich.net:9000/dashboard/index?id=org.ore.infinium)

Ore Infinium, an **Open Source** Terraria-inspired Sci-fi game with a special focus
on in-game tech devices, energy generation/wiring and the survival through using
these things. 

Inspired a lot by tekkit, but I'm not a fan of its overwhelming complexity, although it has some really great gameplay ideas/mechanisms. I aim to incorporate some of these ideas, improve them where I can, improve the process and make it easier and less grueling. For instance, the wiring is done by pressing a key which shows an overlay, and dragging and dropping. So it's not actually in the physical world, which makes things much easier to manage.

Written in/using some libraries such as: **Java 8, LibGDX, artemis-odb, KryoNet, Scene2D.ui**

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

To build locally, just check out the code, run ./gradlew desktop:build

# Debugging/development running

If you wish to test and such, you'll probably want to enable assertions, as I use
them all over the place to ensure consistent game state, and sane behavior. You
must pass '-ea' (which means "enable assertions") to VM Options.

For IDEA, see: http://www.jetbrains.com/idea/help/setting-configuration-options.html

# Command line commands
There are some command line commands that can be passed to it. Find them by running java -jar ./ore-infinium-and-what-not --help. Some of these switches are used for development, some are used for gameplay, testing etc.

# License
Code is licensed under GPLv2, assets are licensed under a more permissive license. (CC0)

# Contributing and Contact
Email me (sreich02 at gmail com), feel free to ask questions and get help, create issues, make patches, join the irc channel on freenode (channel #ore-infinium), and get involved!

freenode: http://webchat.freenode.net/?channels=#ore-infinium

(of course, it's recommended you just get an IRC client, if you prefer. hexchat is a common one.)

