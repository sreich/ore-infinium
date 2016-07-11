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
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntArray
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryonet.EndPoint
import com.ore.infinium.components.*
import com.ore.infinium.util.INVALID_ENTITY_ID
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

    private fun registerComponents(kryo: Kryo) {
        //components
        kryo.register(Component::class.java)
        kryo.register(PowerDeviceComponent::class.java)
        kryo.register(ItemComponent.State::class.java)
        kryo.register(AirComponent::class.java)
        kryo.register(PowerGeneratorComponent::class.java)
        kryo.register(PowerConsumerComponent::class.java)
        kryo.register(PowerDeviceComponent::class.java)
        kryo.register(BlockComponent::class.java)
        kryo.register(FloraComponent::class.java)
        kryo.register(HealthComponent::class.java)
        kryo.register(ItemComponent::class.java)
        kryo.register(JumpComponent::class.java)
        //playercomponent is not sent. big and pointless.
        //spritecomponent is not sent. big..replaced  by module ones
        //controllablecomponent not sent
        kryo.register(ToolComponent::class.java)
        kryo.register(ToolComponent.ToolMaterial::class.java)
        kryo.register(ToolComponent.ToolType::class.java)

        kryo.register(LightComponent::class.java)
        kryo.register(VelocityComponent::class.java)
        //////////
    }

    private fun registerShared(kryo: Kryo) {
        kryo.register(Shared.DisconnectReason::class.java)
        kryo.register(Shared.DisconnectReason.Reason::class.java)
        //modular components. some components are too fucking huge and stupid to serialize automatically (like Sprite),
        //so we split up only what we need.
        kryo.register(Shared.PositionPacket::class.java)
        kryo.register(Shared.SizePacket::class.java)

        kryo.register(Shared.BlockRegion::class.java)
        kryo.register(Shared.SparseBlockUpdate::class.java)
        kryo.register(Shared.SingleSparseBlock::class.java)
        kryo.register(Shared.SingleBlock::class.java)

        kryo.register(OreBlock.BlockType::class.java)

        kryo.register(Chat.ChatSender::class.java)
        kryo.register(Shared.InventoryType::class.java)

        // primitives/builtin
        //        kryo.register(String[]::class.java)
        kryo.register(ByteArray::class.java)
        kryo.register(IntArray::class.java)
        kryo.register(Array::class.java)
        kryo.register(ArrayList::class.java)

        kryo.register(kotlin.Array<Any>::class.java)
        kryo.register(Vector2::class.java)
        kryo.register(IntArray::class.java)
        //       kryo.register(Array<Any>::class.java)
        kryo.register(Rectangle::class.java)
    }

    private fun registerServer(kryo: Kryo) {
        kryo.register(Server.EntitySpawn::class.java)
        kryo.register(Server.EntitySpawnMultiple::class.java)
        kryo.register(Server.EntityDestroyMultiple::class.java)
        kryo.register(Server.EntityKilled::class.java)
        kryo.register(Server.EntityMoved::class.java)

        kryo.register(Server.SpawnInventoryItems::class.java)
        kryo.register(Server.SpawnGeneratorInventoryItems::class.java)

        kryo.register(Server.LoadedViewportMoved::class.java)
        kryo.register(Server.PlayerSpawned::class.java)
        kryo.register(Server.ChatMessage::class.java)
    }

    private fun registerClient(kryo: Kryo) {
        kryo.register(Client.InitialClientData::class.java)

        kryo.register(Client.ChatMessage::class.java)

        kryo.register(Client.MoveInventoryItem::class.java)

        kryo.register(Client.PlayerMove::class.java)
        kryo.register(Client.BlockDigBegin::class.java)
        kryo.register(Client.BlockDigFinish::class.java)
        kryo.register(Client.BlockPlace::class.java)
        kryo.register(Client.ItemPlace::class.java)
        kryo.register(Client.PlayerEquipHotbarIndex::class.java)
        kryo.register(Client.HotbarDropItem::class.java)
        kryo.register(Client.OpenDeviceControlPanel::class.java)
        kryo.register(Client.CloseDeviceControlPanel::class.java)

        kryo.register(Client.PlayerEquippedItemAttack::class.java)
        kryo.register(Client.PlayerEquippedItemAttack.ItemAttackType::class.java)

        kryo.register(Client.EntityAttack::class.java)
    }

    object Server {
        class InitialClientData
        class ChatMessage {
            lateinit var timestamp: String
            lateinit var playerName: String
            lateinit var message: String
            lateinit var sender: Chat.ChatSender
        }

        //fixme: unneeded??
        class LoadedViewportMoved {
            lateinit var rect: Rectangle
        }

        class DestroyEntities {
            var entityId: IntArray? = null
        }

        class WorldTimeChanged {
            var hour: Int = 0
            var minute: Int = 0
            var second: Int = 0
        }

        class PlayerSpawned {
            var connectionId: Int = 0 // session local id, to be displayed
            var playerName: String? = null
            var pos = Shared.PositionPacket()
            //we don't need a size packet for player. we know how big one will be, always.
        }

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
             * applies only to generator inventory and only if one is in that slot
             */
            var fuelSourceEntity: EntitySpawn? = null
        }

        /**
         * for loading list of items which are (probably) fuel sources
         * that will be burned in the generator.
         *
         * used for when the generator's device control panel is opened.
         */
        class SpawnGeneratorInventoryItems() {
            var generatorEntityId = INVALID_ENTITY_ID

            /**
             * we know which index it gets spawned in due to @see ItemComponent.inventoryIndex
             * this is a list of only non-empty inventory slots. empty ones are not sent
             */
            var entitiesToSpawn = mutableListOf<EntitySpawn>()

            //entity in the currently-being-burned fuel source, if any
            var fuelSourceEntity: EntitySpawn? = null
        }

        /**
         * updates the current output stats for the generator,
         * visible only when the control panel is open
         */
        class UpdateGeneratorControlPanelStats() {
            var entityId = -1
            var supply = -1

            /**
             * remaining health of the fuel (percent)
             * 0 means that fuel source would be expired/removed.
             */
            var fuelHealth = 0
        }

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
        class EntityKilled {
            var entityToKill: Int? = null
        }

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
            var size = Shared.SizePacket()
            var pos = Shared.PositionPacket()

            var textureName: String = ""

            var id: Int = 0

            lateinit var components: Array<Component>
        }

        class PlayerDisconnected {
            var playerId: Int = 0
        }

        class EntityMoved {
            var id: Int = 0
            var position = Vector2()
        }
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

        class ChatMessage {
            var message: String? = null
        }

        //todo reduce data type sizes for lots of this stuff...
        class PlayerEquipHotbarIndex {
            var index: Byte = 0
        }

        class BlockDigBegin {
            var x: Int = 0
            var y: Int = 0
        }

        /**
         * sent when client knows it finished digging a block
         * at x,y successfully.
         * server will verify it is true.
         * if this packet is not sent, the server will eventually time out
         * the dig(cancel it), for that block.
         */
        class BlockDigFinish {
            var x: Int = 0
            var y: Int = 0
        }

        class BlockPlace {
            var x: Int = 0
            var y: Int = 0
        }

        class ItemPlace {
            var x: Float = 0.0f
            var y: Float = 0.0f
        }

        class PlayerMove {
            var position: Vector2? = null
        }

        /**
         * request for the player to perform
         * an attack on the given entity.
         * server will do some sanity checking
         */
        class EntityAttack {
            var id: Int = 0
        }

        /**
         * request to drop an item from the hotbar/equipped
         */
        class HotbarDropItem {
            var index: Byte = 0
        }

        class MoveInventoryItem {
            var sourceType: Shared.InventoryType? = null
            var destType: Shared.InventoryType? = null
            var sourceIndex: Byte = 0
            var destIndex: Byte = 0
        }

        /**
         * notify request, to tell server we need info
         * for this device for us to open the control panel
         * (like generator fuel source inventory info)
         *
         * server will keep track of it but we must notify when
         * we close it
         */
        class OpenDeviceControlPanel {
            var entityId: Int = INVALID_ENTITY_ID
        }

        class CloseDeviceControlPanel {
            var entityId: Int = INVALID_ENTITY_ID
        }

        /**
         * indicate that we are trying to attack whatever item is equipped,
         * at the given vector in world coordinates.
         */
        class PlayerEquippedItemAttack {

            var itemAttackType = ItemAttackType.Primary
            var attackPositionWorldCoords = Vector2()

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
        class SparseBlockUpdate() {
            var blocks = Array<SingleSparseBlock>()
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

            constructor(_x: Int, _y: Int, _x2: Int, _y2: Int) {
                x = _x
                y = _y
                x2 = _x2
                y2 = _y2
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

        ///
        enum class InventoryType {
            Hotbar,
            Inventory, //standard inventory
            Generator
        }

    }
    //is that needed????
    //    static public class PlayerHotbarInventoryItemCountChanged {
    //        int dragSourceIndex;
    //        int newCount;
    //    }

}

object NetworkHelper {
    fun debugPacketFrequencies(receivedObject: Any,
                               m_debugPacketFrequencyByType: MutableMap<String, Int>) {
        val debugPacketTypeName = receivedObject.javaClass.toString()
        val current = m_debugPacketFrequencyByType[debugPacketTypeName]

        if (current != null) {
            m_debugPacketFrequencyByType.put(debugPacketTypeName, current + 1)
        } else {
            m_debugPacketFrequencyByType.put(debugPacketTypeName, 1)
        }
    }
}

