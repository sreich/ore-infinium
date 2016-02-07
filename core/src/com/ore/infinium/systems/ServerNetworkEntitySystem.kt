package com.ore.infinium.systems

import com.artemis.Aspect
import com.artemis.ComponentMapper
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.artemis.utils.IntBag
import com.badlogic.gdx.utils.Array
import com.ore.infinium.OreWorld
import com.ore.infinium.components.*
import java.util.*

/**
 * ***************************************************************************
 * Copyright (C) 2016 by Shaun Reich @gmail.com>                    *
 * *
 * This program is free software; you can redistribute it and/or            *
 * modify it under the terms of the GNU General Public License as           *
 * published by the Free Software Foundation; either version 2 of           *
 * the License, or (at your option) any later version.                      *
 * *
 * This program is distributed in the hope that it will be useful,          *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * GNU General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU General Public License        *
 * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire(failOnNull = false)
/**
 * system for keeping track of which entities should be on each
 * and every player/client, and which (we think/hope) are

 * entities should not be spawned manually, as this system
 * will take care of it, as well as notifying of destruction.

 * each tick it checks which entities should be added or removed
 * to the client's viewport region, compared to what we know/think
 * is already spawned on that client. and sends out appropriate net
 * commands

 */
class ServerNetworkEntitySystem(private val m_world: OreWorld) : IteratingSystem(
        Aspect.one(SpriteComponent::class.java)) {

    private lateinit var playerMapper: ComponentMapper<PlayerComponent>
    private lateinit var spriteMapper: ComponentMapper<SpriteComponent>
    private lateinit var controlMapper: ComponentMapper<ControllableComponent>
    private lateinit var itemMapper: ComponentMapper<ItemComponent>
    private lateinit var velocityMapper: ComponentMapper<VelocityComponent>
    private lateinit var jumpMapper: ComponentMapper<JumpComponent>

    private lateinit var m_networkServerSystem: NetworkServerSystem
    private lateinit var m_spatialSystem: SpatialSystem

    private val m_playerEntities = Array<PlayerEntitiesInViewport>()

    //todo store a list for each player, of entities within the viewport
    //each tick we will check if entities should be added or removed
    //on the client(client will do this automatically on their end)
    //but we will too, so we can remove it from the list of entities each client has spawned(although, maybe we
    // should instead send packets indicating this?)

    //we may want to instead query the spatial hash and compare that list to this list?

    /**
     * Checks if the entity is spawned on player client
     * this function is costly!! (todo)
     */
    fun entityExistsInPlayerView(playerEntityId: Int, entityId: Int): Boolean {
        //todo this gonna be really costly. should replace with hash map approach, likely
        val playerEntityInViewport = m_playerEntities.find { it -> it.playerEntityId == playerEntityId }

        val knownEntity = playerEntityInViewport!!.knownEntities.find { knownEntityId -> knownEntityId == entityId }

        return if (knownEntity != null) {
            true
        } else {
            false
        }
    }

    private inner class PlayerEntitiesInViewport {
        /**
         * entity id of the player whose viewport/list of spawned
         * entities it knows about
         */
        internal var playerEntityId = -1

        /**
         * we must know which entities the client has spawned, so we don't end up
         * re-sending them *all*, when his viewport moves a bit. so using this,
         * we know which ones the client has spawned, we know which ones need to be spawned
         *
         *
         * on client disconnection, all entities in here, associated with his player,
         * will be removed. this does not mean they will actually be removed from the world,
         * since this is just a "which entities does this client have in viewport"
         */
        internal var knownEntities = ArrayList<Int>()
    }

    override fun initialize() {
        m_networkServerSystem.addConnectionListener(ConnectionListener())
    }

    private inner class ConnectionListener : NetworkServerSystem.NetworkServerConnectionListener {
        override fun playerDisconnected(playerEntityId: Int) {
            for (i in 0..m_playerEntities.size - 1) {
                val playerEntitiesInViewport = m_playerEntities.get(i)
                //remove all entities for this player, since he's disconnecting
                if (playerEntitiesInViewport.playerEntityId == playerEntityId) {
                    m_playerEntities.removeIndex(i)
                }
            }
        }

        override fun playerConnected(playerEntityId: Int) {
            val playerEntitiesInViewport = PlayerEntitiesInViewport()
            playerEntitiesInViewport.playerEntityId = playerEntityId

            m_playerEntities.add(playerEntitiesInViewport)
        }
    }

    override fun removed(entityId: Int) {

        //i think we can assume at this point, that the client will end up knowing in some way or another,
        //that this entity has died (e.g. if it is a player killed message, or an enemy killed,
        // or something gets destroyed), the events that send out that info should be giving the
        //appropriate information, since more context may need to be specified, other than
        //"thing removed"

        // remove entity from client list

        //hack, dunno if this is  even needed..it's more polling based than event based, for player viewport spawns
        /*
        for (playerEntity in m_playerEntities) {
            for (i in 0..playerEntity.knownEntities.size - 1) {
                val ent = playerEntity.knownEntities.get(i)

                if (ent == entityId) {
                    playerEntity.knownEntities.removeAt(i)
                }
            }
        }
        */
    }

    override fun process(entityId: Int) {
        val spriteComponent = spriteMapper.get(entityId)

        //for each player, check their list of entities spawned in their viewport,
        //compare with our list of entities that actually exist (spatial query)
        for (playerEntity in m_playerEntities) {
            //playerEntity.knownEntities;

            val playerComponent = playerMapper.get(playerEntity.playerEntityId)
            val viewport = playerComponent.loadedViewport.blockRegionInViewport()

            //get the entities that actually exist in this viewport
            val fill = IntBag()
            m_spatialSystem.m_tree.get(fill, viewport.x.toFloat(), viewport.y.toFloat(), viewport.width.toFloat(),
                                       viewport.height.toFloat())

            //hack copy to intarray only because the quadtree uses an intbag
            val entitiesInRegion = ArrayList<Int>()

            for (i in (0..fill.size() - 1)) {
                entitiesInRegion.add(fill.get(i))
            }


            //entity doesn't exist in known entities, but does in actual. send spawn
            val entitiesToSpawn = entitiesInRegion.filter { entityInRegion ->
                !playerEntity.knownEntities.contains(entityInRegion) &&
                        //hack ignore players for now, we don't spawn them via this mechanisms..it'd get hairy
                        //gotta rethink player spawn/destroying
                        !playerMapper.has(entityInRegion)
            }

            //list of entities we'll need to tell the client we no longer want him to have
            //remove from known, tell client he needs to delete that.
            val entitiesToDestroy = playerEntity.knownEntities.filter { knownEntity ->
                !entitiesInRegion.contains(knownEntity) && !playerMapper.has(knownEntity)
            }

            //update  our status
            playerEntity.knownEntities.addAll(entitiesToSpawn)
            playerEntity.knownEntities.removeAll(entitiesToDestroy)

            ////////////////// debug testing, to ensure validity
            val hashSet = HashSet<Int>()
            for (i in 0..entitiesToSpawn.size - 1) {
                hashSet.add(entitiesToSpawn[i])
            }
            assert(entitiesToSpawn.size == hashSet.size) { "ENTITIES TO SPAWN HAD DUPES" }

            hashSet.clear()
            for (i in 0..entitiesToDestroy.size - 1) {
                hashSet.add(entitiesToDestroy[i])
            }
            assert(entitiesToDestroy.size == hashSet.size) { "ENTITIES TO destroy HAD DUPES" }

            hashSet.clear()

            for (i in 0..playerEntity.knownEntities.size - 1) {
                hashSet.add(playerEntity.knownEntities[i])
            }

            for (i in 0..entitiesToSpawn.size - 1) {
                if (hashSet.contains(entitiesToSpawn[i])) {
                } else {
                    assert(false)
                }
            }

            ////////////////////

            if (entitiesToDestroy.size > 0) {
                OreWorld.log("servernetworkentitysystem",
                             "sending DestroyMultipleEntities: " + entitiesToDestroy.toString())
                m_networkServerSystem.sendDestroyMultipleEntities(entitiesToDestroy,
                                                                  playerComponent.connectionPlayerId)
            }

            if (entitiesToSpawn.size > 0) {
                OreWorld.log("servernetworkentitysystem",
                             "sending SpawnMultipleEntities: " + entitiesToSpawn.toString())
                //send what is remaining...these are entities the client doesn't yet have, we send them in a batch
                m_networkServerSystem.sendSpawnMultipleEntities(entitiesToSpawn,
                                                                playerComponent.connectionPlayerId)
            }

        }
    }
}
