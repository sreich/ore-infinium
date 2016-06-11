package com.ore.infinium.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglInput
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.beust.jcommander.JCommander
import com.ore.infinium.*

class DesktopLauncher {

    private fun runGame(arg: Array<String>) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            //            ExceptionDialog dialog = new ExceptionDialog("Ore Infinium Exception Handler", s, throwable);
            val dialog2 = ErrorDialog(throwable, Thread.currentThread())
            dialog2.isVisible = true
        }

        //inject jcommander into OreSettings, to properly parse args
        //into respective annotated variables
        val jCommander = JCommander().apply {
            addObject(OreSettings)
            setCaseSensitiveOptions(false)
            setProgramName("Ore Infinium")
            parse(*arg)
        }

        if (OreSettings.generateWorld) {
            generateWorld()
            return
        }

        LwjglInput.keyRepeatTime = 0.08f
        LwjglInput.keyRepeatInitialTime = 0.15f

        if (OreSettings.help) {
            printHelp(jCommander)
            return
        }

        if (OreSettings.pack) {
            packTextures()
        }

        LwjglApplication(OreClient(), createLwjglConfig())
    }

    private fun createLwjglConfig(): LwjglApplicationConfiguration {
        val config = LwjglApplicationConfiguration().apply {

            useGL30 = true
            title = "Ore Infinium"
            //addIcon()

            //borderless window mode
            //System.setProperty("org.lwjgl.opengl.Window.undecorated", "true");
            //width = LwjglApplicationConfiguration.getDesktopDisplayMode().width;
            //height = LwjglApplicationConfiguration.getDesktopDisplayMode().height;

            width = OreSettings.width
            height = OreSettings.height
            resizable = OreSettings.resizable
            vSyncEnabled = OreSettings.vsyncEnabled
            foregroundFPS = OreSettings.framerate
            backgroundFPS = OreSettings.framerate
        }

        return config
    }

    private fun generateWorld() {
        OreWorld.log("DesktopLauncher generateWorld", "creating server and world to generate the world and exit.")
        val server = OreServer()
        val world = OreWorld(m_client = null, m_server = server,
                             worldInstanceType = OreWorld.WorldInstanceType.Server)

        world.init()

        OreWorld.log("DesktopLauncher generateWorld", "shutting down world. exiting.")
        world.shutdown()
    }

    private fun packTextures() {
        //TexturePacker.Settings settings = new TexturePacker.Settings();
        //settings.maxWidth = 512;
        //settings.maxHeight = 512;
        //            settings.pot = true;
        //settings.fast = true; //fixme just to speed up debugging, overrides local settings(probably??)
        //lwjglfiles().internal("blah")
        TexturePacker.process("blocks", "../assets/packed", "blocks")
        TexturePacker.process("tiles", "../assets/packed", "tiles")
        TexturePacker.process("ui", "../assets/packed", "ui")
        TexturePacker.process("entities", "../assets/packed", "entities")
    }

    private fun printHelp(jCommander: JCommander) {
        printHelp(jCommander)
        println("Ore Infinium - an open source block building survival game.\n" + "To enable assertions, you may want to pass to the Java VM, -ea")
        //print how to use
        jCommander.usage()
    }

    companion object {
        @JvmStatic fun main(arg: Array<String>) {
            DesktopLauncher().runGame(arg)
        }
    }
}
