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

package com.ore.infinium.util

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Event
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.utils.Align


/**
 * convenience function
 *
 * @return true if the given milliseconds time is past
 * the given last milliseconds time, by the specified
 * threshold/interval
 * @param currentMillis
 * @param intervalMs
 * @param lastMs
 */
fun timeMsSurpassed(currentMs: Long, lastMs: Long, intervalMs: Int) = (currentMs - lastMs) > intervalMs

/**
 * wrapper class around interface to avoid having to
 * implement each member
 */
interface OreApplicationListener : ApplicationListener {
    override fun resize(width: Int, height: Int) = Unit
    override fun create() = Unit
    override fun render() = Unit
    override fun resume() = Unit
    override fun dispose() = Unit
    override fun pause() = Unit
}

/**
 * deriving from InputListener for the sole purpose of getting
 * guarantees on nullability
 */
open class OreInputListener : InputListener() {
    override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor) = Unit
    override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor) = Unit
    override fun handle(e: Event) = false
    override fun keyDown(event: InputEvent, keycode: Int) = false
    override fun keyTyped(event: InputEvent, character: Char) = false
    override fun keyUp(event: InputEvent, keycode: Int) = false
    override fun mouseMoved(event: InputEvent, x: Float, y: Float) = false
    override fun scrolled(event: InputEvent, x: Float, y: Float, amount: Int) = false
    override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) = false
    override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) = Unit
    override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) = Unit
}

interface OreInputProcessor : InputProcessor {
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    override fun keyDown(keycode: Int) = false
    override fun keyTyped(character: Char) = false
    override fun keyUp(keycode: Int) = false
    override fun mouseMoved(screenX: Int, screenY: Int) = false
    override fun scrolled(amount: Int) = false
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = false
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = false
}

/**
 * x, y is top left. width, height bottom right
 */
val Sprite.rect: Rectangle get() = Rectangle(x - (width * 0.5f), y - (height * 0.5f), width, height)
val Sprite.size: Vector2 get() = Vector2(width, height)

/**
 * construct an appropriately offset rect from
 * size and vector position
 */
fun Vector2.rectFromSize(width: Float, height: Float): Rectangle {
    return Rectangle(x - (width * 0.5f), y - (height * 0.5f), width, height)
}

val Rectangle.bottom: Float get() = y + height
val Rectangle.bottomi: Int get() = bottom.toInt()

val Rectangle.top: Float get() = y
val Rectangle.topi: Int get() = top.toInt()

val Rectangle.left: Float get() = x
val Rectangle.lefti: Int get() = left.toInt()

val Rectangle.right: Float get() = x + width
val Rectangle.righti: Int get() = right.toInt()

val Rectangle.halfWidth: Float get() = width * 0.5f
val Rectangle.halfHeight: Float get() = height * 0.5f

/**
 * @returns true if the this rectangle overlaps the other,
 * plus some padding
 * i.e. the overlap will get triggered that much sooner
 * (rect + padding).
 *
 * useful for when you want objects to stay away from each other
 * for that amount of padding/buffer area.
 */
fun Rectangle.overlapsPadded(rect: Rectangle, padding: Float): Boolean {
    return x < (rect.x + rect.width + padding) &&
            (x + width + padding) > rect.x &&
            y < (rect.y + rect.height + padding) &&
            (y + height + padding) > rect.y
//    return this.overlaps(rect)
}

/**
 * get next random int, in the range
 */
fun RandomXS128.nextInt(start: Int, end: Int): Int {
    return start + nextInt(end - start + 1)
}

val MAX_SPRITES_PER_BATCH = 5460

/**
 * get some type safety around libgdx's alignments
 */
enum class GdxAlign(val alignValue: Int) {
    Bottom(Align.bottom),
    BottomLeft(Align.bottomLeft),
    BottomRight(Align.bottomRight),
    Center(Align.center),
    Left(Align.left),
    Right(Align.right),
    Top(Align.top),
    TopLeft(Align.topLeft),
    TopRight(Align.topRight)
}
