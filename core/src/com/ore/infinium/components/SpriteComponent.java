package com.ore.infinium.components;

import com.artemis.Component;
import com.badlogic.gdx.graphics.g2d.Sprite;

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
public class SpriteComponent extends Component {

    public Sprite sprite = new Sprite();

    public EntityCategory category;
    //HACK yup..gonna redo all of this and rethink using atlases, texture packer, and assetmanager
    public String textureName;

    public boolean placementValid;
    public boolean visible = true;

    /*
     * enabled, ignore this and every entity that can ever check for collisions against it,
     * or overlaps (useful for ignoring some on-screen client-only items like tooltips, which technically don't exist
      * in the world)
     */
    public boolean noClip = false;

    public enum EntityCategory {
        Character,
        Entity
    }

    public SpriteComponent() {
        sprite.flip(false, true);
    }

    /**
     * copy a component (similar to copy constructor)
     *
     * @param spriteComponent
     *         component to copy from, into this instance
     */
    public void copyFrom(SpriteComponent spriteComponent) {
        sprite = new Sprite(spriteComponent.sprite);

        if (!sprite.isFlipY()) {
            sprite.flip(false, true);
        }

        textureName = spriteComponent.textureName;
        category = spriteComponent.category;
        noClip = spriteComponent.noClip;
        placementValid = spriteComponent.placementValid;
    }
}
