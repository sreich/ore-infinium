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

import com.artemis.Component
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.IntArray
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryonet.EndPoint
import com.ore.infinium.components.*
import com.ore.infinium.util.EnumSetSerializer
import com.ore.infinium.util.INVALID_ENTITY_ID
import com.ore.infinium.util.registerClass
import java.util.*

object Network {
    const val PORT = 54553
    const val bufferObjectSize = 255032
    const val bufferWriteSize = 250536

    // This registers objects that are going to be sent over the network.
    fun register(endPoint: EndPoint) {
        val kryo = endPoint.kryo

        registerClient(kryo)
        registerServer(kryo)
        registerShared(kryo)

        registerComponents(kryo)
    }

    //TODO i wonder if i can implement my own serializer (trivially!) and make it use the
    // entity/component pool for entity spawning.
    // look into kryo itself, you can override creation (easily i hope), per class
    // premature optimization right now though
    private fun registerComponents(kryo: Kryo) {
        //components
        kryo.registerClass<Component>()
        kryo.registerClass<PowerDeviceComponent>()
        kryo.registerClass<ItemComponent.State>()
        kryo.registerClass<ItemComponent.PlacementAdjacencyHints>()

        kryo.registerClass<AIComponent>()
        kryo.registerClass<AirComponent>()
        kryo.registerClass<PowerGeneratorComponent>()
        kryo.registerClass<PowerGeneratorComponent.GeneratorType>()
        kryo.registerClass<PowerConsumerComponent>()
        kryo.registerClass<PowerDeviceComponent>()
        kryo.registerClass<BlockComponent>()
        kryo.registerClass<FloraComponent>()
        kryo.registerClass<HealthComponent>()
        kryo.registerClass<ItemComponent>()
        kryo.registerClass<JumpComponent>()
        //playercomponent is not sent. big and pointless.
        //spritecomponent is not sent. big..replaced  by module ones
        //controllablecomponent not sent
        kryo.registerClass<ToolComponent>()
        kryo.registerClass<ToolComponent.ToolMaterial>()
        kryo.registerClass<ToolComponent.ToolType>()

        kryo.registerClass<DoorComponent>()
        kryo.registerClass<DoorComponent.DoorState>()

        kryo.registerClass<LightComponent>()
        kryo.registerClass<VelocityComponent>()
        //////////
    }

    private fun registerShared(kryo: Kryo) {
        kryo.registerClass<Shared.DisconnectReason>()
        kryo.registerClass<Shared.DisconnectReason.Reason>()
        //modular components. some components are too fucking huge and stupid to serialize automatically (like Sprite),
        //so we split up only what we need.
        kryo.registerClass<Shared.PositionPacket>()
        kryo.registerClass<Shared.SizePacket>()

        kryo.registerClass<Shared.BlockRegion>()
        kryo.registerClass<Shared.SparseBlockUpdate>()
        kryo.registerClass<Shared.SingleSparseBlock>()
        kryo.registerClass<Shared.SingleBlock>()

        kryo.registerClass<OreBlock.BlockType>()

        kryo.registerClass<Chat.ChatSender>()
        kryo.registerClass<Shared.InventoryType>()

        // primitives/builtin
        kryo.registerClass<ByteArray>()
        kryo.registerClass<IntArray>()

//        kryo.register(ArrayList::class.java)
        kryo.registerClass<ArrayList<Any>>()

        kryo.registerClass<kotlin.Array<Any>>()
        kryo.registerClass<kotlin.Array<Any>>()
        kryo.registerClass<Vector2>()
        kryo.registerClass<IntArray>()
        kryo.registerClass<Rectangle>()
        kryo.register(EnumSet::class.java, EnumSetSerializer())
    }

    private fun registerServer(kryo: Kryo) {
        kryo.registerClass<Server.EntitySpawn>()
        kryo.registerClass<Server.EntitySpawnMultiple>()
        kryo.registerClass<Server.EntityDestroyMultiple>()
        kryo.registerClass<Server.EntityKilled>()
        kryo.registerClass<Server.EntityMoved>()
        kryo.registerClass<Server.EntityHealthChanged>()

        kryo.registerClass<Server.SpawnInventoryItems>()

        kryo.registerClass<Server.LoadedViewportMoved>()
        kryo.registerClass<Server.PlayerSpawned>()
        kryo.registerClass<Server.PlayerAirChanged>()
        kryo.registerClass<Server.ChatMessage>()
        kryo.registerClass<Server.UpdateGeneratorControlPanelStats>()
        kryo.registerClass<Server.DoorOpen>()
        kryo.registerClass<Server.DeviceToggle>()
    }

    private fun registerClient(kryo: Kryo) {
        kryo.registerClass<Client.InitialClientData>()

        kryo.registerClass<Client.ChatMessage>()

        kryo.registerClass<Client.MoveInventoryItem>()

        kryo.registerClass<Client.PlayerMove>()
        kryo.registerClass<Client.BlockDigBegin>()
        kryo.registerClass<Client.BlockDigFinish>()
        kryo.registerClass<Client.BlockPlace>()
        kryo.registerClass<Client.ItemPlace>()
        kryo.registerClass<Client.PlayerEquipHotbarIndex>()
        kryo.registerClass<Client.InventoryDropItem>()
        kryo.registerClass<Client.OpenDeviceControlPanel>()
        kryo.registerClass<Client.CloseDeviceControlPanel>()
        kryo.registerClass<Client.DoorOpen>()
        kryo.registerClass<Client.DeviceToggle>()

        kryo.registerClass<Client.PlayerEquippedItemAttack>()
        kryo.registerClass<Client.PlayerEquippedItemAttack.ItemAttackType>()

        kryo.registerClass<Client.EntityAttack>()
    }

    object Server {
        class InitialClientData
        class ChatMessage(
                var timestamp: String = "",
                var playerName: String = "",
                var message: String = "",
                var sender: Chat.ChatSender = Chat.ChatSender.Local
                         )

        //fixme: unneeded??
        class LoadedViewportMoved(var rect: Rectangle = Rectangle())

        class DestroyEntities {
            var entityId: IntArray? = null
        }

        class WorldTimeChanged {
            var hour: Int = 0
            var minute: Int = 0
            var second: Int = 0
        }

        class PlayerSpawned(
                // session local id, to be displayed
                var connectionId: Int = 0,
                var playerName: String = "",
                var pos: Vector2 = Vector2()
                //we don't need a size packet for player. we know how big one will be, always.
                           )

        /**
         * toggle running status of a device
         */
        class DeviceToggle(var entityId: Int = INVALID_ENTITY_ID)

        /**
         * sends to the client a list of inventory items to spawn
         */
        class SpawnInventoryItems {
            /**
             * we know which index it gets spawned in due to @see ItemComponent.inventoryIndex
             * this is a list of only non-empty inventory slots. empty ones are not sent
             */
            var entitiesToSpawn = mutableListOf<EntitySpawn>()

            var typeOfInventory = Shared.InventoryType.Hotbar

            /**
             * true if this was spawn because a user picked up an item.
             * Because when the user picks up an item, the item is destroyed(server, client),
             * and then a spawn message for a new one is sent, wherever that may be.
             * so the client may play a pickup sound.
             */
            var causedByPickedUpItem = false

            /**
             * applies only to generator inventory, if this is that type
             * we never spawn this, but this is just so we know which generator
             * we're talking about, and that this packet applies to that
             */
            //var generatorEntityId = INVALID_ENTITY_ID
        }

        /**
         * updates the current output stats for the generator,
         * visible only when the control panel is open
         */
        class UpdateGeneratorControlPanelStats(
                //fixme, not sure if entity id is necessary even
                var generatorEntityId: Int = -1,
                var supply: Int = -1,

                /**
                 * remaining health of the fuel (percent)
                 * 0 means that fuel source would be expired/removed.
                 */
                var fuelHealth: Int = 0
                                              )

        /**
         * sent when the stats of a global power supply/demand gets changed,
         * or when it needs an initial update
         */
        class PowerStats {
            var supply = -1
            var demand = -1
        }

        /**
         * Indicates this entity got killed in the world.
         * (attacked by something, and health hit 0)
         *
         */
        class EntityKilled(
                val entityToKill: Int = INVALID_ENTITY_ID
                          )

        class EntitySpawnMultiple {
            var entitySpawn = mutableListOf<EntitySpawn>()
        }

        /**
         * Tells client to destroy certain entities that it shouldn't have
         * spawned anymore (outside of players region). The entities
         * probably still exist in the world on the server, as well
         * as possibly on other clients.
         *
         * @see EntityKilled
         */
        class EntityDestroyMultiple {
            lateinit var entitiesToDestroy: List<Int>
        }

        class EntitySpawn {
            var size: Vector2 = Vector2()
            var pos: Vector2 = Vector2()

            var textureName: String = ""

            var id: Int = 0

            var components = listOf<Component>()
        }

        class PlayerDisconnected {
            var playerId: Int = 0
        }

        class EntityMoved(var id: Int = -1, var position: Vector2 = Vector2())

        class EntityHealthChanged(var entityId: Int = INVALID_ENTITY_ID, var health: Float = -1f)

        class PlayerAirChanged(var air: Int = -1)

        class DoorOpen(var entityId: Int = INVALID_ENTITY_ID,
                       var state: DoorComponent.DoorState = DoorComponent.DoorState.Closed)

    }

    object Client {
        class InitialClientData {

            /**
             * UUID of player associated with this name. used as a "password" of sorts.
             * so that another cannot log on with the same name and impersonate without that info.
             *
             *
             * Past this point, the server refers to players by id instead. Which is just session-persistent,
             * whereas UUID is world persistent.
             */
            var playerUUID: String? = null
            var playerName: String? = null

            var versionMajor: Int = 0
            var versionMinor: Int = 0
            var versionRevision: Int = 0
        }

        class ChatMessage(var message: String = "")

        //todo reduce data type sizes for lots of this stuff...
        class PlayerEquipHotbarIndex(var index: Byte = 0)

        class BlockDigBegin(var x: Int = 0, var y: Int = 0)

        /**
         * sent when client knows it finished digging a block
         * at x,y successfully.
         * server will verify it is true.
         * if this packet is not sent, the server will eventually time out
         * the dig(cancel it), for that block.
         */
        class BlockDigFinish(var x: Int = 0, var y: Int = 0)

        class BlockPlace(var x: Int = 0, var y: Int = 0)

        class ItemPlace(var x: Float = 0f,
                        var y: Float = 0f)

        class PlayerMove(var position: Vector2? = null)

        /**
         * request for the player to perform
         * an attack on the given entity.
         * server will do some sanity checking
         */
        class EntityAttack(var entityId: Int = 0)

        /**
         * request to drop an item from the specified
         * inventory
         *
         * usually used for dropping the current equipped
         * item, but it can be used for dropping from an
         * inventory window as well.
         *
         * we do not need to send the entity id for a generator
         * inventory, because we(server) already knows which control panel
         * the player has opened. For the generator, fuel sources cannot
         * be moved, since they're already getting burnt
         */
        class InventoryDropItem(var index: Byte = -1,
                                var inventoryType: Shared.InventoryType = Shared.InventoryType.Hotbar)

        class MoveInventoryItem(
                var sourceType: Shared.InventoryType = Shared.InventoryType.Hotbar,
                var destType: Shared.InventoryType = Shared.InventoryType.Hotbar,
                var sourceIndex: Byte = -1,
                var destIndex: Byte = -1
                               )

        /**
         * notify request, to tell server we need info
         * for this device for us to open the control panel
         * (like generator fuel source inventory info)
         *
         * server will keep track of it but we must notify when
         * we close it
         */
        class OpenDeviceControlPanel(var entityId: Int = INVALID_ENTITY_ID)

        class CloseDeviceControlPanel(var entityId: Int = INVALID_ENTITY_ID)

        /**
         * client can only toggle the state of a door from
         * what it is currently. server will send
         * back the status of the door (for when it gets opened
         * without our interactions, too)
         */
        class DoorOpen(var entityId: Int = INVALID_ENTITY_ID)

        /**
         * toggle running status of a device
         */
        class DeviceToggle(var entityId: Int = INVALID_ENTITY_ID)

        /**
         * indicate that we are trying to attack whatever item is equipped,
         * at the given vector in world coordinates.
         */
        class PlayerEquippedItemAttack(var itemAttackType: ItemAttackType = ItemAttackType.Primary,
                                       var attackPositionWorldCoords: Vector2 = Vector2()) {

            enum class ItemAttackType {
                Primary,
                Secondary
            }
        }
    }

    object Shared {
        /// some of these are so we don't serialize an entire Sprite
        class PositionPacket {
            var pos = Vector2()
        }

        class SizePacket {
            var size = Vector2()
        }

        class DisconnectReason {
            lateinit var reason: Reason

            enum class Reason {
                VersionMismatch,
                InvalidPlayerName
            }
        }

        /**
         * Tiny(er) class to wrap a Block and send over the wire
         */
        class SingleBlock {
            constructor() {
            }

            constructor(_type: Byte, _wallType: Byte, _flags: Byte) {
                type = _type
                wallType = _wallType
                flags = _flags
            }

            internal var type: Byte = 0
            internal var wallType: Byte = 0
            internal var flags: Byte = 0

            //mesh type is not passed, but recalculated as each chunk is merged with the running world
        }

        class SingleSparseBlock {

            lateinit var block: SingleBlock
            var x: Int = 0
            var y: Int = 0

            constructor() {
            }

            constructor(x: Int, y: Int, type: Byte, wallType: Byte, flags: Byte) {
                block = SingleBlock(type, wallType, flags)
                this.x = x
                this.y = y
            }
        }

        /**
         * Incremental and sparse update of changed blocks. These are different in that they are more efficient
         * for single to double digit block counts. They store with them their block index.
         *
         *
         * Usually used for when out-of-line blocks are modified and changes need to be sent. They are queued on the server
         * so there is not one packet per tiny block update.
         */
        class SparseBlockUpdate {
            var blocks = mutableListOf<SingleSparseBlock>()
        }

        class BlockRegion {

            /**
             * uses similar logic as world
             * amount of fields per each block. e.g. every 3 bytes
             * is for each block. but we don't send mesh type
             */
            lateinit var blocks: ByteArray
            //start and end indices, inclusive(a rect)
            var x: Int = 0
            var y: Int = 0
            var x2: Int = 0
            var y2: Int = 0

            constructor() {
            }

            constructor(x: Int, y: Int, x2: Int, y2: Int) {
                this.x = x
                this.y = y
                this.x2 = x2
                this.y2 = y2
            }

            companion object {
                //different than what is in the Block class, because we don't send everything
                //over. some things are client only, some are serverside only.
                const val BLOCK_FIELD_COUNT = 4

                const val BLOCK_FIELD_INDEX_TYPE = 0
                const val BLOCK_FIELD_INDEX_WALLTYPE = 1
                const val BLOCK_FIELD_INDEX_LIGHT_LEVEL = 2
                const val BLOCK_FIELD_INDEX_FLAGS = 3
            }
        }

        enum class InventoryType {
            Hotbar,
            Inventory, //standard inventory
            Generator
        }

    }
}

object NetworkHelper {
    fun debugPacketFrequencies(receivedObject: Any,
                               debugPacketFrequencyByType: MutableMap<String, Int>) {
        val debugPacketTypeName = receivedObject.javaClass.toString()
        val current = debugPacketFrequencyByType[debugPacketTypeName]

        if (current != null) {
            debugPacketFrequencyByType.put(debugPacketTypeName, current + 1)
        } else {
            debugPacketFrequencyByType.put(debugPacketTypeName, 1)
        }
    }
}

