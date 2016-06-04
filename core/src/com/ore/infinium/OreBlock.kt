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

class OreBlock private constructor() {

    /**
     * Which mesh sprite to use, aka subsprite.
     * This is utilized to cleanly decide which exact sprite(e.g. full block, corner pieces, etc.) to show for whatever
     * tile (e.g. dirt, grass) this is.
     * 0-15.
     * For example, @sa primitiveType
     * which does not generally depend on the surroundings.
     *
     *
     * meshType however, is determined by calculating the surrounding tiles and if they are of a simlar type or similar
     * blendType, then it will change the overall look of it.
     *
     *
     * Bottom line: use meshType ONLY for rendering, use primitiveType for everything else. meshType is only a
     * displaying
     * niche of a detail, not a gameplay mechanic
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
        Air (0),
        Sand(1),
        Dirt(2),
        Stone (3),
        Copper (4),
        Coal (5),
        Iron (6),
        Silver (7),
        Gold (8),
        Uranium (9),
        Diamond (10),
        Bedrock (11)
    }

    enum class WallType(val oreValue: Byte) {
        Air (0),
        Dirt (1),
        DirtUnderground (2)
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
         * number of byte fields we use for each block.
         * because they're all stored in one array, as primitives.
         * each is a byte..obviously
         *
         *
         * As follows are:
         * -meshType
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
    }
}
