package com.ore.infinium.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.utils.Pool;
import com.ore.infinium.Block;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich@kde.org>                    *
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
public class BlockComponent extends Component implements Pool.Poolable {

    public Block.BlockType blockType;

    public void reset() {

    }

    public BlockComponent() {
    }

    public BlockComponent(BlockComponent blockComponent) {
        blockType = blockComponent.blockType;
    }
}
