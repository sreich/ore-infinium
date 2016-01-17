package com.ore.infinium

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align

/**
 * ***************************************************************************
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
 * ***************************************************************************
 */

/**
 * class that is an action  bar on the side of the screen.
 * simply gives quick actions to things like chat, inventory..etc.
 */
class Sidebar(stage: Stage, private val m_skin: Skin, client: OreClient) {
    private val container: Table

    init {

        container = Table(m_skin)
        container.setFillParent(true)
        container.bottom().left().setSize(20f, 400f)
        container.padLeft(10f).padBottom(400f)
        container.defaults().space(4f).align(Align.left)

        val chatButton = TextButton("chat [enter]", m_skin)
        container.add(chatButton)

        chatButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeListener.ChangeEvent, actor: Actor) {
                client.toggleChatVisible()
            }
        })

        container.row()

        val inventoryButton = TextButton("inventory [i]", m_skin)
        container.add(inventoryButton)

        inventoryButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeListener.ChangeEvent, actor: Actor) {
                client.toggleInventoryVisible()
            }
        })

        stage.addActor(container)
    }
}
