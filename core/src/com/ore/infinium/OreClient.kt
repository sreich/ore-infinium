package com.ore.infinium

import com.artemis.ComponentMapper
import com.artemis.managers.TagManager
import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.viewport.StretchViewport
import com.esotericsoftware.minlog.Log
import com.ore.infinium.components.*
import com.ore.infinium.systems.ClientBlockDiggingSystem
import com.ore.infinium.systems.DebugTextRenderSystem
import com.ore.infinium.systems.NetworkClientSystem
import com.ore.infinium.systems.PowerOverlayRenderSystem
import java.io.IOException

class OreClient : ApplicationListener, InputProcessor {

    var leftMouseDown: Boolean = false
    lateinit var viewport: StretchViewport
    protected var m_world: OreWorld? = null

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

    private lateinit var m_networkClientSystem: NetworkClientSystem
    private lateinit var m_tagManager: TagManager
    private lateinit var m_debugTextRenderSystem: DebugTextRenderSystem
    private lateinit var m_powerOverlayRenderSystem: PowerOverlayRenderSystem
    private lateinit var m_clientBlockDiggingSystem: ClientBlockDiggingSystem

    lateinit private var m_multiplexer: InputMultiplexer

    lateinit var stage: Stage
    lateinit var skin: Skin

    var m_chat: Chat? = null
    private var m_sidebar: Sidebar? = null

    private var m_dragAndDrop: DragAndDrop? = null

    private var dialog: Dialog? = null
    private var m_chatDialog: ChatDialog? = null
    private var m_hotbarView: HotbarInventoryView? = null
    private var m_inventoryView: InventoryView? = null

    var m_hotbarInventory: Inventory? = null
    private var m_inventory: Inventory? = null

    private val m_viewport: ScreenViewport? = null

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

        //        ProgressBar progressBar = new ProgressBar(0, 100, 10, false, m_skin);
        //        progressBar.setValue(50);
        //        progressBar.getStyle().knobBefore = progressBar.getStyle().knob;
        //        progressBar.getStyle().knob.setMinHeight(50);
        //        container.add(progressBar);

        Thread.currentThread().name = "client render thread (GL)"

        m_dragAndDrop = DragAndDrop()

        viewport = StretchViewport(OreSettings.width.toFloat(), OreSettings.height.toFloat())
        stage = Stage(viewport)
        m_multiplexer = InputMultiplexer(stage, this)

        Gdx.input.inputProcessor = m_multiplexer

        //fixme: this really needs to be stripped out of the client, put in a proper
        //system or something
        m_fontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Ubuntu-L.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 13
        bitmapFont_8pt = m_fontGenerator.generateFont(parameter)

        parameter.size = 9

        m_fontGenerator.dispose()

        skin = Skin()
        skin.addRegions(TextureAtlas(Gdx.files.internal("packed/ui.atlas")))
        skin.add("myfont", bitmapFont_8pt, BitmapFont::class.java)
        skin.load(Gdx.files.internal("ui/ui.json"))

        m_chatDialog = ChatDialog(this, stage, skin)
        m_chat = Chat()
        m_chat!!.addListener(m_chatDialog!!)

        m_sidebar = Sidebar(stage, skin, this)

        hostAndJoin()
    }

    fun handleLeftMousePrimaryAttack() {
        val mouse = m_world!!.mousePositionWorldCoords()

        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id

        val playerComponent = playerMapper.get(player)
        val itemEntity = playerComponent.equippedPrimaryItem
        if (itemEntity == OreWorld.ENTITY_INVALID) {
            return
        }

        val blockComponent = blockMapper.getSafe(itemEntity)
        if (blockComponent != null) {

            val x = mouse.x.toInt()
            val y = mouse.y.toInt()

            val blockPlaced = m_world!!.attemptBlockPlacement(x, y, blockComponent.blockType)
            if (blockPlaced) {
                m_networkClientSystem.sendBlockPlace(x, y)
            }

            return
        }

        val itemComponent = itemMapper.getSafe(itemEntity)
        if (itemComponent != null) {
            //ignore tools and such, can't place those
            if (toolMapper.has(itemEntity)) {
                return
            }

            if (playerComponent.placeableItemTimer.milliseconds() > PlayerComponent.placeableItemDelay) {
                playerComponent.placeableItemTimer.reset()

                attemptItemPlace(playerComponent.equippedPrimaryItem)
            }
        }
    }

    /**
     * Placement position is determined by the current position of the overlay

     * @param itemEntity
     */
    private fun attemptItemPlace(itemEntity: Int) {

        //place the item
        val placedItemEntity = m_world!!.cloneEntity(itemEntity)

        val placedItemComponent = itemMapper.get(placedItemEntity)

        placedItemComponent.state = ItemComponent.State.InWorldState

        val spriteComponent = spriteMapper.get(placedItemEntity)

        val placementOverlay = m_tagManager.getEntity(OreWorld.s_itemPlacementOverlay).id
        val placementOverlaySprite = spriteMapper.get(placementOverlay)

        val placeX = placementOverlaySprite.sprite.x
        val placeY = placementOverlaySprite.sprite.y
        spriteComponent.sprite.setPosition(placeX, placeY)

        if (m_world!!.isPlacementValid(placedItemEntity)) {
            //todo, do more validation..
            m_networkClientSystem.sendItemPlace(placeX, placeY)
        } else {
            //fixme i know, it isn't ideal..i technically add the item anyways and delete it if it cannot be placed
            //because the function actually takes only the entity, to check if its size, position etc conflict with
            // anything

            //engine.removeEntity(placedItemEntity);
        }
    }

    fun toggleChatVisible() {
        if (m_chatDialog!!.chatVisibilityState == ChatDialog.ChatVisibility.Normal) {
            m_chatDialog!!.closeChatDialog()
        } else {
            m_chatDialog!!.openChatDialog()
        }
    }

    fun toggleInventoryVisible() {
        m_inventoryView!!.setVisible(!m_inventoryView!!.inventoryVisible)
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

        m_networkClientSystem.addListener(NetworkConnectListener(this))

        try {
            m_networkClientSystem.connect("127.0.0.1", Network.PORT)
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
            stage.act(Math.min(Gdx.graphics.deltaTime, 1 / 30f))
            stage.draw()
        }

        val zoomAmount = 0.004f
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            if (m_zoomTimer.milliseconds() >= zoomInterval) {
                //zoom out
                zoom(1.0f + zoomAmount)
                m_zoomTimer.reset()
            }
        }

        if (Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            if (m_zoomTimer.milliseconds() >= zoomInterval) {
                zoom(1.0f - zoomAmount)
                m_zoomTimer.reset()
            }
        }
    }

    private fun showFailToConnectDialog() {
        dialog = object : Dialog("", skin, "dialog") {
            override fun result(`object`: Any?) {
                println("Chosen: " + `object`!!)
            }

        }

        var dbutton = TextButton("Yes", skin, "default")
        dialog!!.button(dbutton, true)

        dbutton = TextButton("No", skin, "default")
        dialog!!.button(dbutton, false)
        dialog!!.key(Input.Keys.ENTER, true).key(Input.Keys.ESCAPE, false)
        dialog!!.invalidateHierarchy()
        dialog!!.invalidate()
        dialog!!.layout()
        //m_stage.addActor(dialog);
        dialog!!.show(stage)

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
        stage.viewport.update(width, height, true)
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.ESCAPE) {
            shutdown()
        } else if (keycode == Input.Keys.F7) {
        } else if (keycode == Input.Keys.F8) {
            //fixme; this kind of stuff could be maybe put into a base interface which systems interested in input
            // could derive from. so we could just call this, and await the return...all of the debug things could be
            // handled
            //directly in there. but the question is, what to do for everything else.
            m_debugTextRenderSystem.m_renderDebugClient = !m_debugTextRenderSystem.m_renderDebugClient
        } else if (keycode == Input.Keys.F9) {
            m_debugTextRenderSystem.m_renderDebugServer = !m_debugTextRenderSystem.m_renderDebugServer
        } else if (keycode == Input.Keys.F10) {
            m_debugTextRenderSystem.m_renderTiles = !m_debugTextRenderSystem.m_renderTiles
        } else if (keycode == Input.Keys.F11) {
            m_renderGui = !m_renderGui
        } else if (keycode == Input.Keys.F12) {
            m_debugTextRenderSystem.m_guiDebug = !m_debugTextRenderSystem.m_guiDebug
            stage.setDebugAll(m_debugTextRenderSystem.m_guiDebug)
        } else if (keycode == Input.Keys.I) {
            if (m_inventoryView != null) {
                m_inventoryView!!.setVisible(!m_inventoryView!!.inventoryVisible)
            }
        }

        //everything below here requires a world. it's terrible, i know...fixme
        if (m_world == null) {
            return false
        }

        if (!m_networkClientSystem.connected) {
            return false
        }

        val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id
        val controllableComponent = controlMapper.get(player)

        if (keycode == Input.Keys.Q) {
            attemptItemDrop()
        } else if (keycode == Input.Keys.E) {
            //power overlay
            m_powerOverlayRenderSystem.toggleOverlay()
        } else if (keycode == Input.Keys.NUM_1) {
            m_hotbarInventory!!.selectSlot(0)
        } else if (keycode == Input.Keys.NUM_2) {
            m_hotbarInventory!!.selectSlot(1)
        } else if (keycode == Input.Keys.NUM_3) {
            m_hotbarInventory!!.selectSlot(2)
        } else if (keycode == Input.Keys.NUM_4) {
            m_hotbarInventory!!.selectSlot(3)
        } else if (keycode == Input.Keys.NUM_5) {
            m_hotbarInventory!!.selectSlot(4)
        } else if (keycode == Input.Keys.NUM_6) {
            m_hotbarInventory!!.selectSlot(5)
        } else if (keycode == Input.Keys.NUM_7) {
            m_hotbarInventory!!.selectSlot(6)
        } else if (keycode == Input.Keys.NUM_8) {
            m_hotbarInventory!!.selectSlot(7)
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

        if (playerComponent.equippedPrimaryItem != OreWorld.ENTITY_INVALID) {
            val dropItemRequestFromClient = Network.HotbarDropItemFromClient()
            val currentEquippedIndex = playerComponent.hotbarInventory.selectedSlot
            dropItemRequestFromClient.index = currentEquippedIndex.toByte()

            // decrement count, we assume it'll get spawned shortly when the server tells us to.
            // delete in-inventory entity if necessary server assumes we already do so
            val itemEntity = playerComponent.equippedPrimaryItem
            val itemComponent = itemMapper.get(itemEntity)
            if (itemComponent.stackSize > 1) {
                //decrement count, server has already done so. we assume here that it went through properly.
                itemComponent.stackSize -= 1
                m_hotbarInventory!!.setCount(currentEquippedIndex, itemComponent.stackSize)
            } else {
                //delete it, server knows/assumes we already did, since there are no more left
                val item = playerComponent.hotbarInventory.takeItem(dropItemRequestFromClient.index.toInt())!!
                m_world!!.m_artemisWorld.delete(item)
            }

            m_networkClientSystem.m_clientKryo.sendTCP(dropItemRequestFromClient)
        }
    }

    override fun keyUp(keycode: Int): Boolean {
        if (m_world == null) {
            return false
        }

        if (!m_networkClientSystem.connected) {
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
        leftMouseDown = true

        if (m_world != null) {
            return m_world!!.touchDown(screenX, screenY, pointer, button)
            //fixme

        }

        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        leftMouseDown = false
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
        if (m_world == null) {
            return false
        }

        if (!m_networkClientSystem.connected) {
            return false
        }

        if (m_powerOverlayRenderSystem.overlayVisible) {
            //don't allow item/inventory selection during this
            return false
        }

        val index = m_hotbarInventory!!.selectedSlot
        if (amount > 0) {
            //right, inventory selection scrolling does not wrap around.
            m_hotbarInventory!!.selectSlot(Math.min(index + 1, Inventory.maxHotbarSlots - 1))
        } else {
            //left
            m_hotbarInventory!!.selectSlot(Math.max(index - 1, 0))
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

        m_hotbarInventory = Inventory(player, Inventory.InventoryType.Hotbar)
        m_world!!.m_artemisWorld.inject(m_hotbarInventory, true)
        playerComponent.hotbarInventory = m_hotbarInventory

        m_hotbarInventory!!.addListener(HotbarSlotListener())

        m_inventory = Inventory(player, Inventory.InventoryType.Inventory)
        m_world!!.m_artemisWorld.inject(m_inventory, true)
        playerComponent.inventory = m_inventory

        m_hotbarView = HotbarInventoryView(stage, skin, m_hotbarInventory!!, m_inventory!!, m_dragAndDrop!!, m_world!!)
        m_inventoryView = InventoryView(stage, skin, m_hotbarInventory!!, m_inventory!!, m_dragAndDrop!!, m_world!!)

        if (mainPlayer) {
            m_tagManager.register(OreWorld.s_mainPlayer, player)
        }

        //select the first slot, so the inventory view highlights something.
        playerComponent.hotbarInventory.selectSlot(0)

        //          SpriteComponent spriteComponent = spriteMapper.get(player);
        //        spriteComponent.sprite.setTexture();

        return player
    }

    private inner class HotbarSlotListener : Inventory.SlotListener {
        override fun selected(index: Int, inventory: Inventory) {
            assert(m_world != null)

            val player = m_tagManager.getEntity(OreWorld.s_mainPlayer).id

            m_networkClientSystem.sendHotbarEquipped(index.toByte())

            val playerComponent = playerMapper.get(player)
        }

        override fun countChanged(index: Int, inventory: Inventory) {

        }

        override operator fun set(index: Int, inventory: Inventory) {

        }

        override fun removed(index: Int, inventory: Inventory) {

        }
    }

    private class NetworkConnectListener(private val m_client: OreClient) : NetworkClientSystem.NetworkClientListener {

        override fun connected() {
            //todo surely there's some first-time connection stuff we must do?
        }

        override fun disconnected() {
            //todo show gui, say we've disconnected
        }

    }

    companion object {
        val ORE_VERSION_MAJOR = 0
        val ORE_VERSION_MINOR = 1
        val ORE_VERSION_REVISION = 1
    }

}
