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

package com.ore.infinium.systems.server

import com.artemis.Aspect
import com.artemis.annotations.Wire
import com.artemis.systems.IteratingSystem
import com.artemis.utils.IntBag
import com.ore.infinium.OreWorld
import com.ore.infinium.components.PlayerComponent
import com.ore.infinium.components.SpriteComponent
import com.ore.infinium.systems.SpatialSystem
import com.ore.infinium.util.indices
import com.ore.infinium.util.mapper
import com.ore.infinium.util.require
import com.ore.infinium.util.system

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
class ServerNetworkEntitySystem(private val oreWorld: OreWorld) : IteratingSystem(Aspect.all()) {

    private val mSprite by require<SpriteComponent>()
    private val mPlayer by mapper<PlayerComponent>()

    private val serverNetworkSystem by system<ServerNetworkSystem>()
    private val spatialSystem by system<SpatialSystem>()

    private val playerEntities = mutableListOf<PlayerEntitiesInViewport>()

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
        val playerEntityInViewport = playerEntities.find { it -> it.playerEntityId == playerEntityId }

        val knownEntity = playerEntityInViewport!!.knownEntities.find { knownEntityId -> knownEntityId == entityId }

        return knownEntity != null
    }

    private inner class PlayerEntitiesInViewport(
            /**
             * entity id of the player whose viewport/list of spawned
             * entities it knows about
             */
            var playerEntityId: Int = -1) {

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
        internal var knownEntities = mutableListOf<Int>()
    }

    override fun initialize() {
        serverNetworkSystem.addConnectionListener(ConnectionListener())
    }

    private inner class ConnectionListener : ServerNetworkSystem.NetworkServerConnectionListener {
        override fun playerDisconnected(playerEntityId: Int) {
            playerEntities.removeAll { playerEntities ->
                //remove all entity 'copies' for this player, since he's disconnecting
                playerEntities.playerEntityId == playerEntityId
            }
        }

        override fun playerConnected(playerEntityId: Int) {
            val playerEntitiesInViewport = PlayerEntitiesInViewport(playerEntityId)
            playerEntities.add(playerEntitiesInViewport)
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
        for (playerEntity in playerEntities) {
            for (i in playerEntity.knownEntities.indices) {
                val ent = playerEntity.knownEntities.get(i)

                if (ent == entityId) {
                    playerEntity.knownEntities.removeAt(i)
                }
            }
        }
        */
    }

    override fun process(entityId: Int) {
        val spriteComponent = mSprite.get(entityId)

        //for each player, check their list of entities spawned in their viewport,
        //compare with our list of entities that actually exist (spatial query)
        for (playerEntity in playerEntities) {
            //playerEntity.knownEntities;

            val playerComponent = mPlayer.get(playerEntity.playerEntityId)
            val viewport = playerComponent.loadedViewport.blockRegionInViewport()

            //get the entities that actually exist in this viewport
            val fill = IntBag()
            spatialSystem.quadTree.get(fill, viewport.x.toFloat(), viewport.y.toFloat(), viewport.width.toFloat(),
                                       viewport.height.toFloat())

            //hack copy to intarray only because the quadtree uses an intbag
            val entitiesInRegion = mutableListOf<Int>()

            for (i in fill.indices) {
                entitiesInRegion.add(fill.get(i))
            }

            //entity doesn't exist in known entities, but does in actual. send spawn
            val entitiesToSpawn = entitiesInRegion.filter { entityInRegion ->
                !playerEntity.knownEntities.contains(entityInRegion) &&
                        //hack ignore players for now, we don't spawn them via this mechanisms..it'd get hairy
                        //gotta rethink player spawn/destroying
                        !mPlayer.has(entityInRegion)
            }

            //list of entities we'll need to tell the client we no longer want him to have
            //remove from known, tell client he needs to delete that.
            val entitiesToDestroy = playerEntity.knownEntities.filter { knownEntity ->
                !entitiesInRegion.contains(knownEntity) && !mPlayer.has(knownEntity)
            }

            //update  our status
            playerEntity.knownEntities.addAll(entitiesToSpawn)
            playerEntity.knownEntities.removeAll(entitiesToDestroy)

            ////////////////////

            if (entitiesToDestroy.size > 0) {
                OreWorld.log("servernetworkentitysystem",
                             "sending DestroyMultipleEntities: " + entitiesToDestroy.toString())
                serverNetworkSystem.sendDestroyMultipleEntities(entitiesToDestroy,
                                                                  playerComponent.connectionPlayerId)
            }

            if (entitiesToSpawn.size > 0) {
                OreWorld.log("servernetworkentitysystem",
                             "sending SpawnMultipleEntities: " + entitiesToSpawn.toString())
                //send what is remaining...these are entities the client doesn't yet have, we send them in a batch
                serverNetworkSystem.sendSpawnMultipleEntities(entitiesToSpawn,
                                                                playerComponent.connectionPlayerId)
            }

        }
    }
}
