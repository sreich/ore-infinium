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

import com.artemis.ComponentMapper
import com.artemis.managers.TagManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.utils.viewport.StretchViewport
import com.esotericsoftware.minlog.Log
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.VisDialog
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.ore.infinium.components.*
import com.ore.infinium.systems.client.*
import com.ore.infinium.util.*
import java.io.IOException

class OreClient : OreApplicationListener, OreInputProcessor {

    var leftMouseDown: Boolean = false
    var rightMouseDown: Boolean = false

    lateinit var viewport: StretchViewport
    var world: OreWorld? = null

    // zoom every n ms, while zoom key is held down
    private val zoomInterval = 30L
    private val zoomTimer = OreTimer()

    private lateinit var mPlayer: ComponentMapper<PlayerComponent>
    private lateinit var mSprite: ComponentMapper<SpriteComponent>
    private lateinit var mControl: ComponentMapper<ControllableComponent>
    private lateinit var mItem: ComponentMapper<ItemComponent>
    private lateinit var mJump: ComponentMapper<JumpComponent>
    private lateinit var mBlock: ComponentMapper<BlockComponent>
    private lateinit var mTool: ComponentMapper<ToolComponent>
    private lateinit var mDevice: ComponentMapper<PowerDeviceComponent>
    private lateinit var mDoor: ComponentMapper<DoorComponent>

    private lateinit var clientNetworkSystem: ClientNetworkSystem
    private lateinit var tagManager: TagManager
    private lateinit var debugTextRenderSystem: DebugTextRenderSystem
    private lateinit var powerOverlayRenderSystem: PowerOverlayRenderSystem
    private lateinit var tileRenderSystem: TileRenderSystem
    private lateinit var soundSystem: SoundSystem

    lateinit private var multiplexer: InputMultiplexer

    lateinit var stage: Stage

    lateinit var rootTable: VisTable
    lateinit var chat: Chat
    private var sidebar: Sidebar? = null
    lateinit var hud: Hud

    private var dragAndDrop: DragAndDrop? = null

    private var dialog: VisDialog? = null
    private lateinit var chatDialog: ChatDialog

    private var hotbarView: HotbarInventoryView? = null
    private var inventoryView: InventoryView? = null
    private var debugProfilerView: DebugProfilerView? = null

    var hotbarInventory: HotbarInventory? = null
    var inventory: Inventory? = null

    var generatorControlPanelView: GeneratorControlPanelView? = null
    var generatorInventory: GeneratorInventory? = null

    var server: OreServer? = null
    private var serverThread: Thread? = null

    lateinit var bitmapFont_8pt: BitmapFont

    internal lateinit var fontGenerator: FreeTypeFontGenerator

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

        dragAndDrop = DragAndDrop()

        viewport = StretchViewport(OreSettings.width.toFloat(), OreSettings.height.toFloat())

        //load before stage
        VisUI.load(VisUI.SkinScale.X1)
        TooltipManager.getInstance().apply {
            initialTime = 0f
            hideAll()
        }

        //todo custom skin
        //VisUI.load(Gdx.files.internal("ui/ui.json"))

        stage = Stage(viewport)
        rootTable = VisTable()
        rootTable.setFillParent(true)
        stage.addActor(rootTable)

        multiplexer = InputMultiplexer(stage, this)

        Gdx.input.inputProcessor = multiplexer

        //fixme: this really needs to be stripped out of the client, put in a proper
        //system or something
        fontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 13
        bitmapFont_8pt = fontGenerator.generateFont(parameter)

        parameter.size = 9

        fontGenerator.dispose()

        chatDialog = ChatDialog(this, stage, rootTable)

        chat = Chat()
        chat.addListener(chatDialog)

        hud = Hud(this, stage, rootTable)

        sidebar = Sidebar(stage, this)

        hostAndJoin()
    }

    fun handlePrimaryAttack() {
        val mouseWorldCoords = world!!.mousePositionWorldCoords()

        val player = tagManager.getEntity(OreWorld.s_mainPlayer).id

        val cPlayer = mPlayer.get(player)
        val equippedItem = cPlayer.equippedPrimaryItem

        if (isInvalidEntity(equippedItem)) {
            return
        }

        val blockComp = mBlock.opt(equippedItem)

        if (blockComp != null) {

            val x = mouseWorldCoords.x.toInt()
            val y = mouseWorldCoords.y.toInt()

            val blockPlaced = world!!.attemptBlockPlacement(x, y, blockComp.blockType)
            if (blockPlaced) {
                clientNetworkSystem.sendBlockPlace(x, y)
                soundSystem.playDirtPlace()
            }

            return
        }

        val equippedItemComp = mItem.opt(equippedItem) ?: return

        val equippedToolComp = mTool.opt(equippedItem)
        if (equippedToolComp != null) {

            //note, digging is handled by its own system, not anywhere near here.
            attemptToolAttack(cPlayer, equippedToolComp, mouseWorldCoords)
            return
        }

        if (cPlayer.placeableItemTimer.resetIfSurpassed(PlayerComponent.placeableItemDelay)) {
            cPlayer.placeableItemTimer.reset()

            attemptItemPlace()
        }
    }

    fun handleSecondaryAttack() {
        val player = tagManager.getEntity(OreWorld.s_mainPlayer).id
        val cPlayer = mPlayer.get(player)

        if (cPlayer.secondaryActionTimer.resetIfSurpassed(PlayerComponent.secondaryActionDelay)) {
            //todo do we want right click to be activating stuff? toggling doors, opening up machinery control panels?
            //or do we want a separate key for that?
            val mouse = world!!.mousePositionWorldCoords()
            val entity = world!!.entityAtPosition(mouse) ?: return

            //is it a device and can we activate it?
            if (attemptActivateDeviceControlPanel(entity)) {
                return
            }

            if (attemptActivateDoor(entity)) {
                return
            }
        }
    }

    private fun attemptActivateDoor(entity: Int): Boolean {
        val cDoor = mDoor.get(entity) ?: return false

        //toggle state right now, tell server.
        cDoor.state = when (cDoor.state) {
            DoorComponent.DoorState.Closed -> DoorComponent.DoorState.Open
            DoorComponent.DoorState.Open -> DoorComponent.DoorState.Closed
        }

        clientNetworkSystem.sendDoorOpen(entity)

        return true
    }

    /**
     * attempts to open a device's control panel, if this even is a device
     */
    private fun attemptActivateDeviceControlPanel(entityId: Int): Boolean {
        //todo request from server populating control panel (with items within it)
        val deviceComp = mDevice.get(entityId) ?: return false

        if (generatorControlPanelView!!.visible) {
            if (generatorInventory!!.owningGeneratorEntityId == entityId) {
                //close it, he clicked on the same device
                generatorControlPanelView!!.closePanel()
            } else {
                //open on this entity instead, it's different
                generatorControlPanelView!!.openPanel(entityId)
            }
        } else {
            generatorControlPanelView!!.openPanel(entityId)
        }

        return true
    }

    private fun attemptToolAttack(cPlayer: PlayerComponent,
                                  equippedToolComp: ToolComponent,
                                  mouseWorldCoords: Vector2) {
        if (cPlayer.primaryAttackTimer.resetIfSurpassed(equippedToolComp.attackIntervalMs)) {
            //fixme obviously, iterating over every entityId to find the one under position is beyond dumb, use a spatial hash/quadtree etc

            when (equippedToolComp.type) {
                ToolComponent.ToolType.Bucket -> liquidGunAttackAndSend(mouseWorldCoords)
                ToolComponent.ToolType.Explosive -> explosiveAttackAndSend(mouseWorldCoords)

            //for attacking like trees and stuff. likely needs a much better system designed later on, as it evolves..
                else ->
                    attemptToolAttackOnAnEntityAndSend(mouseWorldCoords)
            }
        }
    }

    private fun explosiveAttackAndSend(mouseWorldCoords: Vector2) {
        clientNetworkSystem.sendEquippedItemAttack(
                _attackType = Network.Client.PlayerEquippedItemAttack.ItemAttackType.Primary,
                _attackPositionWorldCoords = mouseWorldCoords)
    }

    private fun liquidGunAttackAndSend(mouseWorldCoords: Vector2) {
        //todo play sound immediately, then send attack command

        clientNetworkSystem.sendEquippedItemAttack(
                _attackType = Network.Client.PlayerEquippedItemAttack.ItemAttackType.Primary,
                _attackPositionWorldCoords = mouseWorldCoords)
    }

    private fun attemptToolAttackOnAnEntityAndSend(mouse: Vector2) {
        val entities = world!!.artemisWorld.entities(allOf()).toMutableList()

        val entityToAttack = entities.filter { e ->
            val spriteComp = mSprite.get(e)
            //ignore players, we don't attack them
            !mPlayer.has(e) && !world!!.shouldIgnoreClientEntityTag(e) &&
                    spriteComp.sprite.rect.contains(mouse) && canAttackEntity(e)
        }.forEach { e ->
            clientNetworkSystem.sendEntityAttack(e)
        }
    }

    /**
     * @return true if the entityId is able to be attacked.
     *
     * e.g. items dropped in the world are not attackable
     */
    private fun canAttackEntity(entityId: Int): Boolean {
        mItem.ifPresent(entityId) { cItem ->
            if (cItem.state == ItemComponent.State.DroppedInWorld) {
                return false
            }
        }
        //don't let them attack dropped items, makes no sense

        return true
    }

    /**
     * Everything about the placed item (including placed position) is determined
     * by the placement overlay. then if valid to place, we then notify the server
     * and it clones the currently equipped item and places it at the position
     * we provided
     */
    private fun attemptItemPlace() {
        val placementOverlay = tagManager.getEntity(OreWorld.s_itemPlacementOverlay).id
        val placementOverlaySprite = mSprite.get(placementOverlay)

        val placeX = placementOverlaySprite.sprite.x
        val placeY = placementOverlaySprite.sprite.y

        if (world!!.isPlacementValid(placementOverlay)) {
            clientNetworkSystem.sendItemPlace(placeX, placeY)
            soundSystem.playItemPlace()
        }
    }

    fun toggleChatVisible() {
        if (chatDialog.chatVisibilityState == ChatDialog.ChatVisibility.Normal) {
            chatDialog.closeChatDialog()
        } else {
            chatDialog.openChatDialog()
        }
    }

    fun toggleInventoryVisible() {
        inventoryView!!.visible = !inventoryView!!.visible
    }

    /**
     * immediately hops into hosting and joining its own local server
     */
    private fun hostAndJoin() {
        server = OreServer()
        serverThread = Thread(server, "main server thread")
        serverThread!!.start()

        try {
            //wait for the local server thread to report that it is live and running, before we attempt
            // a connection to it
            server!!.connectHostLatch.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        //call system, if returns false, fail and show:
        world = OreWorld(this, server, OreWorld.WorldInstanceType.ClientHostingServer)
        world!!.init()
        world!!.artemisWorld.inject(this)

        clientNetworkSystem.addListener(NetworkConnectListener(this))

        try {
            clientNetworkSystem.connect("127.0.0.1", Network.PORT)
        } catch (e: IOException) {
            e.printStackTrace()
            //fuck. gonna have to show the fail to connect dialog.
            //could be a socket error..or anything, i guess
            System.exit(1)
        }

        //showFailToConnectDialog();
    }

    override fun dispose() {
        world?.shutdown()
    }

    override fun render() {
        if (world != null) {
            world!!.process()
        }

        if (OreSettings.debugRenderGui) {
            stage.act(Gdx.graphics.deltaTime.coerceAtLeast(1f / 30f))
            stage.draw()
        }

        //fixme: minus isn't working?? but plus(equals) is
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT_BRACKET)) {
            if (zoomTimer.resetIfSurpassed(zoomInterval)) {
                //zoom out
                zoom(1.0f + OreSettings.zoomAmount)
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            if (zoomTimer.resetIfSurpassed(zoomInterval)) {
                zoom(1.0f - OreSettings.zoomAmount)
            }
        }
    }

    private fun showFailToConnectDialog() {
        dialog = object : VisDialog("", "dialog") {
            override fun result(obj: Any?) {
                println("Chosen: " + obj!!)
            }

        }

        var dbutton = VisTextButton("Yes")
        dialog!!.button(dbutton, true)

        dbutton = VisTextButton("No")
        dialog!!.button(dbutton, false)
        dialog!!.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false)
        dialog!!.invalidateHierarchy()
        dialog!!.invalidate()
        dialog!!.layout()
        //stage.addActor(dialog);
        dialog!!.show(stage)

    }

    private fun shutdown() {
        //only have to shutdown the server if it's a client-hosted server
        if (server != null) {
            server!!.shutdownLatch.countDown()
            try {
                //merge the server thread over, it should already have
                //gotten the shutdown signal
                serverThread!!.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }

        Gdx.app.exit()
    }

    fun zoom(factor: Float) {
        if (world != null) {
            world!!.camera.zoom *= factor
        }
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun keyDown(keycode: Int): Boolean {
        if (chatDialog.chatVisibilityState == ChatDialog.ChatVisibility.Normal) {
            //chat is happening and consuming input
            return false
        }

        when (keycode) {
            Input.Keys.ESCAPE -> shutdown()
            Input.Keys.F6 -> {
                if (clientNetworkSystem.connected) {
                    OreSettings.profilerEnabled = !OreSettings.profilerEnabled
                    debugProfilerView!!.profilerVisible = !debugProfilerView!!.profilerVisible
                }
            }
            Input.Keys.F7 -> {
                tileRenderSystem.debugRenderTileLighting = !tileRenderSystem.debugRenderTileLighting
            }
            Input.Keys.F8 -> //fixme; this kind of stuff could be maybe put into a base interface which systems interested in input
                // could derive from. so we could just call this, and await the return...all of the debug things could be
                // handled
                //directly in there. but the question is, what to do for everything else.
                debugTextRenderSystem.renderDebugClient = !debugTextRenderSystem.renderDebugClient
            Input.Keys.F9 -> debugTextRenderSystem.renderDebugServer = !debugTextRenderSystem.renderDebugServer
            Input.Keys.F10 -> {
                tileRenderSystem.debugRenderTiles = !tileRenderSystem.debugRenderTiles
            }
            Input.Keys.F11 -> OreSettings.debugRenderGui = !OreSettings.debugRenderGui
            Input.Keys.F12 -> {
                debugTextRenderSystem.guiDebug = !debugTextRenderSystem.guiDebug
                stage.setDebugAll(debugTextRenderSystem.guiDebug)
            }
            Input.Keys.I -> if (inventoryView != null) {
                toggleInventoryVisible()
            }
        }

        //everything below here requires a world. it's terrible, i know...fixme
        if (world == null) {
            return false
        }

        if (!clientNetworkSystem.connected) {
            return false
        }

        val player = tagManager.getEntity(OreWorld.s_mainPlayer).id
        val cControl = mControl.get(player)

        when (keycode) {
            Input.Keys.Q -> attemptItemDrop()
            Input.Keys.E -> //power overlay
                powerOverlayRenderSystem.toggleOverlay()
            Input.Keys.NUM_1 -> hotbarInventory!!.selectSlot(0)
            Input.Keys.NUM_2 -> hotbarInventory!!.selectSlot(1)
            Input.Keys.NUM_3 -> hotbarInventory!!.selectSlot(2)
            Input.Keys.NUM_4 -> hotbarInventory!!.selectSlot(3)
            Input.Keys.NUM_5 -> hotbarInventory!!.selectSlot(4)
            Input.Keys.NUM_6 -> hotbarInventory!!.selectSlot(5)
            Input.Keys.NUM_7 -> hotbarInventory!!.selectSlot(6)
            Input.Keys.NUM_8 -> hotbarInventory!!.selectSlot(7)
        }

        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            cControl.desiredDirection.x = -1f
        }

        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            cControl.desiredDirection.x = 1f
        }

        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            cControl.desiredDirection.y = -1f
        }

        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            cControl.desiredDirection.y = 1f
        }

        if (keycode == Input.Keys.SPACE) {
            val cJump = mJump.get(player)
            cJump.shouldJump = true
        }

        return true
    }

    private fun attemptItemDrop() {
        val player = tagManager.getEntity(OreWorld.s_mainPlayer).id
        val cPlayer = mPlayer.get(player)

        val itemEntity = cPlayer.equippedPrimaryItem

        if (itemEntity == INVALID_ENTITY_ID) {
            return
        }

        val currentEquippedIndex = cPlayer.hotbarInventory!!.selectedSlot
        val dropItemRequestFromClient = Network.Client.InventoryDropItem(index = currentEquippedIndex.toByte(),
                                                                         inventoryType = Network.Shared.InventoryType.Hotbar)

        // decrement count, we assume it'll get spawned shortly when the server tells us to.
        // delete in-inventory entityId if necessary server assumes we already do so
        val cItem = mItem.get(itemEntity)
        if (cItem.stackSize > 1) {
            //decrement count, server has already done so. we assume here that it went through properly.
            cItem.stackSize -= 1
            hotbarInventory!!.setCount(currentEquippedIndex, cItem.stackSize)
        } else {
            //delete it, server knows/assumes we already did, since there are no more left. so server doesn't have to
            //send another useless packet back to the our client
            val item = cPlayer.hotbarInventory!!.takeItem(dropItemRequestFromClient.index.toInt())
            world!!.artemisWorld.delete(item)
        }

        clientNetworkSystem.clientKryo.sendTCP(dropItemRequestFromClient)
    }

    override fun keyUp(keycode: Int): Boolean {
        when {
            world == null ->
                return false

            !clientNetworkSystem.connected ->
                return false
        }

        val player = tagManager.getEntity(OreWorld.s_mainPlayer).id

        val cControl = mControl.get(player)

        if (keycode == Input.Keys.LEFT || keycode == Input.Keys.A) {
            cControl.desiredDirection.x = 0f
        }

        if (keycode == Input.Keys.RIGHT || keycode == Input.Keys.D) {
            cControl.desiredDirection.x = 0f
        }

        if (keycode == Input.Keys.UP || keycode == Input.Keys.W) {
            cControl.desiredDirection.y = 0f
        }

        if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S) {
            cControl.desiredDirection.y = 0f
        }

        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.LEFT) {
            leftMouseDown = true
        }

        if (button == Input.Buttons.RIGHT) {
            rightMouseDown = true
        }

        if (world != null) {
            return world!!.touchDown(screenX, screenY, pointer, button)
            //fixme

        }

        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.LEFT) {
            leftMouseDown = false
        }

        if (button == Input.Buttons.RIGHT) {
            rightMouseDown = false
        }

        if (world != null) {
            return world!!.touchUp(screenX, screenY, pointer, button)
        }

        return false
    }

    override fun scrolled(amount: Int): Boolean {
        when {
            world == null ->
                return false

            !clientNetworkSystem.connected ->
                return false
            powerOverlayRenderSystem.overlayVisible ->
                //don't allow item/inventory selection during this
                return false
        }

        val index = hotbarInventory!!.selectedSlot
        if (amount > 0) {
            //right, inventory selection scrolling does not wrap around.
            hotbarInventory!!.selectSlot((index + 1).coerceAtMost(Inventory.maxHotbarSlots - 1))
        } else {
            //left
            hotbarInventory!!.selectSlot((index - 1).coerceAtLeast(0))
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
        val player = world!!.createPlayer(playerName, connectionId)
        val cControl = mControl.create(player)

        //only do this for the main player! each other player that gets spawned will not need this information, ever.
        val cPlayer = mPlayer.get(player)

        hotbarInventory = HotbarInventory(slotCount = Inventory.maxHotbarSlots)
        cPlayer.hotbarInventory = hotbarInventory

        hotbarInventory!!.addListener(HotbarSlotListener())

        inventory = Inventory(slotCount = Inventory.maxSlots)
        cPlayer.inventory = inventory

        hotbarView = HotbarInventoryView(stage = stage, inventory = hotbarInventory!!,
                                         dragAndDrop = dragAndDrop!!, world = world!!)

        inventoryView = InventoryView(stage = stage, inventory = inventory!!,
                                      dragAndDrop = dragAndDrop!!, world = world!!)

        generatorInventory = GeneratorInventory(GeneratorInventory.MAX_SLOTS)
        generatorControlPanelView = GeneratorControlPanelView(stage = stage,
                                                              generatorControlPanelInventory = generatorInventory!!,
                                                              dragAndDrop = dragAndDrop!!,
                                                              world = world!!)

        debugProfilerView = DebugProfilerView(stage = stage, world = world!!)

        world!!.artemisWorld.oreInject(hotbarInventory!!)
        world!!.artemisWorld.oreInject(inventory!!)
        world!!.artemisWorld.oreInject(generatorInventory!!)

        if (mainPlayer) {
            tagManager.register(OreWorld.s_mainPlayer, player)
        }

        //fixme push into the model and have view pull. or just in view init
        //select the first slot, so the inventory view highlights something
        cPlayer.hotbarInventory!!.selectSlot(0)

        //          SpriteComponent cSprite = mSprite.get(player);
        //        cSprite.sprite.setTexture();

        return player
    }

    private inner class HotbarSlotListener : Inventory.SlotListener {
        override fun slotItemSelected(index: Int, inventory: Inventory) {
            assert(world != null)

            val player = tagManager.getEntity(OreWorld.s_mainPlayer).id

            clientNetworkSystem.sendHotbarEquipped(index.toByte())

            val cPlayer = mPlayer.get(player)
        }
    }

    private class NetworkConnectListener(private val client: OreClient) : ClientNetworkSystem.NetworkClientListener {

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
