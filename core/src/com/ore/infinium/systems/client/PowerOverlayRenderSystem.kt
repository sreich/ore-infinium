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

import com.artemis.annotations.Wire
import com.artemis.managers.TagManager
import com.artemis.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.ore.infinium.OreWorld
import com.ore.infinium.components.PowerDeviceComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.util.RenderSystemMarker
import com.ore.infinium.util.allOf
import com.ore.infinium.util.mapper
import com.ore.infinium.util.system

@Wire
class PowerOverlayRenderSystem(private val oreWorld: OreWorld, private val stage: Stage)
        : IteratingSystem(allOf(PowerDeviceComponent::class)), RenderSystemMarker {

    var overlayVisible = false
    private lateinit var batch: SpriteBatch

    private val mSprite by mapper<SpriteComponent>()

    private val entityOverlaySystem by system<EntityOverlaySystem>()
    private val tagManager by system<TagManager>()

    //displays info for the current circuit
    private var powerCircuitTooltipEntity: Int = 0

    private lateinit var container: Table
    private lateinit var totalStatsTable: Table

    private lateinit var circuitSupplyLabel: Label
    private lateinit var circuitDemandLabel: Label

    override fun initialize() {
        batch = SpriteBatch()

        powerCircuitTooltipEntity = world.create()

        val tooltipSprite = mSprite.create(powerCircuitTooltipEntity)
        tooltipSprite.sprite.setSize(1f, 1f)
        tooltipSprite.textureName = "debug"
        tooltipSprite.sprite.setRegion(oreWorld.atlas.findRegion(tooltipSprite.textureName))
        tooltipSprite.noClip = true

        container = VisTable()
        container.setFillParent(true)
        //      container.padLeft(10).padTop(10);
        //        container.set

        totalStatsTable = VisTable()
        totalStatsTable.top().left().pad(0f, 6f, 0f, 0f)
        totalStatsTable.setBackground("default-pane")

        val headerLabel = VisLabel("Electricity Resources")
        totalStatsTable.add(headerLabel).left()

        totalStatsTable.row()

        val demandLabel = VisLabel("Energy Demand:")
        totalStatsTable.add(demandLabel).left()

        circuitDemandLabel = VisLabel("-1")
        totalStatsTable.add<Label>(circuitDemandLabel)

        totalStatsTable.row()

        val supplyLabel = VisLabel("Energy Supply:")
        totalStatsTable.add(supplyLabel).left()

        circuitSupplyLabel = VisLabel("-1")
        totalStatsTable.add<Label>(circuitSupplyLabel)

        container.add<Table>(totalStatsTable).expand().bottom().right().size(400f, 100f)

        //        container.defaults().space(4);
        container.isVisible = false

        stage.addActor(container)
    }

    override fun dispose() {
        batch.dispose()
    }

    //todo sufficient until we get a spatial hash or whatever

    /**
     * Process the system.
     */
    override fun process(entityId: Int) {
        if (!overlayVisible) {
            return
        }

        //        batch.setProjectionMatrix(oreWorld.m_camera.combined);
        batch.projectionMatrix = oreWorld.camera.combined
        batch.begin()

       // renderEntities(this.getWorld().delta)

        batch.end()

        //screen space rendering
        batch.projectionMatrix = oreWorld.client!!.viewport.camera.combined
        batch.begin()

        //fixme replace this crap w/ scene2d stuff?
        oreWorld.client!!.bitmapFont_8pt.setColor(1f, 0f, 0f, 1f)

        var fontY = 150f
        val fontX = (oreWorld.client!!.viewport.rightGutterX - 220).toFloat()

        batch.draw(oreWorld.atlas.findRegion("backgroundRect"), fontX - 10, fontY + 10, fontX + 100,
                   fontY - 300)

        oreWorld.client!!.bitmapFont_8pt.draw(batch, "Energy overlay visible (press E)", fontX, fontY)
        fontY -= 15f

        oreWorld.client!!.bitmapFont_8pt.draw(batch, "Input: N/A Output: N/A", fontX, fontY)

        oreWorld.client!!.bitmapFont_8pt.setColor(1f, 1f, 1f, 1f)

        batch.end()

        updateCircuitStats()
    }

    private fun updateCircuitStats() {
        val mouse = oreWorld.mousePositionWorldCoords()

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

            circuitDemandLabel.setText(currentCircuitDemand.toString())
            circuitSupplyLabel.setText(currentCircuitSupply.toString())
        } else {
            //reset them both and update the labels
            if (currentCircuitDemand != -1) {
                currentCircuitDemand = -1
                currentCircuitSupply = -1

                circuitDemandLabel.setText(currentCircuitDemand.toString())
                circuitSupplyLabel.setText(currentCircuitSupply.toString())
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
        entityOverlaySystem.isEnabled = !overlayVisible
        entityOverlaySystem.setOverlaysVisible(!overlayVisible)

        if (overlayVisible) {
            container.toFront()
        }

        container.isVisible = overlayVisible
    }
}
