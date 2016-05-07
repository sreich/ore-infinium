package com.ore.infinium.systems.client

import com.artemis.BaseSystem
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*

/**
 * ***************************************************************************
 * Copyright (C) 2014, 2015 by Shaun Reich @gmail.com>
 *
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see //www.gnu.org/licenses/>.
 * ***************************************************************************
 */
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
    private var m_drillAttackSound: Sound? = null
    private var m_dirtAttackSound: Sound? = null
    private var m_dirtPlaceSound: Sound? = null
    private var m_itemPlaceSound: Sound? = null

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

        assert(m_itemPickupSound != null) { "sound failed to load" }
    }

    override fun processSystem() {
    }

    fun playItemPickup() {
        m_itemPickupSound!!.play()
    }

    fun playItemPlace() {
        m_itemPlaceSound!!.play()
    }

    fun playDrillAttack() {
        m_drillAttackSound!!.play()
    }

    fun playDirtAttack() {
        m_dirtAttackSound!!.play()
    }

    fun playDirtPlace() {
        m_dirtPlaceSound!!.play()
    }

}
