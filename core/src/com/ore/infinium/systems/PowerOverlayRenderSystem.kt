package com.ore.infinium.systems

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.getNullable
import com.ore.infinium.util.getTagNullable
import com.ore.infinium.util.indices

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                    *
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
     * game notes:
     *
     * -client needs to know the supply/demand total for each circuit
     *
     * -needs to know circuits it should be interested in(but not every wire in it)
     *
     * -which wire each entity belongs on. we can store an id in the powerdevice component
     *
     * when we try and dis/connect devices, we just tell the server the entity id's we want connected,
     * it will ping back as normal when devices get connected or disconnected
     *
     * PowerDeviceComponent should store: wireId, the wire it resides on. circuitid.
     *
     * wireid is needed or else we wouldn't know which entities are connected to which
     *
     * circuitid is needed because we need to know what the stats are for each circuit.
     *
     *
     * scenarios:
     *
     * - entity gets spawned in viewport. we know it's on wire n. find other entity that is also
     * on wire n, if any.
     *
     * - we connected entity 1 to 2. just send the id, handle response as usual
     *
     * - handle connection event/packet of entity 1 to 2.
     *
     */

@Wire
class PowerOverlayRenderSystem(//   public TextureAtlas m_atlas;

        private val m_world: OreWorld, private val m_stage: Stage, private val m_skin: Skin) : IteratingSystem(
        Aspect.all(PowerDeviceComponent::class.java)), RenderSystemMarker {
    var overlayVisible = false
    private lateinit var m_batch: SpriteBatch

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>
    private lateinit var powerGeneratorMapper: ComponentMapper<PowerGeneratorComponent>

    private lateinit var m_entityOverlaySystem: EntityOverlaySystem
    private lateinit var m_serverPowerCircuitSystem: ServerPowerCircuitSystem
    private lateinit var m_tagManager: TagManager

    //    public Sprite outputNode = new Sprite();

    private var m_leftClicked: Boolean = false
    private var m_dragInProgress: Boolean = false
    private var m_dragSourceEntity: Int? = null

    //displays info for the current circuit
    private var m_powerCircuitTooltipEntity: Int = 0

    private lateinit var m_container: Table
    private lateinit var m_totalStatsTable: Table

    private var currentCircuitSupply = -1
    private var currentCircuitDemand = -1

    private lateinit var m_circuitSupplyLabel: Label
    private lateinit var m_circuitDemandLabel: Label

    override fun initialize() {
        m_batch = SpriteBatch()

        m_powerCircuitTooltipEntity = world.create()

        val tooltipSprite = spriteMapper.create(m_powerCircuitTooltipEntity)
        tooltipSprite.sprite.setSize(1f, 1f)
        tooltipSprite.textureName = "debug"
        tooltipSprite.sprite.setRegion(m_world.m_atlas.findRegion(tooltipSprite.textureName))
        tooltipSprite.noClip = true

        m_container = Table(m_skin)
        m_container.setFillParent(true)
        //      m_container.padLeft(10).padTop(10);
        //        m_container.set

        m_totalStatsTable = Table(m_skin)
        m_totalStatsTable.top().left().pad(0f, 6f, 0f, 0f)
        m_totalStatsTable.setBackground("default-pane")

        val headerLabel = Label("Power Circuit Stats", m_skin)
        m_totalStatsTable.add(headerLabel).left()

        m_totalStatsTable.row()

        val demandLabel = Label("Circuit Demand:", m_skin)
        m_totalStatsTable.add(demandLabel).left()

        m_circuitDemandLabel = Label("-1", m_skin)
        m_totalStatsTable.add<Label>(m_circuitDemandLabel)

        m_totalStatsTable.row()

        val supplyLabel = Label("Circuit Supply:", m_skin)
        m_totalStatsTable.add(supplyLabel).left()

        m_circuitSupplyLabel = Label("-1", m_skin)
        m_totalStatsTable.add<Label>(m_circuitSupplyLabel)

        m_container.add<Table>(m_totalStatsTable).expand().bottom().right().size(400f, 100f)

        //        m_container.defaults().space(4);
        m_container.isVisible = false

        m_stage.addActor(m_container)
    }

    override fun dispose() {
        m_batch.dispose()
    }

    //todo sufficient until we get a spatial hash or whatever

    private fun entityAtPosition(pos: Vector2): Int? {

        var spriteComponent: SpriteComponent
        val entities = entityIds
        for (i in entities.indices) {
            val currentEntity = entities[i]
            val entityBoxed = world.getEntity(currentEntity)

            val entityTag = m_tagManager.getTagNullable(entityBoxed)

            //could be placement overlay, but we don't want this. skip over.
            if (entityTag != null && entityTag == OreWorld.s_itemPlacementOverlay) {
                continue
            }

            spriteComponent = spriteMapper.get(currentEntity)

            val rectangle = Rectangle(spriteComponent.sprite.x - spriteComponent.sprite.width * 0.5f,
                                      spriteComponent.sprite.y - spriteComponent.sprite.height * 0.5f,
                                      spriteComponent.sprite.width, spriteComponent.sprite.height)

            if (rectangle.contains(pos)) {
                return currentEntity
            }
        }

        return null
    }

    fun leftMouseClicked() {
        m_leftClicked = true

        //fixme prolly make a threshold for dragging
        m_dragInProgress = true

        //find the entity we're dragging on
        val entity = entityAtPosition(m_world.mousePositionWorldCoords()) ?: return

        var item = itemMapper.getNullable(entity)?.let { item ->
            if (item.state == ItemComponent.State.DroppedInWorld) {
                //do not allow drag starts from dropped items
                return
            }
        }

        m_dragSourceEntity = entity
    }

    fun rightMouseClicked() {
        //check if we can delete a wire
        m_serverPowerCircuitSystem.disconnectWireAtPosition(m_world.mousePositionWorldCoords())
    }

    fun leftMouseReleased() {
        m_leftClicked = false

        if (m_dragInProgress) {
            //check if drag can be connected

            if (m_dragSourceEntity != null) {
                val mouse = m_world.mousePositionWorldCoords()

                val dropEntity = entityAtPosition(Vector2(mouse.x, mouse.y))
                //if the drop is invalid/empty, or they attempted to drop on the same spot they dragged from, ignore
                if (dropEntity == null || dropEntity == m_dragSourceEntity) {
                    m_dragSourceEntity = null
                    m_dragInProgress = false
                    return
                }

                val sourcePowerDeviceComponent = powerDeviceMapper.get(m_dragSourceEntity!!)
                val dropPowerDeviceComponent = powerDeviceMapper.get(dropEntity)

                //              if (!sourcePowerDeviceComponent.outputEntities.contains(dropEntity, true) &&
                //                     !dropPowerDeviceComponent.outputEntities.contains(m_dragSourceEntity, true)) {

                //                    sourcePowerDeviceComponent.outputEntities.add(dropEntity);

                m_serverPowerCircuitSystem.connectDevices(m_dragSourceEntity!!, dropEntity)

                //               }

                m_dragSourceEntity = null
            }

            m_dragInProgress = false
        }
    }

    /**
     * Process the system.
     */
    override fun process(entityId: Int) {
        if (!overlayVisible) {
            return
        }

        //        m_batch.setProjectionMatrix(m_world.m_camera.combined);
        m_batch.projectionMatrix = m_world.m_camera.combined
        m_batch.begin()

        renderEntities(this.getWorld().delta)

        m_batch.end()

        //screen space rendering
        m_batch.projectionMatrix = m_world.m_client!!.viewport.camera.combined
        m_batch.begin()

        //fixme replace this crap w/ scene2d stuff?
        m_world.m_client!!.bitmapFont_8pt.setColor(1f, 0f, 0f, 1f)

        var fontY = 150f
        val fontX = (m_world.m_client!!.viewport.rightGutterX - 220).toFloat()

        m_batch.draw(m_world.m_atlas.findRegion("backgroundRect"), fontX - 10, fontY + 10, fontX + 100,
                     fontY - 300)

        m_world.m_client!!.bitmapFont_8pt.draw(m_batch, "Energy overlay visible (press E)", fontX, fontY)
        fontY -= 15f

        m_world.m_client!!.bitmapFont_8pt.draw(m_batch, "Input: N/A Output: N/A", fontX, fontY)

        m_world.m_client!!.bitmapFont_8pt.setColor(1f, 1f, 1f, 1f)

        m_batch.end()

        updateCircuitStats()
    }

    private fun updateCircuitStats() {
        val mouse = m_world.mousePositionWorldCoords()

        val dropEntity = entityAtPosition(Vector2(mouse.x, mouse.y))
        if (dropEntity != null) {
            val powerDeviceComponent = powerDeviceMapper.getNullable(dropEntity)
            if (powerDeviceComponent == null || powerDeviceComponent.owningCircuit == null) {
                return
            }

            currentCircuitDemand = powerDeviceComponent.owningCircuit!!.totalDemand
            currentCircuitSupply = powerDeviceComponent.owningCircuit!!.totalSupply

            m_circuitDemandLabel.setText(currentCircuitDemand.toString())
            m_circuitSupplyLabel.setText(currentCircuitSupply.toString())
        } else {
            //reset them both and update the labels
            if (currentCircuitDemand != -1) {
                currentCircuitDemand = -1
                currentCircuitSupply = -1

                m_circuitDemandLabel.setText(currentCircuitDemand.toString())
                m_circuitSupplyLabel.setText(currentCircuitSupply.toString())
            }
        }
    }

    private fun renderEntities(delta: Float) {
        //todo need to exclude blocks?

        val mouse = m_world.mousePositionWorldCoords()

        val tooltipSprite = spriteMapper.get(m_powerCircuitTooltipEntity)
        //        tooltipSprite.sprite.setPosition(mouse.x, mouse.y);

        if (m_dragInProgress && m_dragSourceEntity != null) {
            val dragSpriteComponent = spriteMapper.get(m_dragSourceEntity!!)

            m_batch.setColor(1f, 1f, 0f, 0.5f)

            //in the middle of a drag, draw powernode from source, to mouse position
            renderWire(Vector2(mouse.x, mouse.y),
                       Vector2(dragSpriteComponent.sprite.x + dragSpriteComponent.sprite.width * powerNodeOffsetRatioX,
                               dragSpriteComponent.sprite.y + dragSpriteComponent.sprite.height * powerNodeOffsetRatioY))
            m_batch.setColor(1f, 1f, 1f, 1f)
        }

        var firstEntitySpriteComponent: SpriteComponent
        var secondEntitySpriteComponent: SpriteComponent

        val serverPowerCircuitSystem = m_serverPowerCircuitSystem
        for (circuit in serverPowerCircuitSystem.m_circuits) {
            //for each device, draw a power node, a "hub" of wireConnections of sorts.
            for (generator in circuit.generators) {
                val deviceSprite = spriteMapper.get(generator)
                renderPowerNode(deviceSprite)
            }

            //do the same for devices. devices(consumers)
            for (consumer in circuit.consumers) {
                val deviceSprite = spriteMapper.get(consumer)
                renderPowerNode(deviceSprite)
            }

            //draw wires of each connection, in every circuit. Wires only have a start and end point.
            for (powerWireConnection in circuit.wireConnections) {

                firstEntitySpriteComponent = spriteMapper.get(powerWireConnection.firstEntity)
                secondEntitySpriteComponent = spriteMapper.get(powerWireConnection.secondEntity)

                //go over each output of this entity, and draw a connection from this entity to the connected dest
                renderWire(
                        Vector2(firstEntitySpriteComponent.sprite.x + firstEntitySpriteComponent.sprite.width * powerNodeOffsetRatioX,
                                firstEntitySpriteComponent.sprite.y + firstEntitySpriteComponent.sprite.height * powerNodeOffsetRatioY),
                        Vector2(secondEntitySpriteComponent.sprite.x + secondEntitySpriteComponent.sprite.width * powerNodeOffsetRatioX,
                                secondEntitySpriteComponent.sprite.y + secondEntitySpriteComponent.sprite.height * powerNodeOffsetRatioY))
            }
        }

    }

    private fun renderWire(source: Vector2, dest: Vector2) {
        val diff = Vector2(source.x - dest.x, source.y - dest.y)

        val rads = MathUtils.atan2(diff.y, diff.x)
        val degrees = rads * MathUtils.radiansToDegrees - 90

        val wireLength = Vector2.dst(source.x, source.y, dest.x, dest.y)

        m_batch.draw(m_world.m_atlas.findRegion("power-node-line"), dest.x, dest.y, 0f, 0f,
                     ServerPowerCircuitSystem.WIRE_THICKNESS, wireLength, 1.0f, 1.0f, degrees)
    }

    private fun renderPowerNode(spriteComponent: SpriteComponent) {
        val powerNodeWidth = 1f
        val powerNodeHeight = 1f

        m_batch.draw(m_world.m_atlas.findRegion("power-node-circle"),
                     spriteComponent.sprite.x + spriteComponent.sprite.width * powerNodeOffsetRatioX - powerNodeWidth * 0.5f,
                     spriteComponent.sprite.y + spriteComponent.sprite.height * powerNodeOffsetRatioY - powerNodeHeight * 0.5f,
                     powerNodeWidth, powerNodeHeight)
    }

    /**
     * handle toggling the state of if the wire editing overlay is shown.
     * including hiding other things that should not be shown while this is.
     * as well as turning on/off some state that should not be on.
     */
    fun toggleOverlay() {
        overlayVisible = !overlayVisible

        //also, turn off/on the overlays, like crosshairs, itemplacement overlays and stuff.
        //when wire overlay is visible, the entity overlays should be off.
        m_entityOverlaySystem.isEnabled = !overlayVisible
        m_entityOverlaySystem.setOverlaysVisible(!overlayVisible)

        if (overlayVisible) {
            m_container.toFront()
        }

        m_container.isVisible = overlayVisible
    }

    companion object {

        private val powerNodeOffsetRatioX = 0.1f
        private val powerNodeOffsetRatioY = 0.1f
    }

}
