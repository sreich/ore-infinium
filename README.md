# ore-infinium

Ore Infinium, an **Open Source** Terraria-inspired Sci-fi game with a special focus
on in-game tech devices, energy generation/wiring and the survival through using
these things. 

Inspired a lot by tekkit, but I'm not a fan of its overwhelming complexity, although it has some really great gameplay ideas/mechanisms. I aim to incorporate some of these ideas, improve them where I can, improve the process and make it easier and less grueling. For instance, the wiring is done by pressing a key which shows an overlay, and dragging and dropping. So it's not actually in the physical world, which makes things much easier to manage. In general I like the idea of the player being able to createconnect devices, have them depend on resources, and use them to do neat things, assuming it doesn't make things too overwhelmingly complicated for someone like me.

Written in/using some libraries such as: **Kotlin, LibGDX, artemis-odb, KryoNet, Scene2D.ui**

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
All desktop platforms (which can run the JVM) (Linux, with OpenJDK,
Windows, Mac OS X).

# Requirements
**You will need java (jre) 1.8 (java 8).** Tested with intellij IDEA. 

# Builds
Regular builds (jar files) can be downloaded from: https://copy.com/kzGi2ilrM4lb6ZWm
Simply execute it by 'java -jar pathtojar'

To build locally, just check out the code. It uses a submodule currently, for the assets. So run git clone --recursive <the url>. If you've already checked out the main repo and forgot to run it with --recursive, you won't have the submodule located in ore-infinium/core/assets. So, do git submodule update --recursive

To build it with gradle, from the command line, run ./gradlew desktop:build (or just run that gradle task from your IDE.

# Debugging/development running

If you wish to test and such, you'll probably want to enable assertions, as I use
them all over the place to ensure consistent game state, and sane behavior. You
must pass '-ea' (which means "enable assertions") to VM Options.

For IDEA, see: http://www.jetbrains.com/idea/help/setting-configuration-options.html

# Command line commands
There are some command line commands that can be passed to it. Find them by running java -jar ./ore-infinium-and-what-not --help. Some of these switches are used for development, some are used for gameplay, testing etc.

# License
Code is licensed under GPLv2, assets are licensed under a more permissive license. (CC0)
* --pack
** used for automatically packing assets into spritesheets, necessary for
debugging or any testing..I'm not sure if it actually works at runtime, outside
of a build/dev environment.

The goal is to make it easily repack them so people can test without
needing to fire up anything java developmenty.

## Why Kotlin
Chosen because it's a very nice language that released a little after I discovered it,
it improves a lot on Java's shortcomings, but also doesn't get in one's way. It's
more pragmatic than other languages. Why it over java? Well, useful lambdas..I'm
actually using them now all the time, unlike before. And null safety is another big
one, that gives some really nice predictability. Static typing is great because
it means the compiler can smack me before I run it and figure out my mistake
an hour later. Loads of convenience things that any modern language will have.
Unfortunately, java has none of those, and probably never will.

# Contributing and Contact
Email me (sreich02 g mail com), feel free to ask questions and get help, create issues, make patches, join the irc channel on freenode (channel #ore-infinium)!

freenode: http://webchat.freenode.net/?channels=#ore-infinium

(of course, it's recommended you just get an IRC client, if you prefer. hexchat is a common one.)

