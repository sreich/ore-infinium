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

package com.ore.infinium.systems.client

import com.artemis.BaseSystem
import com.artemis.annotations.Wire
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.TimeUtils
import com.ore.infinium.OreWorld
import ktx.assets.file

@Wire
class SoundSystem(private val oreWorld: OreWorld) : BaseSystem() {
    private lateinit var itemPickupSound: Sound
    private lateinit var itemPlaceSound: Sound

    private lateinit var drillAttackSound: Sound

    private lateinit var dirtAttackSound: Sound
    private lateinit var dirtPlaceSound: Sound
    private lateinit var dirtAttackFinishSound: Sound

    //last ms time it played triggered
    private var dirtAttackLastMs = 0L

    private lateinit var stoneAttackFinishSound: Sound

    override fun initialize() {
        try {
            itemPlaceSound = Gdx.audio.newSound(file("sounds/itemPlace.ogg"))
            itemPickupSound = Gdx.audio.newSound(file("sounds/itemPickup.wav"))

            drillAttackSound = Gdx.audio.newSound(file("sounds/drillAttack.ogg"))

            dirtAttackSound = Gdx.audio.newSound(file("sounds/dirtAttack.ogg"))
            dirtPlaceSound = Gdx.audio.newSound(file("sounds/dirtPlace.ogg"))
            dirtAttackFinishSound = Gdx.audio.newSound(file("sounds/dirtAttackFinish.ogg"))

            stoneAttackFinishSound = Gdx.audio.newSound(file("sounds/stoneAttackFinish.ogg"))
        } catch (e:GdxRuntimeException) {
            error("sounds failed to load" )
        }

    }

    override fun processSystem() {
    }

    fun playItemPickup() {
        itemPickupSound.play(0.20f)
    }

    fun playItemPlace() {
        itemPlaceSound.play()
    }

    fun playDrillAttack() {
        drillAttackSound.play(0.30f)
    }

    //todo for all of these, i think we want to have only 1 playing at a time. if another happens to play,
    //we should interrupt it maybe?
    fun playDirtAttack() {
        if (dirtAttackLastMs == 0L || TimeUtils.timeSinceMillis(dirtAttackLastMs) >= 300) {
            dirtAttackLastMs = TimeUtils.millis()
            dirtAttackSound.play(0.50f)
        }
    }

    fun playDirtPlace() {
        dirtPlaceSound.play()
    }

    fun playStoneAttackFinish() {
        stoneAttackFinishSound.play()
    }

    fun playDirtAttackFinish() {
        dirtAttackFinishSound.play()
    }
}
