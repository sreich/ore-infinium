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

    init {
//        m_blockAtlas = TextureAtlas(Gdx.files.internal("packed/blocks.atlas"))
        //       m_tilesAtlas = TextureAtlas(Gdx.files.internal("packed/tiles.atlas"))
    }

    override fun initialize() {
        m_itemPickupSound = Gdx.audio.newSound(Gdx.files.internal("sounds/itemPickup.wav"))
        assert(m_itemPickupSound != null) { "sound failed to load" }
    }

    override fun processSystem() {
    }

    fun itemPickup() {
        m_itemPickupSound!!.play()
    }
}
