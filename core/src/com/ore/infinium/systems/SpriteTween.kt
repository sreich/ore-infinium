package com.ore.infinium.systems

import aurelienribon.tweenengine.TweenAccessor
import com.badlogic.gdx.graphics.g2d.Sprite

/******************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                *
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
 */
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
