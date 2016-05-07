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
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.TimeUtils
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*

@Wire
class SoundSystem(private val m_world: OreWorld) : BaseSystem() {
    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_clientNetworkSystem: ClientNetworkSystem
    private lateinit var m_tagManager: TagManager

    private var m_itemPickupSound: Sound? = null
    private var m_itemPlaceSound: Sound? = null

    private var m_drillAttackSound: Sound? = null

    private var m_dirtAttackSound: Sound? = null
    private var m_dirtPlaceSound: Sound? = null
    private var m_dirtAttackFinishSound: Sound? = null

    //last ms time it played triggered
    private var m_dirtAttackLastMs = 0L

    private var m_stoneAttackFinishSound: Sound? = null

    init {
//        m_blockAtlas = TextureAtlas(Gdx.files.internal("packed/blocks.atlas"))
        //       m_tilesAtlas = TextureAtlas(Gdx.files.internal("packed/tiles.atlas"))
    }

    override fun initialize() {
        m_itemPlaceSound = Gdx.audio.newSound(Gdx.files.internal("sounds/itemPlace.ogg"))
        m_itemPickupSound = Gdx.audio.newSound(Gdx.files.internal("sounds/itemPickup.wav"))

        m_drillAttackSound = Gdx.audio.newSound(Gdx.files.internal("sounds/drillAttack.ogg"))

        m_dirtAttackSound = Gdx.audio.newSound(Gdx.files.internal("sounds/dirtAttack.ogg"))
        m_dirtPlaceSound = Gdx.audio.newSound(Gdx.files.internal("sounds/dirtPlace.ogg"))
        m_dirtAttackFinishSound = Gdx.audio.newSound(Gdx.files.internal("sounds/dirtAttackFinish.ogg"))

        m_stoneAttackFinishSound = Gdx.audio.newSound(Gdx.files.internal("sounds/stoneAttackFinish.ogg"))

        assert(m_itemPickupSound != null) { "sound failed to load" }
    }

    override fun processSystem() {
    }

    fun playItemPickup() {
        m_itemPickupSound!!.play(0.20f)
    }

    fun playItemPlace() {
        m_itemPlaceSound!!.play()
    }

    fun playDrillAttack() {
        m_drillAttackSound!!.play(0.30f)
    }

    //todo for all of these, i think we want to have only 1 playing at a time. if another happens to play,
    //we should interrupt it maybe?
    fun playDirtAttack() {
        if (m_dirtAttackLastMs == 0L || TimeUtils.timeSinceMillis(m_dirtAttackLastMs) >= 300) {
            m_dirtAttackLastMs = TimeUtils.millis()
            m_dirtAttackSound!!.play(0.50f)
        }
    }

    fun playDirtPlace() {
        m_dirtPlaceSound!!.play()
    }

    fun playStoneAttackFinish() {
        m_stoneAttackFinishSound!!.play()
    }

    fun playDirtAttackFinish() {
        m_dirtAttackFinishSound!!.play()
    }
}
