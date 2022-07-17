# Ore Infinium

[![license](https://img.shields.io/github/license/mashape/apistatus.svg?maxAge=2592000?style=plastic)]()[![LibGDX](https://img.shields.io/badge/libgdx-1.9.6-red.svg)](https://libgdx.badlogicgames.com/)[![Kotlin](https://img.shields.io/badge/kotlin-1.1.1-orange.svg)](http://kotlinlang.org/)


[![Build Status](https://travis-ci.org/sreich/ore-infinium.svg?branch=master)](https://travis-ci.org/sreich/ore-infinium)

Ore Infinium, an open source multiplayer Terraria-inspired Sci-fi game written in [Kotlin](https://kotlinlang.org/), with a special focus on in-game tech devices, energy generation/consumption, resources gathering and the survival
through using these things. 

![World generation in the world](http://i.imgur.com/6A8fxdz.png)
![Underwater air meter](http://i.imgur.com/bAhDtkF.png)
![World generation](http://i.imgur.com/uZRsqcG.png)
![Player in game](https://i.imgur.com/EVhMT1w.png)

Inspired a lot by tekkit(now feed the beast) for minecraft, but I'm not a fan of its overwhelming complexity, although it has some really great gameplay ideas/mechanisms. I aim to incorporate some of these ideas, improve them where I can, improve the process and hopefully make it easier, less grueling and more fun. 

I'm likely going to go the route of making energy consumption/production be a global thing. For example, placing a generator will increase your global energy generation rate/stats by that much. I'm most interested in having devices depend on resources, and use them to do neat things, assuming it doesn't make things too overwhelmingly complicated for someone like me. Having to manage fuel sources for generators, switch off devices when too much is being consumed (so that important devices like charging stations, which enable you to dig), and defenses, don't stop working.

**Extremely early development!**

Basically it's only a tech demo right now, no gameplay. Unless you count walking around gameplay. Then it's got lots of that.

It is planned and written for multiplayer but so far has only been tested local
machine only.

It also will not have any lag compensation/prediction at all for a while until
I get more functionality (functionality being more important, and lag compensation
can be a bit of an after thought, as it is more time consuming and requires
a lot of tuning)..not to mention the protocol will need to be optimized,
eg things like block sending are very inefficient, as I
work on getting other features in. It also currently spams packets like
crazy, when it should be aiming for 20-30 packets/s each for client or server.

#Platforms
All desktop platforms (which can run the JVM) (Linux with OpenJDK,
Windows, macOS).

# Requirements
**You will need java 8 (jre 1.8) installed.**
**OpenGL 3.0+** (shouldn't be difficult to meet, linux open source drivers usually meet that easily these days. Any integrated gpu in the past several years should support it).

# Downloads/Builds

**Regular releases are made (runnable jars) after every commit.**
**See [github releases](https://github.com/sreich/ore-infinium/releases)**

Simply execute it by clicking, or by command line with `java -jar pathtojar`

Notable Libraries used:
* [LibGDX](https://libgdx.badlogicgames.com/)(base cross platform game framework)
* [KTX](https://github.com/czyzby/ktx/) (kotlin idiomatic wrappers around libgdx and friends)
* [artemis-odb](https://github.com/junkdog/artemis-odb) (ECS (entity component systems) library)
* [KryoNet](https://github.com/EsotericSoftware/kryonet)(networking library)
* Scene2D.ui(GUI)**, part of libgdx
* [Joise](https://github.com/SudoPlayGames/Joise)(noise module chaining framework, for generating the game world in complex ways)
* [VisUI](https://github.com/kotcrab/vis-editor/wiki/VisUI)(further extends scene2d)
* [JCommander](https://github.com/cbeust/jcommander)(command line parser)
* [gdx-ai](https://github.com/libgdx/gdx-ai)(AI library)

# Debugging/development and testing

If you wish to test and such, you'll probably want to enable assertions, as I use
them all over the place to ensure consistent game state, and sane behavior. You
must pass `-ea` ("enable assertions") to VM Options.

Should work with every IDE just fine. For IDEA, see: http://www.jetbrains.com/idea/help/setting-configuration-options.html

To build locally, just check out the code. It uses a submodule currently, for the assets. So run `git clone update --recursive <the url>`. If you've already checked out the main repo and forgot to run it with --recursive, you won't have the submodule located in ore-infinium/core/assets. So, do `git submodule update --init --recursive`

To build it with gradle, from the command line, run `./gradlew desktop:build` (or just click that gradle task from your IDE. Build the executable jar with `./gradlew desktop:dist`

I'm using Kotlin, so you can use Intellij IDEA or eclipse(if you download the plugin).

**Be sure to set the working directory to the assets directory**, or more easily, just invoke the `desktop:run` gradle task.

# Command line arguments
There are some command line arguments that can be passed to it. Find them by running `java -jar ./ore-infinium-and-what-not --help`. Some of these switches are used for development, some are used for gameplay, testing etc.

# License
Code is licensed as MIT, assets are(will be) licensed under permissive asset licenses. (CC0 mostly).
See assets dir for more detailed license info.

# Contributing and Contact
Feel free to talk to me on gitter, ask questions, get help, create issues, make patches!
