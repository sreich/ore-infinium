package com.ore.infinium

import java.io.FileOutputStream

class WorldIO(val oreWorld: OreWorld) {
    fun loadWorld() {
    }

    val FILESAVE_BASE_PATH = "../saveData/"

    fun saveWorld() {
        val saveFilePath = FILESAVE_BASE_PATH + "worldsave.save"
        val fs = FileOutputStream(saveFilePath).use { fs ->

            //writeWorldHeader(fs)
            writeWorldData(fs)
        }
    }

    private fun writeWorldHeader(fs: FileOutputStream) {
        val header = PbWorldHeader.newBuilder().apply {
            sizeX = oreWorld.worldSize.width
            sizeY = oreWorld.worldSize.height
            worldName = "TEST WORLD NAME"
            worldSeed = 123456789L
        }.build()

        header.writeTo(fs)
    }

    private fun writeWorldData(fs: FileOutputStream) {
        val blocks = PbBlocks.newBuilder()
        for (y in 0 until oreWorld.worldSize.width) {
            for (x in 0 until oreWorld.worldSize.width) {
                val blockType = oreWorld.blockType(x, y).toInt()
                blocks.addBlockTypes(blockType)

                val blockWallType = oreWorld.blockWallType(x, y).toInt()
                blocks.addBlockWallTypes(blockWallType)

                val blockFlags = oreWorld.blockFlags(x, y).toInt()
                blocks.addBlockFlags(blockFlags)

                val blockLightLevel = oreWorld.blockLightLevel(x, y).toInt()
                blocks.addBlockLightLevel(blockLightLevel)
            }
        }

        val b = PbWorldSave.newBuilder().setBlocks(blocks).build()

        b.writeTo(fs)

    }

}
