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
import com.esotericsoftware.kryonet.EndPoint
import com.ore.infinium.components.*
import java.util.*

object Network {
    const val PORT = 54553
    const val bufferObjectSize = 255032
    const val bufferWriteSize = 250536

    // This registers objects that are going to be sent over the network.
    fun register(endPoint: EndPoint) {
        val kryo = endPoint.kryo
        kryo.register(InitialClientDataFromClient::class.java)

        kryo.register(ChatMessageFromClient::class.java)
        kryo.register(ChatMessageFromServer::class.java)
        kryo.register(Chat.ChatSender::class.java)

        kryo.register(PlayerMoveInventoryItemFromClient::class.java)
        kryo.register(Inventory.InventoryType::class.java)
        kryo.register(DisconnectReason::class.java)
        kryo.register(DisconnectReason.Reason::class.java)
        kryo.register(PlayerSpawnedFromServer::class.java)

        kryo.register(PlayerMoveFromClient::class.java)
        kryo.register(BlockDigBeginFromClient::class.java)
        kryo.register(BlockDigFinishFromClient::class.java)
        kryo.register(BlockPlaceFromClient::class.java)
        kryo.register(ItemPlaceFromClient::class.java)
        kryo.register(PlayerEquipHotbarIndexFromClient::class.java)
        kryo.register(HotbarDropItemFromClient::class.java)
        kryo.register(LoadedViewportMovedFromServer::class.java)

        kryo.register(EntityAttackFromClient::class.java)
        kryo.register(EntitySpawnFromServer::class.java)
        kryo.register(EntitySpawnMultipleFromServer::class.java)
        kryo.register(EntityDestroyMultipleFromServer::class.java)
        kryo.register(EntityKilledFromServer::class.java)
        kryo.register(EntityMovedFromServer::class.java)

        kryo.register(PlayerSpawnHotbarInventoryItemFromServer::class.java)

        //modular components. some components are too fucking huge and stupid to serialize automatically (like Sprite),
        //so we split up only what we need.
        kryo.register(PositionPacket::class.java)
        kryo.register(SizePacket::class.java)

        kryo.register(BlockRegion::class.java)
        kryo.register(SparseBlockUpdate::class.java)
        kryo.register(SingleSparseBlock::class.java)
        kryo.register(SingleBlock::class.java)
        kryo.register(OreBlock.BlockType::class.java)

        //components
        kryo.register(Component::class.java)
        kryo.register(PowerDeviceComponent::class.java)
        kryo.register(ItemComponent.State::class.java)
        kryo.register(AirComponent::class.java)
        kryo.register(AirGeneratorComponent::class.java)
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

    class InitialClientDataFromClient {

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

    class InitialClientDataFromServer

    class ChatMessageFromClient {
        var message: String? = null
    }

    class ChatMessageFromServer {
        var timestamp: String ? = null
        var playerName: String ? = null
        var message: String ? = null
        var sender: Chat.ChatSender ? = null
    }

    //fixme: unneeded??
    class LoadedViewportMovedFromServer {
        var rect: Rectangle? = null
    }

    //todo reduce data type sizes for lots of this stuff...
    class PlayerEquipHotbarIndexFromClient {
        var index: Byte = 0
    }

    class DestroyEntitiesFromServer {
        var entityId: IntArray? = null
    }

    class WorldTimeChangedFromServer {
        var hour: Int = 0
        var minute: Int = 0
        var second: Int = 0
    }

    class BlockDigBeginFromClient {
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
    class BlockDigFinishFromClient {
        var x: Int = 0
        var y: Int = 0
    }

    class BlockPlaceFromClient {
        var x: Int = 0
        var y: Int = 0
    }

    class ItemPlaceFromClient {
        var x: Float = 0.0f
        var y: Float = 0.0f
    }

    class PlayerSpawnedFromServer {
        var connectionId: Int = 0 // session local id, to be displayed
        var playerName: String? = null
        var pos = PositionPacket()
        //we don't need a size packet for player. we know how big one will be, always.
    }

    //is that needed????
    //    static public class PlayerHotbarInventoryItemCountChanged {
    //        int dragSourceIndex;
    //        int newCount;
    //    }

    //todo make itemcomponent.inventoryIndex transient, send that instead through the packets?
    class PlayerSpawnHotbarInventoryItemFromServer {
        var size = SizePacket()

        // <sprite

        var textureName: String? = null
        // </sprite

        //pos not sent

        var id: Int = 0
        var components: Array<Component>? = null

        /**
         * true if this was spawn because a user picked up an item.
         * Because when the user picks up an item, the item is destroyed(server, client),
         * and then a spawn message for a new one is sent, wherever that may be.
         */
        var causedByPickedUpItem = false
    }

    /**
     * sent when the stats of a power circuit gets changed,
     * or when it needs an initial update
     */
    class PowerStatsFromServer {
        var supply: Int = -1
        var demand: Int = -1
    }

    /**
     * Indicates this entity got killed in the world.
     * (attacked by something, and health hit 0)
     *
     */
    class EntityKilledFromServer {
        var entityToKill: Int? = null
    }

    class EntitySpawnMultipleFromServer {
        var entitySpawn = mutableListOf<EntitySpawnFromServer>()
    }

    /**
     * Tells client to destroy certain entities that it shouldn't have
     * spawned anymore (outside of players region). The entities
     * probably still exist in the world on the server, as well
     * as possibly on other clients.
     *
     * @see EntityKilledFromServer
     */
    class EntityDestroyMultipleFromServer {
        var entitiesToDestroy: List<Int>? = null
    }

    class EntitySpawnFromServer {
        var size = SizePacket()
        var pos = PositionPacket()

        var textureName: String? = null

        var id: Int = 0

        var components: Array<Component>? = null
    }

    /// some of these are so we don't serialize an entire Sprite
    class PositionPacket {
        var pos = Vector2()
    }

    class SizePacket {
        var size = Vector2()
    }
    ///

    class PlayerDisconnectedFromServer {
        var playerId: Int = 0
    }

    class PlayerMoveFromClient {
        var position: Vector2? = null
    }

    /**
     * request for the player to perform
     * an attack on the given entity.
     * server will do some sanity checking
     */
    class EntityAttackFromClient {
        var id: Int = 0
    }

    class EntityMovedFromServer {
        var id: Int = 0
        var position: Vector2? = null
    }

    /**
     * request to drop an item from the hotbar/equipped
     */
    class HotbarDropItemFromClient {
        var index: Byte = 0
    }

    class DisconnectReason {
        var reason: Reason? = null

        enum class Reason {
            VersionMismatch,
            InvalidPlayerName
        }
    }

    class PlayerMoveInventoryItemFromClient {
        var sourceType: Inventory.InventoryType? = null
        var destType: Inventory.InventoryType? = null
        var sourceIndex: Byte = 0
        var destIndex: Byte = 0
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

        public var block: SingleBlock? = null
        public var x: Int = 0
        public var y: Int = 0

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
        var blocks = Array<Network.SingleSparseBlock>()
    }

    class BlockRegion {

        /**
         * uses similar logic as world
         * amount of fields per each block. e.g. every 3 bytes
         * is for each block. but we don't send mesh type
         */
        var blocks: ByteArray? = null
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

