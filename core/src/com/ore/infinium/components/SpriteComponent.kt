package com.ore.infinium.components

import com.artemis.Component
import com.badlogic.gdx.graphics.g2d.Sprite

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
class SpriteComponent : Component() {

    @Transient var sprite = Sprite()

    var category: EntityCategory = EntityCategory.Character
    //fixme yup..gonna redo all of this and rethink using atlases, texture packer, and assetmanager
    var textureName: String? = null

    var placementValid: Boolean = false
    var visible = true

    /*
     * enabled, ignore this and every entity that can ever check for collisions against it,
     * or overlaps (useful for ignoring some on-screen client-only items like tooltips, which technically don't exist
      * in the world)
     */
    var noClip = false

    enum class EntityCategory {
        Character,
        Entity
    }

    /**
     * copy a component (similar to copy constructor)

     * @param spriteComponent
     * *         component to copy from, into this instance
     */
    fun copyFrom(spriteComponent: SpriteComponent) {
        sprite = Sprite(spriteComponent.sprite)

        textureName = spriteComponent.textureName
        category = spriteComponent.category
        noClip = spriteComponent.noClip
        placementValid = spriteComponent.placementValid
    }

    override fun toString(): String {
        val c = javaClass.simpleName
        return """
        $c.category: $category
        $c.textureName: $textureName
        $c.visible: $visible
        $c.noClip: $noClip
        $c.sprite.position: ${sprite.x}, ${sprite.y}"""
    }
}
