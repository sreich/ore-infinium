package com.ore.infinium;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

/******************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                *
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 *****************************************************************************/

/**
 * Singleton for runtime settings, mostly those that are obtained from
 * command line, but some are also obtained from file loading.
 * Command line should take precedence.
 * <p>
 * Some of these also do not apply, like e.g. @see help
 * it's only there because these are options that can be passed on commandline,
 * and will need to be accessed later on.
 */
public class OreSettings {
    private static OreSettings ourInstance = new OreSettings();

    public static OreSettings getInstance() {
        return ourInstance;
    }

    //todo we can put this stuff in a settings class i think?
    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = "--help", help = true)
    public boolean help;

    //client options//////
    @Parameter(names = "--pack",
               description = "Pack the images on ../assets, into ../assets/packed, and into their corresponding " +
                             "texture atlases. Only images from the packed atlases will be used, so if changes are " +
                             "made to the assets, this must be run")
    public boolean pack;

    @Parameter(names = "--framerate",
               description = "the framerate value to limit the game to. 0 is unlimited")
    public int framerate = 60;

    @Parameter(names = "--vsync", description = "vsync enabled.")
    public boolean vsyncEnabled;

    @Parameter(names = "--resizable", description = "if set, the window will be allowed to be freely resized")
    public boolean resizable;

    @Parameter(names = "--width", description = "window width")
    public int width = 1600;

    @Parameter(names = "--height", description = "window height")
    public int height = 900;

    //////////////////////////

    /////////////// server and client network related options
    @Parameter(names = "--hostAndJoin",
               description = "immediately jumps into hosting a server and joining it locally. Basically singleplayer," +
                             " but with other people being able to join, technically.")
    private boolean hostAndJoin;

    @Parameter(names = "--host", description = "Hosts a server. Additional settings that must or can be set are: port")
    private boolean host;

    @Parameter(names = "--join",
               description = "joins a server. Additional settings that must or can be set are: ip(required), port")
    private boolean join;

    @Parameter(names = "--playerName", description = "applies only to the client")
    public String playerName = "testplayerNameFromCommandLine";

    @Parameter(names = "--port")
    public int port = Network.PORT;

    @Parameter(names = "--ip", description = "applies only to the client")
    public String ip = "localhost";

    @Parameter(names = "--networkLog", description = "enable network (kryonet) debug logging to system out")
    public boolean networkLog;

    @Parameter(names = "--lagMin",
               description = "emulates a slow network guaranteed to have this much latency. For network debugging.")
    public int lagMinMs = 0;

    @Parameter(names = "--lagMax",
               description = "emulates a slow network guaranteed to have less than this much latency. For network " +
                             "debugging.")
    public int lagMaxMs = 0;

    @Parameter(names = "--noclip",
               description = "enable noclip. the server will verify if authorized (if it's a local game server, then " +
                             "it is always authorized.")
    public boolean noclip;
    /////////

    /// lock movement of player to continue moving right
    public boolean lockRight;

    private OreSettings() {
    }

}
