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

import com.ore.infinium.util.Color2
import java.awt.Color
import java.util.*

class OreBlock private constructor() {

    /**
     * Which mesh sprite to use, aka subsprite.
     * This is utilized to cleanly decide which exact sprite(e.g. full block, corner pieces, etc.) to show for whatever
     * tile (e.g. dirt, grass) this is.
     *
     * For example, @sa primitiveType
     * which does not generally depend on the surroundings.
     *
     *
     * meshType however, is determined by calculating the surrounding tiles and if they are of a similar type or similar
     * blendType, then it will change the overall look of it.
     *
     *
     * Bottom line: use meshType ONLY for rendering, use primitiveType for everything else. meshType is only a
     * displaying niche of a detail, not a gameplay mechanic
     */
    //public byte meshType;

    /**
     * The type of tile this is, 0-255 is valid and can be compared with the world's definition of tile types
     * (an enum)
     */
    //public byte type;

    /**
     * 1:1 correspondence to the primitive type. just that it's rendered in the background with a darker color.
     */
    //public byte wallType;

    // TODO: blocks that hurt or help the player's health, etc. (lava), liquids of types,
    //TODO: animations..array of textures for animation..for destroying and other shit

    /**
     * todo obsidian (only from when lava hits water)? or should we just use stone
     */
    enum class BlockType(val oreValue: Byte) {
        Air(0),
        Sand(1),
        Dirt(2),
        Stone(3),
        Copper(4),
        Coal(5),
        Iron(6),
        Silver(7),
        Gold(8),
        Uranium(9),
        Diamond(10),
        Bedrock(11),
        Water(12),
        Lava(13)
    }

    enum class WallType(val oreValue: Byte) {
        Air(0),
        Dirt(1),
        DirtUnderground(2)
    }

    fun destroy() {

    }

    object BlockFlags {
        /// theoretically more things belong in here. except i ran out of ideas :(
        const val OnFireBlock = (1 shl 0).toByte()
        const val GrassBlock = (1 shl 1).toByte()

        /**
         * whether or not this block currently has sunlight access.
         * this flag is used to indicate a global illumination,
         * is recalculated on block destruction.
         *
         * this way, we can decide to darken every tile which
         * hits the sunlight, by a global illumination when
         * dusk starts approaching. it probably won't be used
         * for logic though, just a visual aesthetic
         *
         * this is also in addition to the per light
         */
        const val SunlightVisible = (1 shl 2).toByte()

    }

    companion object {
        /**
         * map ores to a color so we can output the world as a pixmap
         */
        val OreNoiseColorMap = mapOf(BlockType.Dirt.oreValue to Color2.BROWN,
                                     BlockType.Sand.oreValue to Color.ORANGE,
                                     BlockType.Stone.oreValue to Color.GRAY,
                                     BlockType.Copper.oreValue to Color2.NEON_CARROT,
                                     BlockType.Diamond.oreValue to Color2.TEAL,
                                     BlockType.Gold.oreValue to Color.YELLOW,
                                     BlockType.Coal.oreValue to Color.BLACK,
                                     BlockType.Silver.oreValue to Color2.SILVER,
                                     BlockType.Iron.oreValue to Color2.TERRA_COTTA,
                                     BlockType.Uranium.oreValue to Color2.LIME_GREEN,
                                     BlockType.Iron.oreValue to Color2.RED4,
                                     BlockType.Bedrock.oreValue to Color.CYAN,
                                     BlockType.Air.oreValue to Color2.SKY_BLUE,
                                     BlockType.Water.oreValue to Color2.WATER_BLUE,
                                     BlockType.Lava.oreValue to Color.RED
                                    )

        /**
         * looks up the texture prefix name for each block type. e.g. Dirt -> "dirt", etc.
         */
        val blockAttributes = HashMap<Byte, BlockAttributes>()

        init {
            blockAttributes.put(OreBlock.BlockType.Air.oreValue,
                                BlockAttributes(textureName = "NULL because it's air",
                                                collision = BlockAttributes.Collision.False,
                                                category = BlockAttributes.BlockCategory.Null,
                                                blockTotalHealth = 0))

            blockAttributes.put(OreBlock.BlockType.Dirt.oreValue,
                                BlockAttributes(textureName = "dirt",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Dirt,
                                                blockTotalHealth = 200))

            blockAttributes.put(OreBlock.BlockType.Stone.oreValue,
                                BlockAttributes(textureName = "stone",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Sand.oreValue,
                                BlockAttributes(textureName = "sand",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Dirt,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Copper.oreValue,
                                BlockAttributes(textureName = "copper",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Iron.oreValue,
                                BlockAttributes(textureName = "iron",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Silver.oreValue,
                                BlockAttributes(textureName = "silver",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Gold.oreValue,
                                BlockAttributes(textureName = "gold",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Coal.oreValue,
                                BlockAttributes(textureName = "coal",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Uranium.oreValue,
                                BlockAttributes(textureName = "uranium",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Diamond.oreValue,
                                BlockAttributes(textureName = "diamond",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Bedrock.oreValue,
                                BlockAttributes(textureName = "bedrock",
                                                collision = BlockAttributes.Collision.True,
                                                category = BlockAttributes.BlockCategory.Ore,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Water.oreValue,
                                BlockAttributes(textureName = "water",
                                                collision = BlockAttributes.Collision.False,
                                                category = BlockAttributes.BlockCategory.Liquid,
                                                blockTotalHealth = 300))

            blockAttributes.put(OreBlock.BlockType.Lava.oreValue,
                                BlockAttributes(textureName = "lava",
                                                collision = BlockAttributes.Collision.False,
                                                category = BlockAttributes.BlockCategory.Liquid,
                                                blockTotalHealth = 300))
        }

        /**
         * number of byte fields we use for each block.
         * because they're all stored in one array, as primitives.
         * each is a byte..obviously
         *
         * some of these fields are client-side only
         *
         * As follows are(in no particular order):
         * -meshType (todo in the future this should be client only)
         * -type
         * -wallType
         * -flags
         * -light level
         */
        const val BLOCK_BYTE_FIELD_COUNT = 5

        /**
         * these are all index offsets within the big byte block array,
         * since BLOCK_FIELD_COUNT elements are stored for each
         * actual block
         */

        /**
         * tile type (dirt, stone, ...)
         */
        const val BLOCK_BYTE_FIELD_INDEX_TYPE = 0

        /**
         * precalculated mesh type, to determine how tiles should
         * transition with each other.
         *
         * CLIENT SIDE ONLY
         */
        const val BLOCK_BYTE_FIELD_INDEX_MESHTYPE = 1

        const val BLOCK_BYTE_FIELD_INDEX_WALL_TYPE = 2

        /**
         * Light level of each tile, impacted by sunlight,
         * player placed lights, and so on.
         *
         * NOTE: this byte is actually rather underused.
         * we're only using light levels of < 255
         * @see TileLightingSystem
         */
        const val BLOCK_BYTE_FIELD_INDEX_LIGHT_LEVEL = 3

        /**
         * additional flags to store in the block
         * @see BlockFlags
         */
        const val BLOCK_BYTE_FIELD_INDEX_FLAGS = 4

        fun nameOfBlockType(blockType: Byte?): String? {
            return OreBlock.BlockType.values().firstOrNull { it.oreValue == blockType }?.name
        }

    }

    class BlockAttributes internal constructor(var textureName: String //e.g. "dirt", "stone", etc.
                                               ,
                                               /**
                                                * whether or not things should collide with this block
                                                */
                                               var collision: BlockAttributes.Collision,
                                               var category: BlockAttributes.BlockCategory,
                                               blockTotalHealth: Short) {

        /**
         * max starting health of the block
         */
        var blockTotalHealth: Float = 0f

        //if this type is a type of ore (like stone, copper, ...)
        enum class BlockCategory {
            Null,
            Dirt,
            Ore,
            Liquid
        }

        enum class Collision {
            True,
            False
        }

        init {
            this.blockTotalHealth = blockTotalHealth.toFloat()
        }
    }

}
