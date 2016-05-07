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

package com.ore.infinium.components

import com.artemis.Component
import com.badlogic.gdx.graphics.g2d.Sprite
import com.ore.infinium.util.rect

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
        sprite.apply {
            setPosition(spriteComponent.sprite.x, spriteComponent.sprite.y)
        }

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
        $c.boundingRectangle: ${this.sprite.rect.toString()} [bottomleftX, bottomleftY, w, h]
        $c.sprite.width: w: ${sprite.width}, h: ${sprite.height}
        $c.sprite.position: x: ${sprite.x}, y: ${sprite.y}"""
    }
}
