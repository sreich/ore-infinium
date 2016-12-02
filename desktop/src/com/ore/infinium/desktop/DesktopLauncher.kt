package com.ore.infinium.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.beust.jcommander.JCommander
import com.ore.infinium.*
import com.ore.infinium.desktop.texturepacker.TexturePacker
import kotlin.system.measureTimeMillis

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

        //LwjglInput.keyRepeatTime = 0.08f
        //LwjglInput.keyRepeatInitialTime = 0.15f

        if (OreSettings.help) {
            printHelp(jCommander)
            return
        }

        if (OreSettings.pack) {
            val ms = measureTimeMillis { packTextures() }
            OreWorld.log("startup texture packing", "texture packing took $ms ms")
        }

        Lwjgl3Application(OreClient(), createLwjglConfig())
    }

    private fun createLwjglConfig() =
            Lwjgl3ApplicationConfiguration().apply {
                //useOpenGL3(true, 3, 2)
                setTitle("Ore Infinium")
                setWindowedMode(OreSettings.width, OreSettings.height)
                setResizable(OreSettings.resizable)
                useVsync(OreSettings.vsyncEnabled)
                //foregroundFPS = OreSettings.framerate
                //backgroundFPS = OreSettings.framerate
            }

    private fun generateWorld() {
        OreWorld.log("DesktopLauncher generateWorld", "creating server and world to generate the world and exit.")
        val worldSize = OreWorld.WorldSize.TestTiny
        val server = OreServer(worldSize)
        val world = OreWorld(client = null, server = server,
                             worldInstanceType = OreWorld.WorldInstanceType.Server, worldSize = worldSize)

        world.init()

        OreWorld.log("DesktopLauncher generateWorld", "shutting down world. exiting.")
        world.shutdown()
    }

    private fun packTextures() {
        val settings = TexturePacker.Settings().apply {
            this.fast = true
            edgePadding = false
        }
            val settingsTiles = TexturePacker.Settings().apply {
            this.fast = true
            edgePadding = false
            combineSubdirectories = true
        }
        //fixme overridden by file, it seems.
        TexturePacker.process(settings, "blocks", "../assets/packed", "blocks")
        TexturePacker.process(settingsTiles, "tiles", "../assets/packed", "tiles")
        TexturePacker.process(settings, "ui", "../assets/packed", "ui")
        TexturePacker.process(settings, "entities", "../assets/packed", "entities")
    }

    private fun printHelp(jCommander: JCommander) {
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
