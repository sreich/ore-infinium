package com.ore.infinium;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich02@gmail.com>                    *
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
 * ***************************************************************************
 */
public class Block {
    /**
     * Which mesh sprite to use, aka subsprite.
     * This is utilized to cleanly decide which exact sprite(e.g. full block, corner pieces, etc.) to show for whatever
     * tile (e.g. dirt, grass) this is.
     * 0-15.
     * For example, @sa primitiveType
     * which does not generally depend on the surroundings.
     * <p>
     * meshType however, is determined by calculating the surrounding tiles and if they are of a simlar type or similar
     * blendType, then it will change the overall look of it.
     * <p>
     * Bottom line: use meshType ONLY for rendering, use primitiveType for everything else. meshType is only a displaying
     * niche of a detail, not a gameplay mechanic
     */
    public byte meshType;

    /**
     * The type of tile this is, 0-255 is valid and can be compared with the world's definition of tile types
     * (an enum)
     */
    public byte type;


    /**
     * 1:1 correspondence to the primitive type. just that it's rendered in the background with a darker color.
     */
    public byte wallType;

    /*
    struct BlockStruct {
        //TODO: add "flammable"
        BlockStruct(const QString& _texture, bool _collides) {
            texture = _texture;
            collides = _collides;
        }

        BlockStruct() {
        }
        QString texture;

        // I thought about using flags..but this seems better, save for the might-be-fucking-huge-constructor
        // this will be useful for TODO: blocks that hurt or help the player's health, etc. (lava), liquids of types, etc.
        boolean collides;

        //TODO: animations..array of textures for animation..for destroying and other shit
    };

    struct WallStruct {
        WallStruct(const QString& _texture) {
            texture = _texture;
        }

        WallStruct() {
        }
        QString texture;
    };
    */

    //static std::vector<BlockStruct> blockTypes;
    //static std::vector<WallStruct> wallTypes;
    /**
     * if != 0 (WallType::Null), then this is an "underground wall tile" and the user cannot remove/add/change it in any way.
     *
     * @sa WallType
     */
    /// NOTE: block ownership is stored in the Player class, which just stores a list of indices of tiles which the player 'owns'.

    /**
     * Determines the health and texture of the Block.
     */
    public static final class BlockType {
        public static final byte NullBlockType = 0;
        public static final byte DirtBlockType = 1;
        public static final byte StoneBlockType = 2;
        public static final byte CopperBlockType = 3;
    }

    public static final class WallType {
        public static final byte NullWallType = 0;
        public static final byte DirtWallType = 1;
        public static final byte DirtUndergroundWallType = 2;
    }

    public byte flags;

    /**
     * properly destroys the block (sets meshtype, flags etc to defaults)
     * must be called when destroying a block.
     */
    void destroy() {
        type = Block.BlockType.NullBlockType;
        meshType = 0;
        wallType = 0;
        flags = 0;
    }

    public final void setFlag(byte flag) {
        flags |= flag;
    }

    public final boolean hasFlag(byte flag) {
        return (this.flags & flag) != 0;
    }

    public void unsetFlag(byte flag) {
        this.flags &= ~flag;
    }

    public static final class BlockFlags {
        public static final byte OnFireBlock = 1 << 0;
        public static final byte GrassBlock = 1 << 1;
//        public static final int GrassBlock = 1 << 2;
    }



    /*
    public static enum BlockFlags {
        OnFireBlockFlag((byte) (1 << 0)),
        SunlightVisible((byte) (1 << 1)),
        GrassBlock((byte) (1 << 2));

        /// theoretically more things belong in here. except i ran out of ideas :(
        // OnFireBlockFlag(1 << 0)
        BlockFlags(byte blah) {

        }

    }
    */
}
