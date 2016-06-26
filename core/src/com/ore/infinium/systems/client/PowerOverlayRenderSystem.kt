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

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import com.ore.infinium.util.getTagNullable
import com.ore.infinium.util.indices

@Wire
class PowerOverlayRenderSystem(//   public TextureAtlas m_atlas;
        private val m_world: OreWorld,
        private val m_stage: Stage) : IteratingSystem(
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

        m_container = VisTable()
        m_container.setFillParent(true)
        //      m_container.padLeft(10).padTop(10);
        //        m_container.set

        m_totalStatsTable = VisTable()
        m_totalStatsTable.top().left().pad(0f, 6f, 0f, 0f)
        m_totalStatsTable.setBackground("default-pane")

        val headerLabel = VisLabel("Electricity Resources")
        m_totalStatsTable.add(headerLabel).left()

        m_totalStatsTable.row()

        val demandLabel = VisLabel("Energy Demand:")
        m_totalStatsTable.add(demandLabel).left()

        m_circuitDemandLabel = VisLabel("-1")
        m_totalStatsTable.add<Label>(m_circuitDemandLabel)

        m_totalStatsTable.row()

        val supplyLabel = VisLabel("Energy Supply:")
        m_totalStatsTable.add(supplyLabel).left()

        m_circuitSupplyLabel = VisLabel("-1")
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

       // renderEntities(this.getWorld().delta)

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

        /*
        //todo global stats
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
        */
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
}
