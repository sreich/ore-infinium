# Ore Infinium

[![Join the chat at https://gitter.im/sreich/ore-infinium](https://badges.gitter.im/sreich/ore-infinium.svg)](https://gitter.im/sreich/ore-infinium?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/sreich/ore-infinium.svg?branch=master)](https://travis-ci.org/sreich/ore-infinium)

Ore Infinium, an **Open Source** Terraria-inspired Sci-fi game with a special focus
on in-game tech devices, energy generation/consumption, resources gathering and the survival
through using these things. 

Written in **[Kotlin](https://kotlinlang.org/)** 

Built upon libraries such as: **[LibGDX](https://libgdx.badlogicgames.com/)(base cross platform game framework), artemis-odb (entity component systems), KryoNet(networking), Scene2D.ui(GUI)**

reddit: http://reddit.com/r/ore-infinium


Inspired a lot by tekkit(now feed the beast) for minecraft, but I'm not a fan of its overwhelming complexity, although it has some really great gameplay ideas/mechanisms. I aim to incorporate some of these ideas, improve them where I can, improve the process and hopefully make it easier, less grueling and more fun. 

I'm likely going to go the route of making energy consumption/production be a global thing. For example, placing a generator will increase your global energy generation rate/stats by that much. I'm most interested in having devices depend on resources, and use them to do neat things, assuming it doesn't make things too overwhelmingly complicated for someone like me. Having to manage fuel sources for generators, switch off devices when too much is being consumed (so that important devices like charging stations, which enable you to dig), and defenses, don't stop working.

![Screenshot](http://i.imgur.com/iCaUnQZ.png "Screenshot1")


**Extremely early development!**

It is planned and written for multiplayer but so far has only been tested local
machine only.

It also will not have any lag compensation/prediction at all for a while until
I get more functionality (functionality being more important, and lag compensation
can be a bit of an after thought, as it is more time consuming and requires
a lot of tuning)..not to mention the protocol will need to be optimized,
eg things like block sending are very inefficient, as I
work on getting other featuresr in. It also currently spams packets like
crazy, when it should be aiming for 20-30 packets/s each for client or server.

#Platforms
All desktop platforms (which can run the JVM) (Linux, with OpenJDK,
Windows, Mac OS X).

# Requirements
**You will need java 8 (jre 1.8) installed.**
**OpenGL 3.0+** (shouldn't be difficult to meet, linux open source drivers meet that easily. Any integrated gpu in the past several years should support it).

Tested with intellij IDEA. 

# Builds
Regular builds (jar files) can be downloaded from [my dropbox builds](https://www.dropbox.com/sh/utrgredr6xx44jd/AACycdKZElceSHSrIFSvXPkXa?dl=0)

Simply execute it by 'java -jar pathtojar'

# Debugging/development and testing

If you wish to test and such, you'll probably want to enable assertions, as I use
them all over the place to ensure consistent game state, and sane behavior. You
must pass '-ea' (which means "enable assertions") to VM Options.

For IDEA, see: http://www.jetbrains.com/idea/help/setting-configuration-options.html

To build locally, just check out the code. It uses a submodule currently, for the assets. So run git clone --recursive <the url>. If you've already checked out the main repo and forgot to run it with --recursive, you won't have the submodule located in ore-infinium/core/assets. So, do git submodule update --recursive

To build it with gradle, from the command line, run ./gradlew desktop:build (or just click that gradle task from your IDE.

I'm using Kotlin, so you can use Intellij IDEA or eclipse(if you download the plugin)

# Command line arguments
There are some command line arguments that can be passed to it. Find them by running java -jar ./ore-infinium-and-what-not --help. Some of these switches are used for development, some are used for gameplay, testing etc.
* --help to get started on what's available.
* --pack
** used for automatically packing assets into spritesheets, necessary for
debugging or any testing..I'm not sure if it actually works at runtime, outside
of a build/dev environment.

The goal is to make it easily repack them so people can test without
needing to fire up anything java developmenty.

# License
Code is licensed under GPLv2, assets are(will be) licensed under a more permissive license. (CC0)

## Why Kotlin
Chosen because it's a very nice language that released a little after I discovered it,
it improves a lot on Java's shortcomings, but also doesn't get in one's way. It's
similar enough to Java that if you know Java, it should be easy to pickup most things.

It's more pragmatic than other languages. Why it over java? Well, useful lambdas..I'm
actually using them now all the time, unlike before. And null safety is another big
one, that gives some really nice predictability. 

Static typing is great because it means the compiler can smack me before I run
it and figure out my mistake an hour later. Loads of convenience things that any
modern language will have. Unfortunately, java has none of those, and probably never will.

# Contributing and Contact
Feel free to email me (srei ch02 g mail com), feel free to ask questions and get help, create issues, make patches! 

Chat with me on gitter(github messenging):
[![Join the chat at https://gitter.im/sreich/ore-infinium](https://badges.gitter.im/sreich/ore-infinium.svg)](https://gitter.im/sreich/ore-infinium?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

