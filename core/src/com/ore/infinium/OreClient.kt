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

package com.ore.infinium

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.managers.TagManager
import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.viewport.StretchViewport
import com.esotericsoftware.minlog.Log
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisDialog
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.ore.infinium.components.*
import com.ore.infinium.systems.client.*
import com.ore.infinium.util.forEach
import com.ore.infinium.util.opt
import com.ore.infinium.util.rect
import java.io.IOException

class OreClient : ApplicationListener, InputProcessor {

    var m_leftMouseDown: Boolean = false
    var m_rightMouseDown: Boolean = false

    lateinit var viewport: StretchViewport
    var m_world: OreWorld? = null

    // zoom every n ms, while zoom key is held down
    private val zoomInterval = 30
    private val m_zoomTimer = OreTimer()

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>
    private lateinit var deviceMapper: ComponentMapper<PowerDeviceComponent>

    private lateinit var m_clientNetworkSystem: ClientNetworkSystem
    private lateinit var m_tagManager: TagManager
    private lateinit var m_debugTextRenderSystem: DebugTextRenderSystem
    private lateinit var m_powerOverlayRenderSystem: PowerOverlayRenderSystem
    private lateinit var m_tileRenderSystem: TileRenderSystem
    private lateinit var m_clientBlockDiggingSystem: ClientBlockDiggingSystem
    private lateinit var m_soundSystem: SoundSystem

    lateinit private var m_multiplexer: InputMultiplexer

    lateinit var m_stage: Stage

    lateinit var m_rootTable: VisTable
    lateinit var m_chat: Chat
    private var m_sidebar: Sidebar? = null

    private var m_dragAndDrop: DragAndDrop? = null

    private var m_dialog: VisDialog? = null
    private lateinit var m_chatDialog: ChatDialog

    private var m_hotbarView: HotbarInventoryView? = null
    private var m_inventoryView: InventoryView? = null
    private var m_debugProfilerView: DebugProfilerView? = null

    var m_hotbarInventory: HotbarInventory? = null
    private var m_inventory: Inventory? = null

    private var m_generatorControlPanelView: GeneratorControlPanelView? = null
    private var m_generatorInventory: GeneratorInventory? = null

    var m_server: OreServer? = null
    private var m_serverThread: Thread? = null

    var m_renderGui = true

    lateinit var bitmapFont_8pt: BitmapFont

    internal lateinit var m_fontGenerator: FreeTypeFontGenerator

    init {
    }

    override fun create() {
        // for debugging kryonet

        if (OreSettings.networkLog) {
            Log.set(Log.LEVEL_DEBUG)
        }

        //        Gdx.app.setLogLevel(Application.LOG_NONE);
        //        Gdx.app.setLogLevel(Application.LOG_NONE);
        //        Log.set(Log.LEVEL_INF

        Thread.currentThread().name = "client render thread (GL)"

        m_dragAndDrop = DragAndDrop()

        viewport = StretchViewport(OreSettings.width.toFloat(), OreSettings.height.toFloat())

        //load before stage
        VisUI.load(VisUI.SkinScale.X1)
        TooltipManager.getInstance().apply {
            initialTime = 0f
            hideAll()
        }

        //todo custom skin
        //VisUI.load(Gdx.files.internal("ui/ui.json"))

        m_stage = Stage(viewport)
        m_rootTable = VisTable()
        m_rootTable.setFillParent(true)
        m_stage.addActor(m_rootTable)

        m_multiplexer = InputMultiplexer(m_stage, this)

        Gdx.input.inputProcessor = m_multiplexer

        //fixme: this really needs to be stripped out of the client, put in a proper
        //system or something
        m_fontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 13
        bitmapFont_8pt = m_fontGenerator.generateFont(parameter)

        parameter.size = 9

        m_fontGenerator.dispose()

        m_chatDialog = ChatDialog(this, m_stage, m_rootTable)

        m_chat = Chat()
        m_chat.addListener(m_chatDialog)

        m_sidebar = Sidebar(m_stage, this)

        hostAndJoin()
    }

    fun handlePrimaryAttack() {
        val mouseWorldCoords = m_world!!.mousePositionWorldCoords()

        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id

        val playerComp = playerMapper.get(player)
        val equippedItem = playerComp.equippedPrimaryItem ?: return

        val blockComp = blockMapper.opt(equippedItem)

        if (blockComp != null) {

            val x = mouseWorldCoords.x.toInt()
            val y = mouseWorldCoords.y.toInt()

            val blockPlaced = m_world!!.attemptBlockPlacement(x, y, blockComp.blockType)
            if (blockPlaced) {
                m_clientNetworkSystem.sendBlockPlace(x, y)
                m_soundSystem.playDirtPlace()
            }

            return
        }

        val equippedItemComp = itemMapper.opt(equippedItem) ?: return

        val equippedToolComp = toolMapper.opt(equippedItem)
        if (equippedToolComp != null) {

            //note, digging is handled by its own system, not anywhere near here.
            attemptToolAttack(playerComp, equippedToolComp, mouseWorldCoords)
            return
        }

        if (playerComp.placeableItemTimer.milliseconds() > PlayerComponent.placeableItemDelay) {
            playerComp.placeableItemTimer.reset()

            attemptItemPlace()
        }
    }

    fun handleSecondaryAttack() {
        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id
        val playerComp = playerMapper.get(player)

        if (playerComp.secondaryActionTimer.milliseconds() > PlayerComponent.secondaryActionnDelay) {
            playerComp.secondaryActionTimer.reset()

            //todo do we want right click to be activating stuff? toggling doors, opening up machinery control panels?
            //or do we want a separate key for that?
            val mouse = m_world!!.mousePositionWorldCoords()
            val entity = m_world!!.entityAtPosition(mouse) ?: return

            attemptActivateDeviceControlPanel(entity)
        }
    }

    /**
     * attempts to open a device's control panel, if this even is a device
     */
    private fun attemptActivateDeviceControlPanel(entityId: Int): Boolean {
        //todo request from server populating control panel (with items within it)
        val deviceComp = deviceMapper.get(entityId) ?: return false

        if (m_generatorControlPanelView!!.visible) {
            m_generatorControlPanelView!!.closePanel()
        } else {
            m_generatorControlPanelView!!.openPanel(entityId)
        }

        return true
    }

    private fun attemptToolAttack(playerComp: PlayerComponent,
                                  equippedToolComp: ToolComponent,
                                  mouseWorldCoords: Vector2) {

        val currentMillis = TimeUtils.millis()
        if (currentMillis - playerComp.attackLastTick > equippedToolComp.attackTickIntervalMs) {
            //fixme obviously, iterating over every entityId to find the one under position is beyond dumb, use a spatial hash/quadtree etc

            playerComp.attackLastTick = currentMillis

            when (equippedToolComp.type) {
                ToolComponent.ToolType.Bucket -> liquidGunAttackAndSend(mouseWorldCoords)

            //for attacking like trees and stuff. likely needs a much better system designed later on, as it evolves..
                else ->
                    attemptToolAttackOnAnEntityAndSend(mouseWorldCoords)
            }
        }
    }

    private fun liquidGunAttackAndSend(mouseWorldCoords: Vector2) {
        //todo play sound immediately, then send attack command

        m_clientNetworkSystem.sendEquippedItemAttack(
                _attackType = Network.Client.PlayerEquippedItemAttack.ItemAttackType.Primary,
                _attackPositionWorldCoords = mouseWorldCoords)
    }

    private fun attemptToolAttackOnAnEntityAndSend(mouse: Vector2) {
        val clientAspectSubscriptionManager = m_world!!.m_artemisWorld.aspectSubscriptionManager
        val clientEntities = clientAspectSubscriptionManager.get(Aspect.all()).entities

        //fixme, not ideal..but i don't like non-java collections anyways
        val entities = mutableListOf<Int>()
        clientEntities.forEach { entities.add(it) }

        val entityToAttack = entities.filter { e ->
            val spriteComp = spriteMapper.get(e)
            //ignore players, we don't attack them
            !playerMapper.has(e) && !m_world!!.shouldIgnoreClientEntityTag(e) &&
                    spriteComp.sprite.rect.contains(mouse) && canAttackEntity(e)
        }.forEach { e ->
            m_clientNetworkSystem.sendEntityAttack(e)
        }
    }

    /**
     * @return true if the entityId is able to be attacked.
     *
     * e.g. items dropped in the world are not attackable
     */
    private fun canAttackEntity(entityId: Int): Boolean {
        val itemComp = itemMapper.opt(entityId) ?: return false
        //don't let them attack dropped items, makes no sense
        if (itemComp.state == ItemComponent.State.DroppedInWorld) {
            return false
        }

        return true
    }

    /**
     * Everything about the placed item (including placed position) is determined
     * by the placement overlay. then if valid to place, we then notify the server
     * and it clones the currently equipped item and places it at the position
     * we provided
     */
    private fun attemptItemPlace() {
        val placementOverlay = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay).id
        val placementOverlaySprite = spriteMapper.get(placementOverlay)

        val placeX = placementOverlaySprite.sprite.x
        val placeY = placementOverlaySprite.sprite.y

        if (m_world!!.isPlacementValid(placementOverlay)) {
            m_clientNetworkSystem.sendItemPlace(placeX, placeY)
            m_soundSystem.playItemPlace()
        }
    }

    fun toggleChatVisible() {
        if (m_chatDialog.chatVisibilityState == ChatDialog.ChatVisibility.Normal) {
            m_chatDialog.closeChatDialog()
        } else {
            m_chatDialog.openChatDialog()
        }
    }

    fun toggleInventoryVisible() {
        m_inventoryView!!.inventoryVisible = !m_inventoryView!!.inventoryVisible
    }

    /**
     * immediately hops into hosting and joining its own local server
     */
    private fun hostAndJoin() {
        m_server = OreServer()
        m_serverThread = Thread(m_server, "main server thread")
        m_serverThread!!.start()

        try {
            //wait for the local server thread to report that it is live and running, before we attempt
            // a connection to it
            m_server!!.connectHostLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //call system, if returns false, fail and show:
        m_world = OreWorld(this, m_server, OreWorld.WorldInstanceType.ClientHostingServer)
        m_world!!.init()
        m_world!!.m_artemisWorld.inject(this)

        m_clientNetworkSystem.addListener(NetworkConnectListener(this))

        try {
            m_clientNetworkSystem.connect("127.0.0.1", Network.PORT)
        } catch (e: IOException) {
            e.printStackTrace()
            //fuck. gonna have to show the fail to connect dialog.
            //could be a socket error..or anything, i guess
            System.exit(1)
        }

        //showFailToConnectDialog();
    }

    override fun dispose() {
        m_world?.shutdown()
    }

    override fun render() {
        if (m_world != null) {
            m_world!!.process()
        }

        if (m_renderGui) {
            m_stage.act(Gdx.graphics.deltaTime.coerceAtLeast(1f / 30f))
            m_stage.draw()
        }

        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            if (m_zoomTimer.milliseconds() >= zoomInterval) {
                //zoom out
                zoom(1.0f + OreSettings.zoomAmount)
                m_zoomTimer.reset()
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            if (m_zoomTimer.milliseconds() >= zoomInterval) {
                zoom(1.0f - OreSettings.zoomAmount)
                m_zoomTimer.reset()
            }
        }
    }

    private fun showFailToConnectDialog() {
        m_dialog = object : VisDialog("", "dialog") {
            override fun result(obj: Any?) {
                println("Chosen: " + obj!!)
            }

        }

        var dbutton = VisTextButton("Yes")
        m_dialog!!.button(dbutton, true)

        dbutton = VisTextButton("No")
        m_dialog!!.button(dbutton, false)
        m_dialog!!.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false)
        m_dialog!!.invalidateHierarchy()
        m_dialog!!.invalidate()
        m_dialog!!.layout()
        //m_stage.addActor(dialog);
        m_dialog!!.show(m_stage)

    }

    private fun shutdown() {
        //only have to shutdown the server if it's a client-hosted server
        if (m_server != null) {
            m_server!!.shutdownLatch.countDown()
            try {
                //merge the server thread over, it should already have
                //gotten the shutdown signal
                m_serverThread!!.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }

        Gdx.app.exit()
    }

    fun zoom(factor: Float) {
        if (m_world != null) {
            m_world!!.m_camera.zoom *= factor
        }
    }

    override fun resize(width: Int, height: Int) {
        m_stage.viewport.update(width, height, true)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.ESCAPE -> shutdown()
            Input.Keys.F6 -> {
                if (m_clientNetworkSystem.connected) {
                    OreSettings.profilerEnabled = !OreSettings.profilerEnabled
                    m_debugProfilerView!!.profilerVisible = !m_debugProfilerView!!.profilerVisible
                }
            }
            Input.Keys.F7 -> {
                m_tileRenderSystem.debugRenderTileLighting = !m_tileRenderSystem.debugRenderTileLighting
            }
            Input.Keys.F8 -> //fixme; this kind of stuff could be maybe put into a base interface which systems interested in input
                // could derive from. so we could just call this, and await the return...all of the debug things could be
                // handled
                //directly in there. but the question is, what to do for everything else.
                m_debugTextRenderSystem.renderDebugClient = !m_debugTextRenderSystem.renderDebugClient
            Input.Keys.F9 -> m_debugTextRenderSystem.renderDebugServer = !m_debugTextRenderSystem.renderDebugServer
            Input.Keys.F10 -> {
                m_tileRenderSystem.debugRenderTiles = !m_tileRenderSystem.debugRenderTiles
            }
            Input.Keys.F11 -> m_renderGui = !m_renderGui
            Input.Keys.F12 -> {
                m_debugTextRenderSystem.guiDebug = !m_debugTextRenderSystem.guiDebug
                m_stage.setDebugAll(m_debugTextRenderSystem.guiDebug)
            }
            Input.Keys.I -> if (m_inventoryView != null) {
                toggleInventoryVisible()
            }
        }

        //everything below here requires a world. it's terrible, i know...fixme
        if (m_world == null) {
            return false
        }

        if (!m_clientNetworkSystem.connected) {
            return false
        }

        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id
        val controllableComponent = controlMapper.get(player)

        when (keycode) {
            Input.Keys.Q -> attemptItemDrop()
            Input.Keys.E -> //power overlay
                m_powerOverlayRenderSystem.toggleOverlay()
            Input.Keys.NUM_1 -> m_hotbarInventory!!.selectSlot(0)
            Input.Keys.NUM_2 -> m_hotbarInventory!!.selectSlot(1)
            Input.Keys.NUM_3 -> m_hotbarInventory!!.selectSlot(2)
            Input.Keys.NUM_4 -> m_hotbarInventory!!.selectSlot(3)
            Input.Keys.NUM_5 -> m_hotbarInventory!!.selectSlot(4)
            Input.Keys.NUM_6 -> m_hotbarInventory!!.selectSlot(5)
            Input.Keys.NUM_7 -> m_hotbarInventory!!.selectSlot(6)
            Input.Keys.NUM_8 -> m_hotbarInventory!!.selectSlot(7)
        }

        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            controllableComponent.desiredDirection.x = -1f
        }

        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            controllableComponent.desiredDirection.x = 1f
        }

        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            controllableComponent.desiredDirection.y = -1f
        }

        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            controllableComponent.desiredDirection.y = 1f
        }

        if (keycode == Input.Keys.SPACE) {
            val jumpComponent = jumpMapper.get(player)
            jumpComponent.shouldJump = true
        }

        return true
    }

    private fun attemptItemDrop() {
        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id
        val playerComponent = playerMapper.get(player)

        val itemEntity = playerComponent.equippedPrimaryItem
        val currentEquippedIndex = playerComponent.hotbarInventory!!.selectedSlot

        val dropItemRequestFromClient = Network.Client.InventoryDropItem(index = currentEquippedIndex.toByte(),
                inventoryType = Network.Shared.InventoryType.Hotbar)

        // decrement count, we assume it'll get spawned shortly when the server tells us to.
        // delete in-inventory entityId if necessary server assumes we already do so
        val itemComponent = itemMapper.get(itemEntity!!)
        if (itemComponent.stackSize > 1) {
            //decrement count, server has already done so. we assume here that it went through properly.
            itemComponent.stackSize -= 1
            m_hotbarInventory!!.setCount(currentEquippedIndex, itemComponent.stackSize)
        } else {
            //delete it, server knows/assumes we already did, since there are no more left. so server doesn't have to
            //send another useless packet back to the our client
            val item = playerComponent.hotbarInventory!!.takeItem(dropItemRequestFromClient.index.toInt())!!
            m_world!!.m_artemisWorld.delete(item)
        }

        m_clientNetworkSystem.clientKryo.sendTCP(dropItemRequestFromClient)
    }

    override fun keyUp(keycode: Int): Boolean {
        when {
            m_world == null ->
                return false

            !m_clientNetworkSystem.connected ->
                return false
        }

        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id

        val controllableComponent = controlMapper.get(player)

        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            controllableComponent.desiredDirection.x = 0f
        }

        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            controllableComponent.desiredDirection.x = 0f
        }

        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            controllableComponent.desiredDirection.y = 0f
        }

        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            controllableComponent.desiredDirection.y = 0f
        }

        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.LEFT) {
            m_leftMouseDown = true
        }

        if (button == Input.Buttons.RIGHT) {
            m_rightMouseDown = true
        }

        if (m_world != null) {
            return m_world!!.touchDown(screenX, screenY, pointer, button)
            //fixme

        }

        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.LEFT) {
            m_leftMouseDown = false
        }

        if (button == Input.Buttons.RIGHT) {
            m_rightMouseDown = false
        }

        if (m_world != null) {
            return m_world!!.touchUp(screenX, screenY, pointer, button)
        }

        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amount: Int): Boolean {
        when {
            m_world == null ->
                return false

            !m_clientNetworkSystem.connected ->
                return false
            m_powerOverlayRenderSystem.overlayVisible ->
                //don't allow item/inventory selection during this
                return false
        }

        val index = m_hotbarInventory!!.selectedSlot
        if (amount > 0) {
            //right, inventory selection scrolling does not wrap around.
            m_hotbarInventory!!.selectSlot((index + 1).coerceAtMost(Inventory.maxHotbarSlots - 1))
        } else {
            //left
            m_hotbarInventory!!.selectSlot((index - 1).coerceAtLeast(0))
        }

        return true
    }

    /**
     * @param playerName
     * *
     * @param connectionId
     * *
     * @param mainPlayer
     * *         true if we should spawn our clients player (first player we get)
     * *
     * *
     * @return
     */
    fun createPlayer(playerName: String, connectionId: Int, mainPlayer: Boolean): Int {
        val player = m_world!!.createPlayer(playerName, connectionId)
        val controllableComponent = controlMapper.create(player)

        //only do this for the main player! each other player that gets spawned will not need this information, ever.
        val playerComponent = playerMapper.get(player)

        m_hotbarInventory = HotbarInventory(Inventory.maxHotbarSlots)
        playerComponent.hotbarInventory = m_hotbarInventory

        m_hotbarInventory!!.addListener(HotbarSlotListener())

        m_inventory = Inventory(Inventory.maxSlots)
        playerComponent.inventory = m_inventory

        m_hotbarView = HotbarInventoryView(m_stage, m_hotbarInventory!!, m_inventory!!, m_dragAndDrop!!,
                                           m_world!!)
        m_inventoryView = InventoryView(m_stage, m_hotbarInventory!!, m_inventory!!, m_dragAndDrop!!, m_world!!)

        m_generatorInventory = GeneratorInventory(GeneratorInventory.MAX_SLOTS)
        m_generatorControlPanelView = GeneratorControlPanelView(stage = m_stage,
                                                                generatorControlPanelInventory = m_generatorInventory!!,
                                                                playerInventory = m_inventory!!,
                                                                hotbarInventory = m_hotbarInventory!!,
                                                                dragAndDrop = m_dragAndDrop!!,
                                                                world = m_world!!)

        m_debugProfilerView = DebugProfilerView(stage = m_stage, m_world = m_world!!)

        m_world!!.m_artemisWorld.inject(m_hotbarInventory, true)
        m_world!!.m_artemisWorld.inject(m_inventory, true)
        m_world!!.m_artemisWorld.inject(m_inventoryView, true)
        m_world!!.m_artemisWorld.inject(m_hotbarView, true)
        m_world!!.m_artemisWorld.inject(m_generatorControlPanelView, true)
        m_world!!.m_artemisWorld.inject(m_generatorInventory, true)

        if (mainPlayer) {
            m_tagManager.register(OreWorld.s_mainPlayer, player)
        }

        //select the first slot, so the inventory view highlights something.
        playerComponent.hotbarInventory!!.selectSlot(0)

        //          SpriteComponent spriteComponent = spriteMapper.get(player);
        //        spriteComponent.sprite.setTexture();

        return player
    }

    private inner class HotbarSlotListener : Inventory.SlotListener {
        override fun slotItemSelected(index: Int, inventory: Inventory) {
            assert(m_world != null)

            val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id

            m_clientNetworkSystem.sendHotbarEquipped(index.toByte())

            val playerComponent = playerMapper.get(player)
        }

        override fun slotItemCountChanged(index: Int, inventory: Inventory) {

        }

        override fun slotItemChanged(index: Int, inventory: Inventory) {

        }

        override fun slotItemRemoved(index: Int, inventory: Inventory) {

        }
    }

    private class NetworkConnectListener(private val m_client: OreClient) : ClientNetworkSystem.NetworkClientListener {

        override fun connected() {
            //todo surely there's some first-time connection stuff we must do?
        }

        override fun disconnected(disconnectReason: Network.Shared.DisconnectReason) {
            //todo show gui, say we've disconnected
        }

    }

    companion object {
        val ORE_VERSION_MAJOR = 0
        val ORE_VERSION_MINOR = 1
        val ORE_VERSION_REVISION = 1
    }

}
