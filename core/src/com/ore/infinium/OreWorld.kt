package com.ore.infinium

import com.artemis.*
import com.artemis.managers.PlayerManager
import com.artemis.managers.TagManager
import com.artemis.systems.EntityProcessingSystem
import com.artemis.utils.Bag
import com.artemis.utils.reflect.ClassReflection
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.math.*
import com.badlogic.gdx.utils.PerformanceCounter
import com.ore.infinium.components.*
import com.ore.infinium.systems.*
import com.ore.infinium.systems.profiler.ProfilerSystem
import java.util.*

/**
 * ***************************************************************************
 * Copyright (C) 2014, 2015 by Shaun Reich @gmail.com>              *
 * *
 * This program is free software; you can redistribute it and/or             *
 * modify it under the terms of the GNU General Public License as            *
 * published by the Free Software Foundation; either version 2 of            *
 * the License, or (at your option) any later version.                       *
 * *
 * This program is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 * GNU General Public License for more details.                              *
 * *
 * You should have received a copy of the GNU General Public License         *
 * along with this program.  If not, see //www.gnu.org/licenses/>.     *
 * ***************************************************************************
 */
class OreWorld
/**
 * The main world, shared between both client and server, core to a lot of basic
 * shared functionality, as well as stuff that doesn't really belong elsewhere,
 * creates the artemis world and handles processing, as well as shutting down
 * (when told to do so)

 * @param client
 * *         never null..
 * *
 * @param server
 * *         null if it is only a client, if both client and server are valid, the
 * *         this is a local hosted server, (aka singleplayer, or self-hosting)
 */
(var m_client: OreClient?, //fixme players really should be always handled by the system..and i suspect a lot of logic can be handled by
        // them alone.
 var m_server: OreServer?, var worldInstanceType: OreWorld.WorldInstanceType) {

    //each unit is 1 block(16x16 px), in the game world
    //public OreBlock[] blocks;
    var blocks: ByteArray
    lateinit var assetManager: AssetManager
    lateinit var m_camera: OrthographicCamera

    //fixme remove in favor of the render system
    lateinit var m_atlas: TextureAtlas

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>
    private lateinit var blockMapper: ComponentMapper<BlockComponent>
    private lateinit var airGeneratorMapper: ComponentMapper<AirGeneratorComponent>
    private lateinit var toolMapper: ComponentMapper<ToolComponent>
    private lateinit var airMapper: ComponentMapper<AirComponent>
    private lateinit var healthMapper: ComponentMapper<HealthComponent>
    private lateinit var lightMapper: ComponentMapper<LightComponent>
    private lateinit var powerDeviceMapper: ComponentMapper<PowerDeviceComponent>
    private lateinit var powerConsumerMapper: ComponentMapper<PowerConsumerComponent>
    private lateinit var powerGeneratorMapper: ComponentMapper<PowerGeneratorComponent>

    private val m_noClipEnabled: Boolean = false

    lateinit var m_artemisWorld: World

    /**
     * who owns/is running this exact world instance. If it is the server, or a client.
     * Note that if the connection type is only a client, obviously a server
     * world type will never exist
     */
    enum class WorldInstanceType {
        Client,
        Server,
        ClientHostingServer
    }

    init {

        //blocks[(x * 2400 + y) << 2 + i] where i = 0, 1, 2 or 3
        //        blocks = new OreBlock[WORLD_SIZE_Y * WORLD_SIZE_X];
        blocks = ByteArray(WORLD_SIZE_Y * WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT)
    }

    internal fun init() {
        assert(isHotspotOptimizationEnabled) { "error, hotspot optimization (artemis-odb weaving) is not enabled" }

        if (worldInstanceType == WorldInstanceType.Client || worldInstanceType == WorldInstanceType.ClientHostingServer) {
            val width = OreSettings.width / BLOCK_SIZE_PIXELS
            val height = OreSettings.height / BLOCK_SIZE_PIXELS
            m_camera = OrthographicCamera(width, height)//30, 30 * (h / w));
            m_camera.setToOrtho(true, width, height)

            m_atlas = TextureAtlas(Gdx.files.internal("packed/entities.atlas"))

            //we don't generate the block world with noise, just init it so it isn't
            //it isn't null
            initializeBlocksArray()

            //note although it may look like it.. order for render/logic ones..actually doesn't matter, their base
            // class dictates this.
            m_artemisWorld = World(WorldConfigurationBuilder().register(GameLoopSystemInvocationStrategy(25, false))
                                           .with(TagManager())
                                           .with(PlayerManager())
                                           .with(MovementSystem(this))
                                           .with(NetworkClientSystem(this))
                                           .with(ServerPowerCircuitSystem(this))
                                           .with(InputSystem(m_camera, this))
                                           .with(EntityOverlaySystem(this))
                                           .with(PlayerSystem(this))
                                           .with(GameTickSystem(this))
                                           .with(ClientBlockDiggingSystem(this, m_client!!))
                                           .with(TileRenderSystem(m_camera, this))
                                           .with(SpriteRenderSystem(this))
                                           .with(DebugTextRenderSystem(m_camera, this))
                                           .with(PowerOverlayRenderSystem(this, m_client!!.stage,
                                                                          m_client!!.skin))
                                           .with(TileTransitionSystem(m_camera, this))
                                           .with(ProfilerSystem(m_client!!.skin))
                                           .build())
            //b.dependsOn(WorldConfigurationBuilder.Priority.LOWEST + 1000,ProfilerSystem.class);

            //inject the mappers into the world, before we start doing things
            m_artemisWorld.inject(this, true)

            val w = Gdx.graphics.width.toFloat()
            val h = Gdx.graphics.height.toFloat()
        } else if (worldInstanceType == WorldInstanceType.Server) {
            m_artemisWorld = World(WorldConfigurationBuilder()
                                           .with(TagManager())
                                           .with(SpatialSystem(this))
                                           .with(PlayerManager())
                                           .with(MovementSystem(this))
                                           .with(ServerPowerCircuitSystem(this))
                                           .with(GameTickSystem(this))
                                           .with(GrassBlockSystem(this))
                                           .with(ServerNetworkEntitySystem(this))
                                           .with(ServerBlockDiggingSystem(this))
                                           .with(PlayerSystem(this))
                                           .with(NetworkServerSystem(this, m_server!!))
                                           .register(GameLoopSystemInvocationStrategy(25, true))
                                           .build())
            //inject the mappers into the world, before we start doing things
            m_artemisWorld.inject(this, true)

            generateWorld()
        }

        //        assetManager = new AssetManager();
        //        TextureAtlas m_blockAtlas = assetManager.get("data/", TextureAtlas.class);
        //        assetManager.finishLoading();

        //        m_camera.position.set(m_camera.viewportWidth / 2f, m_camera.viewportHeight / 2f, 0);
    }

    /**
     * hotspot optimization replaces (amongst other steps) references to entityprocessingsystem with entitysystem.
     * so we can determine this optimization by EntityProcessingSystem missing from our system's hierarchy.
     * this is for artemis-odb optimization, ing annotations. it helps inline some calls
     */
    private
    val isHotspotOptimizationEnabled: Boolean
        get() = !ClassReflection.isAssignableFrom(EntityProcessingSystem::class.java, NetworkClientSystem::class.java)

    fun initServer() {
    }

    /**
     * @param playerName
     * *
     * @param connectionId
     * *
     * *
     * @return
     */
    fun createPlayer(playerName: String, connectionId: Int): Int {
        val playerEntity = m_artemisWorld.create()
        val playerSprite = spriteMapper.create(playerEntity)
        velocityMapper.create(playerEntity)

        val playerComponent = playerMapper.create(playerEntity)
        playerComponent.connectionPlayerId = connectionId
        //fixme fixme, should be consolidated w/ sprite's noclip...or should it?? make mention, is sprite present on
        // the server?? at least the component, maybe not inner sprite
        playerComponent.noClip = m_noClipEnabled

        playerComponent.playerName = playerName
        playerComponent.loadedViewport.rect = Rectangle(0f, 0f, LoadedViewport.MAX_VIEWPORT_WIDTH.toFloat(),
                                                        LoadedViewport.MAX_VIEWPORT_HEIGHT.toFloat())
        playerComponent.loadedViewport.centerOn(Vector2(playerSprite.sprite.x, playerSprite.sprite.y))

        playerSprite.sprite.setSize(2f, 3f)
        controlMapper.create(playerEntity)

        playerSprite.textureName = "player1Standing1"
        playerSprite.category = SpriteComponent.EntityCategory.Character
        jumpMapper.create(playerEntity)

        val healthComponent = healthMapper.create(playerEntity)
        healthComponent.health = healthComponent.maxHealth

        val airComponent = airMapper.create(playerEntity)
        airComponent.air = airComponent.maxAir

        return playerEntity
    }

    private fun generateWorld() {

        Gdx.app.log("server world gen", "worldgen starting")
        val counter = PerformanceCounter("test")
        counter.start()

        generateOres()
        generateGrassTiles()
        generateTrees()

        counter.stop()
        val s = "total world gen took (incl transitioning, etc): %s seconds".format(counter.current)
        Gdx.app.log("", s)

    }

    private fun generateTrees() {
        val rand = RandomXS128()
        //        for (int y = 0; y < WORLD_SIZE_Y; ++y) {
        //       }
        //randomRange(, 20, rand)
        var spriteComponent: SpriteComponent
        var x = 0
        while (x < WORLD_SIZE_X - 50) {
            val tree = createTree()
            spriteComponent = spriteMapper.get(tree)
            val halfTreeHeight = spriteComponent.sprite.height

            val y = seaLevel() - halfTreeHeight.toInt()

            //final byte blockType = blockType(x, y);

            spriteComponent.sprite.setPosition(x.toFloat(), y.toFloat())
            x += 5
        }

    }

    /**
     * world gen, generates the initial grass of the world
     */
    private fun generateGrassTiles() {
        for (x in 0..WORLD_SIZE_X - 1) {
            var y = 0
            while (y < WORLD_SIZE_Y) {
                val blockType = blockType(x, y)

                //fixme check biomes and their ranges
                //fill the surface/exposed dirt blocks with grass blocks
                if (blockType == OreBlock.BlockType.DirtBlockType) {
                    val topBlockType = blockTypeSafely(x, y - 1)

                    if (topBlockType == OreBlock.BlockType.NullBlockType) {
                        setBlockFlag(x, y, OreBlock.BlockFlags.GrassBlock)

                        y = WORLD_SIZE_Y
                    }
                }
                ++y
            }
        }

        for (x in 0..WORLD_SIZE_X - 1) {
            for (y in 0..WORLD_SIZE_Y - 1) {
                val blockType = blockType(x, y)

                if (blockType == OreBlock.BlockType.DirtBlockType && blockHasFlag(x, y,
                                                                                  OreBlock.BlockFlags.GrassBlock)) {

                    val topBlockType = blockTypeSafely(x, y - 1)
                    //OreBlock bottomBlock = blockTypeSafely(x, y + 1);
                    //OreBlock bottomLeftBlock = blockTypeSafely(x - 1, y + 1);
                    //OreBlock bottomRightBlock = blockTypeSafely(x + 1, y + 1);

                    //                    boolean leftEmpty =

                    //grows grass here
                    if (topBlockType == OreBlock.BlockType.NullBlockType) {
                        setBlockFlag(x, y, OreBlock.BlockFlags.GrassBlock)
                    }
                }
            }
        }
    }

    fun initializeBlocksArray() {
        //NEEDED?? hack
        /*
        for (int x = 0; x < WORLD_SIZE_X; ++x) {
            for (int y = 0; y < WORLD_SIZE_Y; ++y) {

                int index = x * WORLD_SIZE_Y + y;
                blocks[index] = new OreBlock();
                blocks[index].type = OreBlock.BlockType.NullBlockType;
            }
        }
        */
    }

    private fun generateOres() {
        for (x in 0..WORLD_SIZE_X - 1) {
            for (y in 0..WORLD_SIZE_Y - 1) {

                setBlockType(x, y, OreBlock.BlockType.NullBlockType)
                setBlockWallType(x, y, OreBlock.WallType.NullWallType)

                //create some sky
                if (y <= seaLevel()) {
                    continue
                }

                //              boolean underground = true;

                when (MathUtils.random(0, 3)) {
                    0 -> setBlockType(x, y, OreBlock.BlockType.NullBlockType)

                    1 -> setBlockType(x, y, OreBlock.BlockType.DirtBlockType)
                //fixme, simulate only dirt for now. blocks[index].type = Block.BlockType.StoneBlockType;
                    2 -> setBlockType(x, y, OreBlock.BlockType.DirtBlockType)
                }

                //                if (underground) {
                setBlockWallType(x, y, OreBlock.WallType.DirtUndergroundWallType)
                //               }

                //                blocks[dragSourceIndex].wallType = Block::Wall
            }
        }
        //        for (int x = 0; x < WORLD_SIZE_X; ++x) {
        //            for (int y = seaLevel(); y < WORLD_SIZE_Y; ++y) {
        //                Block block = blockAt(x, y);
        //                block.type = Block.BlockType.DirtBlockType;
        //            }
        //        }
    }

    /**
     * safely return a block at x, y, clamped at world bounds

     * @param x
     * *
     * @param y
     * *
     * *
     * @return
     */
    fun blockTypeSafely(x: Int, y: Int): Byte {
        var x = x
        var y = y
        x = MathUtils.clamp(x, 0, WORLD_SIZE_X - 1)
        y = MathUtils.clamp(y, 0, WORLD_SIZE_Y - 1)
        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE]
    }

    /**
     * take a possibly-unsafe x block index,
     * and return a safe (clamped) one

     * @param x
     * *
     * *
     * @return
     */
    fun blockXSafe(x: Int): Int {
        return MathUtils.clamp(x, 0, WORLD_SIZE_X - 1)
    }

    /**
     * take a possibly-unsafe y block index,
     * and return a safe (clamped) one

     * @param y
     * *
     * *
     * @return
     */
    fun blockYSafe(y: Int): Int {
        return MathUtils.clamp(y, 0, WORLD_SIZE_Y - 1)
    }

    //blocks[(x * 2400 + y) * 4 + i] where i = 0, 1, 2 or 3
    fun blockType(x: Int, y: Int): Byte {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE) {
            "blockType index out of range. x: $x, y: $y"
        }

        //todo can change it to bitshift if we want to...the jvm should already know to do this though..but idk if it
        // will do it
        //blocks[(x * 2400 + y) << 2 + i] where i = 0, 1, 2 or 3
        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE]
    }

    fun blockWallType(x: Int, y: Int): Byte {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE) {
            "blockWallType index out of range. x: $x, y: $y"
        }

        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE]
    }

    fun blockMeshType(x: Int, y: Int): Byte {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE) {
            "blockMeshType index out of range. x: $x, y: $y"
        }
        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE]
    }

    fun blockFlags(x: Int, y: Int): Byte {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS) {
            "blockFlags index out of range. x: $x, y: $y"
        }
        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS]
    }

    fun blockHasFlag(x: Int, y: Int, flag: Byte): Boolean {

        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS) {
            "blockHasFlag index out of range. x: $x, y: $y"
        }

        return blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS].toInt().and(
                flag.toInt()) != 0
    }

    fun setBlockType(x: Int, y: Int, type: Byte) {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE) {
            "setBlockType index out of range. x: $x, y: $y"
        }

        //todo can change it to bitshift if we want to...the jvm should already know to do this though..but idk
        //blocks[(x * 2400 + y) << 2 + i] where i = 0, 1, 2 or 3
        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_TYPE] = type
    }

    fun setBlockWallType(x: Int, y: Int, wallType: Byte) {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE) {
            "setBlockWallType index out of range. x: $x, y: $y"
        }

        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_WALLTYPE] = wallType
    }

    fun setBlockMeshType(x: Int, y: Int, meshType: Byte) {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE) {
            "setBlockMeshType index out of range. x: $x, y: $y"
        }
        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_MESHTYPE] = meshType
    }

    /**
     * overwrites the blocks current flags, to now be the provided flags

     * @param x
     * *
     * @param y
     * *
     * @param flags
     */
    fun setBlockFlags(x: Int, y: Int, flags: Byte) {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS) {
            "setBlockFlags index out of range. x: $x, y: $y"
        }
        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS] = flags
    }

    /**
     * disable a block flag

     * @param x
     * *
     * @param y
     * *
     * @param flagToEnable
     */
    fun unsetBlockFlag(x: Int, y: Int, flagToEnable: Byte) {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS) {
            "enableBlockFlags index out of range. x: $x, y: $y"
        }

        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS] =
                blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS].toInt().and(
                        flagToEnable.toInt()).inv().toByte()
    }

    /**
     * enable a block flag

     * @param x
     * *
     * @param y
     * *
     * @param flagToEnable
     */
    fun setBlockFlag(x: Int, y: Int, flagToEnable: Byte) {
        assert(x >= 0 && y >= 0 &&
                       x <= WORLD_SIZE_X * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS &&
                       y <= WORLD_SIZE_Y * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS) {
            "enableBlockFlags index out of range. x: $x, y: $y"
        }

        blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS] =
                blocks[(x * WORLD_SIZE_Y + y) * OreBlock.BLOCK_FIELD_COUNT + OreBlock.BLOCK_FIELD_INDEX_FLAGS].toInt().or(
                        flagToEnable.toInt()).toByte()
    }

    /**
     * properly destroys the block in the array (sets meshtype, flags etc to defaults)
     * must be called when destroying a block, so it looks like it was dug.
     */
    fun destroyBlock(x: Int, y: Int) {
        setBlockType(x, y, OreBlock.BlockType.NullBlockType)
        setBlockMeshType(x, y, 0)
        //wall type doesn't get nulled out. i think that's what we want
        setBlockFlags(x, y, 0)
    }

    fun isBlockSolid(x: Int, y: Int): Boolean {
        var solid = true

        val type = blockType(x, y)

        if (type == OreBlock.BlockType.NullBlockType) {
            solid = false
        }

        return solid
    }

    /**
     * Safely shutdown the world, disposing of all the systems
     * Each system should be designed such that it can safely shut itself down without
     * having to interface with other systems. Though some exceptions may apply
     */
    fun shutdown() {
        m_artemisWorld.dispose()
    }

    /**
     * main world processing,
     * will handle all logic/render processing,
     * as it delegates this to the ECS, which handles
     * ordering and so on.
     */
    fun process() {
        m_artemisWorld.process()
    }

    /**
     * Attempts to place a block at position with the type, can fail. If it succeeds it will *not*
     * notify anything (network wise). If it succeeds, it will take care of destroying e.g. nearby grass,
     * and doing whatever else may need to be done on nearby conditions

     * @param x
     * *
     * @param y
     * *
     * @param placedBlockType
     * *         block type to change it to
     * *
     * *
     * @return true if placement succeeded.
     */
    fun attemptBlockPlacement(x: Int, y: Int, placedBlockType: Byte): Boolean {
        val blockType = blockTypeSafely(x, y)

        //attempt to place one if the area is empty
        if (blockType == OreBlock.BlockType.NullBlockType) {
            setBlockType(x, y, placedBlockType)

            val bottomBlockX = x
            val bottomBlockY = y + 1
            if (blockHasFlag(bottomBlockX, bottomBlockY, OreBlock.BlockFlags.GrassBlock)) {
                //remove grass flag here.
                unsetBlockFlag(bottomBlockX, bottomBlockY, OreBlock.BlockFlags.GrassBlock)
            }

            return true
        }
        //TODO: check collision with other entities...

        return false
    }

    fun mousePositionWorldCoords(): Vector2 {
        //libgdx can and probably will return negative mouse coords..
        val mouse = Vector3(Math.max(Gdx.input.x, 0).toFloat(), Math.max(Gdx.input.y, 0).toFloat(), 0f)
        val finalMouse = m_camera.unproject(mouse)

        return Vector2(finalMouse.x, finalMouse.y)
    }

    fun alignPositionToBlocks(pos: Vector2, size: Vector2) {
        var x = MathUtils.floor(pos.x).toFloat()
        var y = MathUtils.floor(pos.y).toFloat()

        //if size is odd,  it won't look aligned properly
        if (size.x % 2 == 1f) {
            x -= 0.5f
        }

        //odd
        if (size.y % 2 == 1f) {
            y -= 0.5f
        }

        pos.set(x, y)
    }

    fun seaLevel(): Int {
        return WORLD_SEA_LEVEL
    }

    /**
     * @param blockType
     */
    fun createBlockItem(blockType: Byte): Int {
        val block = m_artemisWorld.create()
        velocityMapper.create(block)

        val blockComponent = blockMapper.create(block)
        blockComponent.blockType = blockType

        val blockSprite = spriteMapper.create(block)
        blockSprite.textureName = blockAttributes[blockComponent.blockType]!!.textureName

        blockSprite.sprite.setSize(1f, 1f)

        val itemComponent = itemMapper.create(block)
        itemComponent.stackSize = 800
        itemComponent.maxStackSize = 900

        return block
    }

    fun createLight(): Int {
        val light = m_artemisWorld.create()

        velocityMapper.create(light)

        val itemComponent = itemMapper.create(light)
        itemComponent.stackSize = 800
        itemComponent.maxStackSize = 900

        val powerDeviceComponent = powerDeviceMapper.create(light)

        val sprite = spriteMapper.create(light)
        sprite.textureName = "light-yellow"

        sprite.sprite.setSize(1f, 1f)

        val powerConsumerComponent = powerConsumerMapper.create(light)
        powerConsumerComponent.powerDemandRate = 100

        return light
    }

    fun createPowerGenerator(): Int {
        val power = m_artemisWorld.create()

        velocityMapper.create(power)

        val itemComponent = itemMapper.create(power)
        itemComponent.stackSize = 800
        itemComponent.maxStackSize = 900

        val powerDeviceComponent = powerDeviceMapper.create(power)

        val sprite = spriteMapper.create(power)
        sprite.textureName = "air-generator-64x64"

        sprite.sprite.setSize(4f, 4f)

        val powerComponent = powerGeneratorMapper.create(power)
        powerComponent.powerSupplyRate = 100

        return power
    }

    fun createAirGenerator(): Int {
        val air = m_artemisWorld.create()
        val itemComponent = itemMapper.create(air)
        itemComponent.stackSize = 800
        itemComponent.maxStackSize = 900

        velocityMapper.create(air)

        val power = powerDeviceMapper.create(air)

        val airSprite = spriteMapper.create(air)
        airSprite.textureName = "air-generator-64x64"

        airSprite.sprite.setSize(4f, 4f)

        val airComponent = airGeneratorMapper.create(air)
        airComponent.airOutputRate = 100

        return air
    }

    fun createTree(): Int {
        val tree = m_artemisWorld.create()

        val sprite = spriteMapper.create(tree)
        sprite.textureName = "flora/tree-02"
        sprite.sprite.setSize(5f, 13f)

        return tree
    }

    /**
     * @param entity
     * *         entity id
     * *
     * *
     * @return true if the item can be placed where it currently resides, without any obstructions
     */
    fun isPlacementValid(entity: Int): Boolean {
        val spriteComponent = spriteMapper.get(entity)
        val pos = Vector2(spriteComponent.sprite.x, spriteComponent.sprite.y)
        val size = Vector2(spriteComponent.sprite.width, spriteComponent.sprite.height)

        val epsilon = 0.001f
        val startX = (pos.x - size.x * 0.5f + epsilon).toInt()
        val startY = (pos.y - size.y * 0.5f + epsilon).toInt()

        val endX = (pos.x + size.x * 0.5f).toInt()
        val endY = (pos.y + (size.y * 0.5f - epsilon) + 1).toInt()

        if (!(startX >= 0 && startY >= 0 && endX <= WORLD_SIZE_X && endY <= WORLD_SIZE_Y)) {
            //fixme
            //not sure why, but this ends up giving me some way way invalid values. likely due to mouse being outside
            //of valid range, *somehow*. sometimes does it on startup etc
            return false
        }

        //check collision against blocks first
        for (x in startX..endX - 1) {
            for (y in startY..endY - 1) {
                if (blockType(x, y) != OreBlock.BlockType.NullBlockType) {
                    return false
                }
            }
        }

        //float x = Math.min(pos.x - (BLOCK_SIZE * 20), 0.0f);
        //float y = Math.min(pos.y - (BLOCK_SIZE * 20), 0.0f);
        //float x2 = Math.min(pos.x + (BLOCK_SIZE * 20), WORLD_SIZE_X * BLOCK_SIZE);
        //float y2 = Math.min(pos.y + (BLOCK_SIZE * 20), WORLD_SIZE_Y * BLOCK_SIZE);

        //check collision against entities
        val entities = m_artemisWorld.aspectSubscriptionManager.get(Aspect.all(SpriteComponent::class.java)).entities
        for (i in 0..entities.size() - 1) {
            //it's the item we're trying to place, don't count a collision with ourselves
            if (entities.get(i) == entity) {
                continue
            }

            //ignore players, aka don't count them as colliding when placing static objects.
            //        if (e.has_component<PlayerComponent>()) {
            //            continue;
            //        }

            val itemComponent = itemMapper.getSafe(entities.get(i))
            if (itemComponent != null) {
                // items that are dropped in the world are considered non colliding
                if (itemComponent.state == ItemComponent.State.DroppedInWorld) {
                    continue
                }
            }

            //            if ( m_artemisWorld.getSystem(TagManager.class).getTag(entities.get(i)) != null) {
            //           }

            val entitySpriteComponent = spriteMapper.get(entities.get(i))
            // possible colliding object is not meant to be collided with. skip it/don't count it
            if (entitySpriteComponent.noClip) {
                continue
            }

            if (entityCollides(entities.get(i), entity)) {
                return false
            }
        }

        return true
    }

    private fun entityCollides(first: Int, second: Int): Boolean {
        val spriteComponent1 = spriteMapper.get(first)
        val spriteComponent2 = spriteMapper.get(second)

        val pos1 = Vector2(spriteComponent1.sprite.x, spriteComponent1.sprite.y)
        val pos2 = Vector2(spriteComponent2.sprite.x, spriteComponent2.sprite.y)

        val size1 = Vector2(spriteComponent1.sprite.width, spriteComponent1.sprite.height)
        val size2 = Vector2(spriteComponent2.sprite.width, spriteComponent2.sprite.height)

        val epsilon = 0.0001f

        val left1 = pos1.x - size1.x * 0.5f + epsilon
        val right1 = pos1.x + size1.x * 0.5f - epsilon
        val top1 = pos1.y - size1.y * 0.5f + epsilon
        val bottom1 = pos1.y + size1.y * 0.5f - epsilon

        val left2 = pos2.x - size2.x * 0.5f + epsilon
        val right2 = pos2.x + size2.x * 0.5f - epsilon

        val top2 = pos2.y - size2.y * 0.5f + epsilon
        val bottom2 = pos2.y + size2.y * 0.5f - epsilon

        val collides = !(left2 > right1 || right2 < left1 || top2 > bottom1 || bottom2 < top1)

        return collides
    }

    fun loadSparseBlockUpdate(update: Network.SparseBlockUpdate) {
        //log("sparse block update", "loaded, count: " + update.blocks.size);

        for (sparseBlock in update.blocks) {
            val x = sparseBlock.x
            val y = sparseBlock.y
            setBlockType(x, y, sparseBlock.block!!.type)
            setBlockWallType(x, y, sparseBlock.block!!.wallType)
            setBlockFlags(x, y, sparseBlock.block!!.flags)
        }
    }

    fun loadBlockRegion(region: Network.BlockRegion) {

        var sourceIndex = 0
        for (y in region.y..region.y2) {
            for (x in region.x..region.x2) {
                setBlockType(x, y,
                             region.blocks!![sourceIndex * Network.BlockRegion.BLOCK_FIELD_COUNT + Network.BlockRegion.BLOCK_FIELD_INDEX_TYPE])
                setBlockWallType(x, y,
                                 region.blocks!![sourceIndex * Network.BlockRegion.BLOCK_FIELD_COUNT + Network.BlockRegion.BLOCK_FIELD_INDEX_WALLTYPE])
                setBlockFlags(x, y,
                              region.blocks!![sourceIndex * Network.BlockRegion.BLOCK_FIELD_COUNT + Network.BlockRegion.BLOCK_FIELD_INDEX_FLAGS])

                ++sourceIndex
            }
        }

        //log("client block region", String.format("loaded %s tile index", sourceIndex));

        //fixme should re transition tiles in this area
    }

    /**
     * Clone everything about the entity and adds it to the engine/world

     * @param sourceEntity
     * *         to clone
     * *
     * *
     * @return the cloned entity
     */
    fun cloneEntity(sourceEntity: Int): Int {
        val clonedEntity = m_artemisWorld.create()

        //sorted alphabetically for your pleasure
        if (airMapper.has(sourceEntity)) {
            val sourceComponent = airMapper.get(sourceEntity)
            val component = airMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (airGeneratorMapper.has(sourceEntity)) {
            val sourceComponent = airGeneratorMapper.get(sourceEntity)
            val component = airGeneratorMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (blockMapper.has(sourceEntity)) {
            val sourceComponent = blockMapper.get(sourceEntity)
            val component = blockMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (controlMapper.has(sourceEntity)) {
            val sourceComponent = controlMapper.get(sourceEntity)
            val component = controlMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (healthMapper.has(sourceEntity)) {
            val sourceComponent = healthMapper.get(sourceEntity)
            val component = healthMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (itemMapper.has(sourceEntity)) {
            val sourceComponent = itemMapper.get(sourceEntity)
            val component = itemMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (jumpMapper.has(sourceEntity)) {
            val sourceComponent = jumpMapper.get(sourceEntity)
            val component = jumpMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        //player, unneeded
        assert(playerMapper.getSafe(sourceEntity) == null)

        if (spriteMapper.has(sourceEntity)) {
            val sourceComponent = spriteMapper.get(sourceEntity)
            val component = spriteMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)

            if (worldInstanceType != WorldInstanceType.Server) {
                component.sprite.setRegion(m_atlas.findRegion(component.textureName))
            }
        }

        if (toolMapper.has(sourceEntity)) {
            val sourceComponent = toolMapper.get(sourceEntity)
            val component = toolMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (lightMapper.has(sourceEntity)) {
            val sourceComponent = lightMapper.get(sourceEntity)
            val component = lightMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (velocityMapper.has(sourceEntity)) {
            val sourceComponent = velocityMapper.get(sourceEntity)
            val component = velocityMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (powerDeviceMapper.has(sourceEntity)) {
            val sourceComponent = powerDeviceMapper.get(sourceEntity)
            val component = powerDeviceMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (powerConsumerMapper.has(sourceEntity)) {
            val sourceComponent = powerConsumerMapper.get(sourceEntity)
            val component = powerConsumerMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        if (powerGeneratorMapper.has(sourceEntity)) {
            val sourceComponent = powerGeneratorMapper.get(sourceEntity)
            val component = powerGeneratorMapper.create(clonedEntity)
            component.copyFrom(sourceComponent)
        }

        return clonedEntity
    }

    /**
     * gets the player entity that corresponds to this player id.

     * @param playerId
     * *         the playerid of the player
     * *
     * *
     * @return the player entity
     */
    fun playerEntityForPlayerID(playerId: Int): Int {
        val entities = m_artemisWorld.aspectSubscriptionManager.get(Aspect.all(PlayerComponent::class.java)).entities
        var playerComponent: PlayerComponent
        for (i in 0..entities.size() - 1) {
            playerComponent = playerMapper.get(entities.get(i))
            if (playerComponent.connectionPlayerId == playerId) {
                return entities.get(i)
            }
        }

        throw IllegalStateException("player id attempted to be obtained from item, but this player does not exist")
    }

    //fixme better way to do key and mouse events. i'd like to just have systems be able to sign up,
    //and they can process that in there. or maybe this should be in the client..after all, a server has no key events
    fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button == Input.Buttons.LEFT) {
            val powerOverlaySystem = m_artemisWorld.getSystem(PowerOverlayRenderSystem::class.java)
            if (powerOverlaySystem.overlayVisible) {
                powerOverlaySystem.leftMouseClicked()
            }

            return true
        } else {
            //right
            val powerOverlaySystem = m_artemisWorld.getSystem(PowerOverlayRenderSystem::class.java)
            if (powerOverlaySystem.overlayVisible) {
                powerOverlaySystem.rightMouseClicked()
            }

        }

        return false
    }

    fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (button != Input.Buttons.LEFT) {
            return false
        }

        val powerOverlaySystem = m_artemisWorld.getSystem(PowerOverlayRenderSystem::class.java)
        if (powerOverlaySystem.overlayVisible) {
            powerOverlaySystem.leftMouseReleased()
            return true
        } else {
        }

        return false
    }

    /**
     * gets a list of components this entity has. Mostly for debug

     * @param entity
     * *
     * *
     * @return
     */
    fun getComponentsForEntity(entity: Int): Bag<Component> {
        val bag = Bag<Component>()
        m_artemisWorld.getEntity(entity).getComponents(bag)

        return bag
    }

    class BlockAttributes internal constructor(var textureName: String //e.g. "dirt", "stone", etc.
                                               ,
                                               /**
                                                * whether or not things should collide with this block
                                                */
                                               internal var collision: BlockAttributes.Collision,
                                               var category: BlockAttributes.BlockCategory,
                                               blockTotalHealth: Short) {

        /**
         * max starting health of the block
         */
        var blockTotalHealth: Float = 0f

        //if this type is a type of ore (like stone, copper, ...)
        enum class BlockCategory {
            Null,
            Dirt,
            Ore
            //liquid
        }

        enum class Collision {
            True,
            False
        }

        init {
            this.blockTotalHealth = blockTotalHealth.toFloat()
        }
    }

    companion object {
        val GRAVITY_ACCEL = 0.5f
        val GRAVITY_ACCEL_CLAMP = 0.5f

        val BLOCK_SIZE_PIXELS = 16.0f

        val WORLD_SIZE_X = 2400 //2400
        val WORLD_SIZE_Y = 8400 //8400
        val WORLD_SEA_LEVEL = 50

        /**
         * indicates an invalid entity id
         */
        val ENTITY_INVALID = -1

        /**
         * looks up the texture prefix name for each block type. e.g. DirtBlockType -> "dirt", etc.
         */
        val blockAttributes = HashMap<Byte, BlockAttributes>()

        init {
            blockAttributes.put(OreBlock.BlockType.NullBlockType,
                                BlockAttributes("", BlockAttributes.Collision.False, BlockAttributes.BlockCategory.Null,
                                                0.toShort()))
            blockAttributes.put(OreBlock.BlockType.DirtBlockType,
                                BlockAttributes("dirt", BlockAttributes.Collision.True,
                                                BlockAttributes.BlockCategory.Dirt, 200.toShort()))
            blockAttributes.put(OreBlock.BlockType.StoneBlockType,
                                BlockAttributes("stone", BlockAttributes.Collision.True,
                                                BlockAttributes.BlockCategory.Ore, 300.toShort()))
        }

        val s_itemPlacementOverlay = "itemPlacementOverlay"
        val s_crosshair = "crosshair"
        val s_mainPlayer = "mainPlayer"

        fun randomRange(start: Int, end: Int, rand: RandomXS128): Int {
            return start + rand.nextInt(end - start + 1)
        }

        fun log(tag: String, message: String) {
            val datetime = java.time.LocalDateTime.now()
            val time = datetime.format(java.time.format.DateTimeFormatter.ofPattern("HH:m:s:S"))

            Gdx.app.log(tag, "$message [$time ]")
        }
    }
}
