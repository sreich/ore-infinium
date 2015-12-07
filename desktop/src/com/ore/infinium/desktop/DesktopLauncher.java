package com.ore.infinium.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglInput;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.ore.infinium.ErrorDialog;
import com.ore.infinium.Network;
import com.ore.infinium.OreClient;

import java.util.ArrayList;
import java.util.List;

public class DesktopLauncher {

    //todo we can put this stuff in a settings class i think?
    @Parameter
    private List<String> parameters = new ArrayList<>();

    @Parameter(names = "--help", help = true)
    private boolean help;

    //client options//////
    @Parameter(names = "--pack",
               description = "Pack the images on ../assets, into ../assets/packed, and into their corresponding " +
                             "texture atlases. Only images from the packed atlases will be used, so if changes are " +
                             "made to the assets, this must be run")
    private boolean pack;

    @Parameter(names = "--framerate",
               description = "the framerate value to limit the game to. 0 is unlimited")
    private int framerate = 60;

    @Parameter(names = "--vsync", description = "vsync enabled.")
    private boolean vsyncEnabled;

    @Parameter(names = "--resizable", description = "if set, the window will be allowed to be freely resized")
    private boolean resizable;

    @Parameter(names = "--width", description = "window width")
    private int width = 1600;

    @Parameter(names = "--height", description = "window height")
    private int height = 900;
    //////////////////////////

    //server and client network related options
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
    private String playerName = "testplayerNameFromCommandLine";

    @Parameter(names = "--port")
    private int port = Network.port;

    @Parameter(names = "--ip", description = "applies only to the client")
    private static String ip = "localhost";
    /////////

    public static void main(String[] arg) {
        new DesktopLauncher().runGame(arg);
    }

    private void runGame(String[] arg) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            //            ExceptionDialog dialog = new ExceptionDialog("Ore Infinium Exception Handler", s, throwable);
            ErrorDialog dialog2 = new ErrorDialog(throwable, Thread.currentThread());
            dialog2.setVisible(true);
        });

        JCommander jCommander = new JCommander(this, arg);
        jCommander.setCaseSensitiveOptions(false);
        jCommander.setProgramName("Ore Infinium");

        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "Ore Infinium";

        config.width = width;
        config.height = height;
        config.resizable = resizable;
        config.vSyncEnabled = vsyncEnabled;
        config.foregroundFPS = framerate;
        config.backgroundFPS = framerate;

        if (help) {
            System.out.println("Ore Infinium - an open source block building survival game.\n" +
                               "To enable assertions, you may want to pass to the Java VM, -ea");
            //print how to use
            jCommander.usage();

            return;
        }

        if (pack) {
            TexturePacker.Settings settings = new TexturePacker.Settings();
            settings.maxWidth = 512;
            settings.maxHeight = 512;
            settings.fast = true; //fixme just to speed up debugging, overrides local settings(probably??)

            TexturePacker.process(settings, "blocks", "../assets/packed", "blocks");
            TexturePacker.process(settings, "tiles", "../assets/packed", "tiles");
            TexturePacker.process(settings, "ui", "../assets/packed", "ui");
            TexturePacker.process(settings, "entities", "../assets/packed", "entities");
        }

        LwjglInput.keyRepeatTime = 0.08f;
        LwjglInput.keyRepeatInitialTime = 0.15f;

        new LwjglApplication(new OreClient(), config);
    }
}
