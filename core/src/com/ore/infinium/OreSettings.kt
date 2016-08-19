/**
MIT License

Copyright (c) 2016 Shaun Reich <sreich02@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.ore.infinium

import com.beust.jcommander.Parameter

/**
 * Singleton for runtime settings, mostly those that are obtained from
 * command line, but some are also obtained from file loading.
 * Command line should take precedence.
 *
 *
 * Some of these also do not apply, like e.g. @see help
 * it's only there because these are options that can be passed on commandline,
 * and will need to be accessed later on.
 */
object OreSettings {

    @Parameter(names = arrayOf("--help"), help = true)
    @JvmField
    var help: Boolean = false

    //client options//////
    @Parameter(names = arrayOf("--pack"),
               description = "Pack the images on ../assets, into ../assets/packed, and into their corresponding " + "texture atlases. Only images from the packed atlases will be used, so if changes are " + "made to the assets, this must be run")
    @JvmField
    var pack: Boolean = false

    @Parameter(names = arrayOf("--framerate"), description = "the framerate value to limit the game to. 0 is unlimited")
    @JvmField
    var framerate = 60

    @Parameter(names = arrayOf("--vsync"), description = "vsync enabled.")
    @JvmField
    var vsyncEnabled: Boolean = false

    @Parameter(names = arrayOf("--resizable"), description = "if set, the window will be allowed to be freely resized")
    @JvmField
    var resizable: Boolean = false

    @Parameter(names = arrayOf("--width"), description = "window width")
    @JvmField
    var width = 1600

    @Parameter(names = arrayOf("--height"), description = "window height")
    @JvmField
    var height = 900

    //////////////////////////

    /////////////// server and client network related options
    @Parameter(names = arrayOf("--hostAndJoin"),
               description = "immediately jumps into hosting a server and joining it locally. Basically singleplayer," + " but with other people being able to join, technically.")
    private val hostAndJoin: Boolean = false

    @Parameter(names = arrayOf("--host"),
               description = "Hosts a server. Additional settings that must or can be set are: port")
    private val host: Boolean = false

    @Parameter(names = arrayOf("--join"),
               description = "joins a server. Additional settings that must or can be set are: ip(required), port")
    private val join: Boolean = false

    @Parameter(names = arrayOf("--playerName"), description = "applies only to the client")
    @JvmField
    var playerName = "testplayerNameFromCommandLine"

    @Parameter(names = arrayOf("--port"))
    @JvmField
    var port = Network.PORT

    @Parameter(names = arrayOf("--ip"), description = "applies only to the client")
    @JvmField
    var ip = "localhost"

    @Parameter(names = arrayOf("--networkLog"), description = "enable network (kryonet) debug logging to system out")
    @JvmField
    var networkLog: Boolean = false

    @Parameter(names = arrayOf("--lagMin"),
               description = "emulates a slow network guaranteed to have this much latency. For network debugging.")
    @JvmField
    var lagMinMs = 0

    @Parameter(names = arrayOf("--lagMax"),
               description = "emulates a slow network guaranteed to have less than this much latency. For network " + "debugging.")
    @JvmField
    var lagMaxMs = 0

    /**
     * cheat
     */
    @Parameter(names = arrayOf("--noclip"),
               description = "enable noclip. the server will verify if authorized (if it's a local game server, then " + "it is always authorized.")
    @JvmField
    var noClip: Boolean = false

    @Parameter(names = arrayOf("--generateWorld"),
               description = "Generates the world with default params, outputs to image and immediately exits. For testing.")
    @JvmField
    var generateWorld: Boolean = false

    /// lock movement of player to continue moving right
    @JvmField
    var lockRight: Boolean = false

    /**
     * cheat
     */
    @JvmField
    var speedRun = false

    @Parameter(names = arrayOf("--debugPacketTypeStatistics"),
               description = "enable network debug to stdout for displaying frequencies of each packet received, for both client and server.")
    @JvmField
    var debugPacketTypeStatistics: Boolean = false

    @Parameter(names = arrayOf("--saveLoadWorld"),
               description = "automatically load the world at startup and save it at exit (debug).")
    @JvmField
    var saveLoadWorld: Boolean = false

    var debugRenderGui: Boolean = true

    var profilerEnabled = false

    val zoomAmount = 0.004f
}
