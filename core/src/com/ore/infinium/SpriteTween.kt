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

import aurelienribon.tweenengine.TweenAccessor
import com.badlogic.gdx.graphics.g2d.Sprite

class SpriteTween : TweenAccessor<Sprite> {

    override fun getValues(target: Sprite, tweenType: Int, returnValues: FloatArray): Int {
        when (tweenType) {
            POS_XY -> {
                returnValues[0] = target.x
                returnValues[1] = target.y
                return 2
            }
            SCALE -> {
                returnValues[0] = target.scaleX
                returnValues[1] = target.scaleY
                return 2
            }
            COLOR -> {
                returnValues[0] = target.color.r
                returnValues[1] = target.color.g
                returnValues[2] = target.color.b
                returnValues[3] = target.color.a
                return 4
            }
            ALPHA -> {
                returnValues[0] = target.color.a
                return 1
            }
            SIZE -> {
                returnValues[0] = target.width
                returnValues[1] = target.height
                return 2
            }
            ROTATE -> {
                returnValues[0] = target.rotation
                return 1
            }
            POS_X -> {
                returnValues[0] = target.x
                return 1
            }
            POS_Y -> {
                returnValues[0] = target.y
                return 1
            }
            else -> return -1
        }
    }

    override fun setValues(target: Sprite, tweenType: Int, newValues: FloatArray) {
        when (tweenType) {
            POS_XY -> target.setPosition(newValues[0], newValues[1])
            SCALE -> target.setScale(newValues[0], newValues[1])
            COLOR -> target.setColor(newValues[0], newValues[1], newValues[2], newValues[3])
            ALPHA -> target.setColor(1f, 1f, 1f, newValues[0])
            SIZE -> target.setSize(newValues[0], newValues[1])
            ROTATE -> target.rotation = newValues[0]
            POS_X -> target.x = newValues[0]
            POS_Y -> target.y = newValues[0]
        }
    }

    companion object {

        val POS_XY = 1
        val SCALE = 2
        val COLOR = 3
        val ALPHA = 4
        val SIZE = 5
        val ROTATE = 6
        val POS_X = 7
        val POS_Y = 8
    }
}
