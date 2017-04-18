package com.ore.infinium

import com.artemis.ComponentMapper
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.ore.infinium.components.*
import com.ore.infinium.systems.server.TileLightingSystem
import java.util.*

class OreEntityFactory(val oreWorld: OreWorld) {
    private lateinit var mPlayer: ComponentMapper<PlayerComponent>
    private lateinit var mDoor: ComponentMapper<DoorComponent>
    private lateinit var mSprite: ComponentMapper<SpriteComponent>
    private lateinit var mControl: ComponentMapper<ControllableComponent>
    private lateinit var mItem: ComponentMapper<ItemComponent>
    private lateinit var mVelocity: ComponentMapper<VelocityComponent>
    private lateinit var mJump: ComponentMapper<JumpComponent>
    private lateinit var mBlock: ComponentMapper<BlockComponent>
    private lateinit var mTool: ComponentMapper<ToolComponent>
    private lateinit var mAir: ComponentMapper<AirComponent>
    private lateinit var mHealth: ComponentMapper<HealthComponent>
    private lateinit var mLight: ComponentMapper<LightComponent>
    private lateinit var mFlora: ComponentMapper<FloraComponent>
    private lateinit var mPowerDevice: ComponentMapper<PowerDeviceComponent>
    private lateinit var mPowerConsumer: ComponentMapper<PowerConsumerComponent>
    private lateinit var mPowerGenerator: ComponentMapper<PowerGeneratorComponent>

    val artemisWorld = oreWorld.artemisWorld

    init {
        oreWorld.artemisWorld.inject(this, true)
    }

    /**
     * @param blockType
     */
    fun createBlockItem(blockType: Byte): Int {
        val entity = artemisWorld.create()
        mVelocity.create(entity)

        val cBlock = mBlock.create(entity)
        cBlock.blockType = blockType

        mSprite.create(entity).apply {
            textureName = OreBlock.blockAttributes[cBlock.blockType]!!.textureName
            sprite.setSize(1f, 1f)
        }

        mItem.create(entity).apply {
            stackSize = 800
            maxStackSize = 800
            name = OreBlock.nameOfBlockType(blockType)!!
        }

        return entity
    }

    fun createLiquidGun(): Int {
        val entity = artemisWorld.create()
        mVelocity.create(entity)

        mTool.create(entity).apply {
            type = ToolComponent.ToolType.Bucket
            attackIntervalMs = 100
        }

        mSprite.create(entity).apply {
            textureName = "drill"
            sprite.setSize(2f, 2f)
        }

        val newStackSize = 1
        mItem.create(entity).apply {
            stackSize = newStackSize
            maxStackSize = newStackSize
            name = "Liquid Gun"
        }

        return entity
    }

    fun createLight(): Int {
        val entity = artemisWorld.create()

        mVelocity.create(entity)

        mItem.create(entity).apply {
            stackSize = 800
            maxStackSize = 900
            name = "Light"
        }

        mLight.create(entity).apply {
            radius = TileLightingSystem.MAX_TILE_LIGHT_LEVEL.toInt()
        }

        mPowerDevice.create(entity)

        mSprite.create(entity).apply {
            textureName = "light-yellow"
            sprite.setSize(1f, 1f)
        }

        mPowerConsumer.create(entity).apply {
            powerDemandRate = 100
        }

        return entity
    }

    fun createDoor(): Int {
        val entity = artemisWorld.create()

        mVelocity.create(entity)

        mDoor.create(entity)

        mItem.create(entity).apply {
            stackSize = 50
            maxStackSize = 60
            name = "Door"
            placementAdjacencyHints = EnumSet.of(
                    ItemComponent.PlacementAdjacencyHints.BottomSolid,
                    ItemComponent.PlacementAdjacencyHints.TopSolid)
        }

        mSprite.create(entity).apply {
            textureName = "door-closed-16x36"
            sprite.setSize(1f, 3f)
        }

        return entity
    }

    fun createPowerGenerator(): Int {
        val entity = artemisWorld.create()

        mVelocity.create(entity)

        mItem.create(entity).apply {
            stackSize = 800
            maxStackSize = 900
            name = "Power Generator"
        }

        mPowerDevice.create(entity)

        mSprite.create(entity).apply {
            textureName = "air-generator-64x64"
            sprite.setSize(4f, 4f)
        }

        mPowerGenerator.create(entity).apply {
            supplyRateEU = 100
            fuelSources = GeneratorInventory(GeneratorInventory.MAX_SLOTS, artemisWorld)
            artemisWorld.inject(fuelSources, true)
        }

        return entity
    }

    fun createExplosive(): Int {
        val entity = artemisWorld.create()
        mVelocity.create(entity)

        mTool.create(entity).apply {
            type = ToolComponent.ToolType.Explosive
            blockDamage = 400f
            explosiveRadius = 10
        }

        mSprite.create(entity).apply {
            textureName = "drill"
            sprite.setSize(2f, 2f)
        }

        val newStackSize = 64000
        mItem.create(entity).apply {
            stackSize = newStackSize
            maxStackSize = newStackSize
            name = "Explosives"
        }

        return entity
    }

    fun createBunny(): Int {
        val entity = artemisWorld.create()
        val cSprite = mSprite.create(entity)
        mVelocity.create(entity)

        cSprite.apply {
            sprite.setSize(2f, 3f)
            textureName = "bunny1-stand"
            category = SpriteComponent.EntityCategory.Character
        }

        mControl.create(entity)
        mJump.create(entity)

        mHealth.create(entity).apply {
            health = maxHealth
        }

        mAir.create(entity).apply {
            air = maxAir
        }

        return entity
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
        val entity = artemisWorld.create()
        val cSprite = mSprite.create(entity)
        mVelocity.create(entity)

        val cPlayer = mPlayer.create(entity).apply {
            connectionPlayerId = connectionId
            loadedViewport.rect = Rectangle(0f, 0f, LoadedViewport.MAX_VIEWPORT_WIDTH.toFloat(),
                                            LoadedViewport.MAX_VIEWPORT_HEIGHT.toFloat())
            loadedViewport.centerOn(Vector2(cSprite.sprite.x, cSprite.sprite.y), world = oreWorld)
        }

        cPlayer.playerName = playerName

        cSprite.apply {
            sprite.setSize(2f, 3f)
            textureName = "player1Standing1"
            category = SpriteComponent.EntityCategory.Character
        }

        mControl.create(entity)
        mJump.create(entity)

        mHealth.create(entity).apply {
            health = maxHealth
        }

        mAir.create(entity).apply {
            air = maxAir
        }

        return entity
    }

    fun createDrill(): Int {
        val entity = artemisWorld.create()
        mVelocity.create(entity)

        mTool.create(entity).apply {
            type = ToolComponent.ToolType.Drill
            blockDamage = 400f
        }

        mSprite.create(entity).apply {
            textureName = "drill"
            sprite.setSize(2f, 2f)
        }

        val newStackSize = 64000
        mItem.create(entity).apply {
            stackSize = newStackSize
            maxStackSize = newStackSize
            name = "Drill"
        }

        return entity
    }

    fun createWoodenTree(type: FloraComponent.TreeSize): Int {
        val entity = artemisWorld.create()

        val sprite = mSprite.create(entity)
        val flora = mFlora.create(entity)
        mVelocity.create(entity)

        mItem.create(entity).apply {
            state = ItemComponent.State.InWorldState
            maxStackSize = 64
            name = "Tree"
        }

        when (type) {
            FloraComponent.TreeSize.Large -> {
                sprite.textureName = "flora/tree-02"
                sprite.sprite.setSize(5f, 13f)

                flora.numberOfDropsWhenDestroyed = 4
                flora.stackSizePerDrop = 2
            }

            else -> {
                //undefined
            }
        }

        mHealth.create(entity).apply {
            maxHealth = 2000f
            health = maxHealth
        }

        return entity
    }
}
