package com.ore.infinium

import com.beust.jcommander.Parameter
import java.util.*

/******************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 */

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

    @Parameter
    private val parameters = ArrayList<String>()

    @Parameter(names = arrayOf("--help"), help = true)
    var help: Boolean = false

    //client options//////
    @Parameter(names = arrayOf("--pack"), description = "Pack the images on ../assets, into ../assets/packed, and into their corresponding " + "texture atlases. Only images from the packed atlases will be used, so if changes are " + "made to the assets, this must be run")
    var pack: Boolean = false

    @Parameter(names = arrayOf("--framerate"), description = "the framerate value to limit the game to. 0 is unlimited")
    var framerate = 60

    @Parameter(names = arrayOf("--vsync"), description = "vsync enabled.")
    var vsyncEnabled: Boolean = false

    @Parameter(names = arrayOf("--resizable"), description = "if set, the window will be allowed to be freely resized")
    var resizable: Boolean = false

    @Parameter(names = arrayOf("--width"), description = "window width")
    var width = 1600

    @Parameter(names = arrayOf("--height"), description = "window height")
    var height = 900

    //////////////////////////

    /////////////// server and client network related options
    @Parameter(names = arrayOf("--hostAndJoin"), description = "immediately jumps into hosting a server and joining it locally. Basically singleplayer," + " but with other people being able to join, technically.")
    private val hostAndJoin: Boolean = false

    @Parameter(names = arrayOf("--host"), description = "Hosts a server. Additional settings that must or can be set are: port")
    private val host: Boolean = false

    @Parameter(names = arrayOf("--join"), description = "joins a server. Additional settings that must or can be set are: ip(required), port")
    private val join: Boolean = false

    @Parameter(names = arrayOf("--playerName"), description = "applies only to the client")
    var playerName = "testplayerNameFromCommandLine"

    @Parameter(names = arrayOf("--port"))
    var port = Network.PORT

    @Parameter(names = arrayOf("--ip"), description = "applies only to the client")
    var ip = "localhost"

    @Parameter(names = arrayOf("--networkLog"), description = "enable network (kryonet) debug logging to system out")
    var networkLog: Boolean = false

    @Parameter(names = arrayOf("--lagMin"), description = "emulates a slow network guaranteed to have this much latency. For network debugging.")
    var lagMinMs = 0

    @Parameter(names = arrayOf("--lagMax"), description = "emulates a slow network guaranteed to have less than this much latency. For network " + "debugging.")
    var lagMaxMs = 0

    @Parameter(names = arrayOf("--noclip"), description = "enable noclip. the server will verify if authorized (if it's a local game server, then " + "it is always authorized.")
    var noclip: Boolean = false
    /////////

    /// lock movement of player to continue moving right
    var lockRight: Boolean = false
}
