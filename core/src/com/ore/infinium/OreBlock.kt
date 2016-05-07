package com.ore.infinium

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich @gmail.com>                    *
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
 * ***************************************************************************
 */
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
     * Determines the health and texture of the Block.
     */
    object BlockType {
        const val NullBlockType: Byte = 0
        const val DirtBlockType: Byte = 1
        const val StoneBlockType: Byte = 2
        const val CopperBlockType: Byte = 3
    }

    object WallType {
        const val NullWallType: Byte = 0
        const val DirtWallType: Byte = 1
        const val DirtUndergroundWallType: Byte = 2
    }

    fun destroy() {

    }

    object BlockFlags {
        const val OnFireBlock = (1 shl 0).toByte()
        const val GrassBlock = (1 shl 1).toByte()
        //        public static final int GrassBlock = 1 << 2;
        /*
        SunlightVisible((byte) (1 << 1)),
        GrassBlock((byte) (1 << 2));

        /// theoretically more things belong in here. except i ran out of ideas :(
        // OnFireBlockFlag(1 << 0)
        */
    }

    companion object {

        /**
         * number of byte fields we use for each block.
         * because they're all stored in one array, as primitives.
         *
         *
         * As follows are:
         * -meshType
         * -type
         * -wallType
         * -flags
         */
        const val BLOCK_FIELD_COUNT = 4

        /**
         * index offset within the big byte block array,
         * since BLOCK_FIELD_COUNT elements are stored for each
         * actual block
         */
        const val BLOCK_FIELD_INDEX_TYPE = 0
        const val BLOCK_FIELD_INDEX_MESHTYPE = 1
        const val BLOCK_FIELD_INDEX_WALLTYPE = 2
        const val BLOCK_FIELD_INDEX_FLAGS = 3
    }
}
