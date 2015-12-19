package com.ore.infinium.systems;

import aurelienribon.tweenengine.TweenAccessor;
import com.badlogic.gdx.graphics.g2d.Sprite;

/******************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                *
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
 *****************************************************************************/
public class SpriteTween implements TweenAccessor<Sprite> {

    public static final int POS_XY = 1;
    public static final int SCALE = 2;
    public static final int COLOR = 3;
    public static final int ALPHA = 4;
    public static final int SIZE = 5;
    public static final int ROTATE = 6;
    public static final int POS_X = 7;
    public static final int POS_Y = 8;

    @Override
    public int getValues(Sprite target, int tweenType, float[] returnValues) {
        switch (tweenType) {
            case POS_XY:
                returnValues[0] = target.getX();
                returnValues[1] = target.getY();
                return 2;
            case SCALE:
                returnValues[0] = target.getScaleX();
                returnValues[1] = target.getScaleY();
                return 2;
            case COLOR:
                returnValues[0] = target.getColor().r;
                returnValues[1] = target.getColor().g;
                returnValues[2] = target.getColor().b;
                returnValues[3] = target.getColor().a;
                return 4;
            case ALPHA:
                returnValues[0] = target.getColor().a;
                return 1;
            case SIZE:
                returnValues[0] = target.getWidth();
                returnValues[1] = target.getHeight();
                return 2;
            case ROTATE:
                returnValues[0] = target.getRotation();
                return 1;
            case POS_X:
                returnValues[0] = target.getX();
                return 1;
            case POS_Y:
                returnValues[0] = target.getY();
                return 1;
            default:
                return -1;
        }
    }

    @Override
    public void setValues(Sprite target, int tweenType, float[] newValues) {
        switch (tweenType) {
            case POS_XY:
                target.setPosition(newValues[0], newValues[1]);
                break;
            case SCALE:
                target.setScale(newValues[0], newValues[1]);
                break;
            case COLOR:
                target.setColor(newValues[0], newValues[1], newValues[2], newValues[3]);
                break;
            case ALPHA:
                target.setColor(1, 1, 1, newValues[0]);
                break;
            case SIZE:
                target.setSize(newValues[0], newValues[1]);
                break;
            case ROTATE:
                target.setRotation(newValues[0]);
                break;
            case POS_X:
                target.setX(newValues[0]);
                break;
            case POS_Y:
                target.setY(newValues[0]);
                break;
            default:
        }
    }
}
