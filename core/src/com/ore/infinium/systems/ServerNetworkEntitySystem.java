package com.ore.infinium.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.artemis.systems.IteratingSystem;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.ore.infinium.LoadedViewport;
import com.ore.infinium.OreWorld;
import com.ore.infinium.components.*;

import java.util.HashSet;

/**
 * ***************************************************************************
 * Copyright (C) 2016 by Shaun Reich <sreich02@gmail.com>                    *
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.    *
 * ***************************************************************************
 */
@Wire(failOnNull = false)
/**
 * system for keeping track of which entities should be on each
 * and every player/client, and which (we think/hope) are
 *
 * entities should not be spawned manually, as this system
 * will take care of it, as well as notifying of destruction.
 *
 * each tick it checks which entities should be added or removed
 * to the client's viewport region, compared to what we know/think
 * is already spawned on that client. and sends out appropriate net
 * commands
 *
 */ public class ServerNetworkEntitySystem extends IteratingSystem {
    private OreWorld m_world;

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ControllableComponent> controlMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<JumpComponent> jumpMapper;

    private NetworkServerSystem m_networkServerSystem;
    private SpatialSystem m_spatialSystem;

    private Array<PlayerEntitiesInViewport> m_playerEntities = new Array<>();

    //todo store a list for each player, of entities within the viewport
    //each tick we will check if entities should be added or removed
    //on the client(client will do this automatically on their end)
    //but we will too, so we can remove it from the list of entities each client has spawned(although, maybe we
    // should instead send packets indicating this?)

    //we may want to instead query the spatial hash and compare that list to this list?

    private class PlayerEntitiesInViewport {
        /**
         * entity id of the player whose viewport/list of spawned
         * entities it knows about
         */
        int playerEntityId = -1;

        /**
         * we must know which entities the client has spawned, so we don't end up
         * re-sending them *all*, when his viewport moves a bit. so using this,
         * we know which ones the client has spawned, we know which ones need to be spawned
         * <p>
         * on client disconnection, all entities in here, associated with his player,
         * will be removed. this does not mean they will actually be removed from the world,
         * since this is just a "which entities does this client have in viewport"
         */
        IntArray knownEntities = new IntArray();
    }

    public ServerNetworkEntitySystem(OreWorld world) {
        super(Aspect.one(SpriteComponent.class));

        m_world = world;
    }

    @Override
    protected void initialize() {
        m_networkServerSystem.addConnectionListener(new ConnectionListener());
    }

    private class ConnectionListener implements NetworkServerSystem.NetworkServerConnectionListener {
        @Override
        public void playerDisconnected(int playerEntityId) {
            for (int i = 0; i < m_playerEntities.size; i++) {
                PlayerEntitiesInViewport playerEntitiesInViewport = m_playerEntities.get(i);
                //remove all entities for this player, since he's disconnecting
                if (playerEntitiesInViewport.playerEntityId == playerEntityId) {
                    m_playerEntities.removeIndex(i);
                }
            }
        }

        @Override
        public void playerConnected(int playerEntityId) {
            PlayerEntitiesInViewport playerEntitiesInViewport = new PlayerEntitiesInViewport();
            playerEntitiesInViewport.playerEntityId = playerEntityId;

            m_playerEntities.add(playerEntitiesInViewport);
        }
    }

    @Override
    protected void removed(int entityId) {

        //i think we can assume that the client will end up knowing in some way or another,
        //that this entity has died (e.g. if it is a player killed message, or an enemy killed,
        // or something gets destroyed), the events that send out that info should be giving the
        //appropriate information, since more context may need to be specified, other than
        //"thing removed"

        // remove entity from client list

        for (PlayerEntitiesInViewport playerEntity : m_playerEntities) {
            for (int i = 0; i < playerEntity.knownEntities.size; i++) {
                int ent = playerEntity.knownEntities.get(i);

                if (ent == entityId) {
                    playerEntity.knownEntities.removeIndex(i);
                }
            }
        }
    }

    @Override
    protected void process(int entityId) {
        SpriteComponent spriteComponent = spriteMapper.get(entityId);

        //for each player, check their list of entities spawned in their viewport,
        //compare with our list of entities that actually exist (spatial query)
        for (PlayerEntitiesInViewport playerEntity : m_playerEntities) {
            //playerEntity.knownEntities;

            PlayerComponent playerComponent = playerMapper.get(playerEntity.playerEntityId);
            LoadedViewport.PlayerViewportBlockRegion viewport = playerComponent.loadedViewport.blockRegionInViewport();

            //get the entities that actually exist in this viewport
            IntBag fill = new IntBag();
            m_spatialSystem.m_tree.get(fill, viewport.getX(), viewport.getY(), viewport.getWidth(),
                                       viewport.getHeight());

            //hack copy to intarray only because the quadtree uses an intbag
            IntArray entitiesInRegion = new IntArray(false, 100);
            for (int i = 0; i < fill.size(); i++) {
                entitiesInRegion.add(fill.get(i));
            }

            //list of entities we'll need to tell the client we no longer want him to have
            IntArray entitiesToDestroy = new IntArray();
            IntArray entitiesToSpawn = new IntArray();

            //entity doesn't exist in known entities, but does in actual. send spawn, add to known list
            for (int j = 0; j < entitiesInRegion.size; j++) {
                final int entityInRegion = entitiesInRegion.get(j);

                boolean entityFoundInKnown = false;
                for (int i = 0; i < playerEntity.knownEntities.size; i++) {
                    final int entityKnown = playerEntity.knownEntities.get(i);

                    //it is known
                    if (entityKnown == entityInRegion) {
                        entityFoundInKnown = true;
                        break;
                    }
                }

                if (!entityFoundInKnown) {
                    if (playerMapper.has(entityInRegion)) {
                        //hack gotta rethink player spawn/destroying
                        continue;
                    }

                    //add to spawn list
                    entitiesToSpawn.add(entityInRegion);
                    playerEntity.knownEntities.add(entityInRegion);
                }
            }

            //entity exists in known entities (spawned on client), but not in actual. (moved offscreen)
            //remove from known, tell client he needs to delete that.
            for (int i = 0; i < playerEntity.knownEntities.size; i++) {
                final int entityKnown = playerEntity.knownEntities.get(i);

                boolean entityFoundInRegion = false;
                for (int j = 0; j < entitiesInRegion.size; j++) {
                    final int entityInRegion = entitiesInRegion.get(j);

                    if (entityKnown == entityInRegion) {
                        entityFoundInRegion = true;
                        break;
                    }
                }

                if (!entityFoundInRegion) {
                    if (playerMapper.has(entityKnown)) {
                        //hack gotta rethink player spawn/destroying
                        continue;
                    }

                    //exists on client still, but we know it shouldn't (possibly went offscreen)
                    //add to list to tell it to destroy
                    entitiesToDestroy.add(entityKnown);

                    //assume client will delete it now.
                    playerEntity.knownEntities.removeIndex(i);
                }

            }

            ////////////////// debug testing, to ensure validity
            HashSet<Integer> hashSet = new HashSet<>();
            for (int i = 0; i < entitiesToSpawn.size; ++i) {
                hashSet.add(entitiesToSpawn.get(i));
            }
            assert entitiesToSpawn.size == hashSet.size() : "ENTITIES TO SPAWN HAD DUPES";

            hashSet.clear();
            for (int i = 0; i < entitiesToDestroy.size; ++i) {
                hashSet.add(entitiesToDestroy.get(i));
            }
            assert entitiesToDestroy.size == hashSet.size() : "ENTITIES TO destroy HAD DUPES";

            hashSet.clear();

            for (int i = 0; i < playerEntity.knownEntities.size; i++) {
                hashSet.add(playerEntity.knownEntities.get(i));
            }

            for (int i = 0; i < entitiesToSpawn.size; i++) {
                if (hashSet.contains(entitiesToSpawn.get(i))) {
                } else {
                    assert false;
                }
            }

            ////////////////////

            if (entitiesToDestroy.size > 0) {
                OreWorld.log("servernetworkentitysystem",
                             "sending DestroyMultipleEntities: " + entitiesToDestroy.toString());
                m_networkServerSystem.sendDestroyMultipleEntities(entitiesToDestroy,
                                                                  playerComponent.connectionPlayerId);
            }

            if (entitiesToSpawn.size > 0) {
                OreWorld.log("servernetworkentitysystem",
                             "sending SpawnMultipleEntities: " + entitiesToSpawn.toString());
                //send what is remaining...these are entities the client doesn't yet have, we send them in a batch
                m_networkServerSystem.sendSpawnMultipleEntities(entitiesToSpawn, playerComponent.connectionPlayerId);
            }

        }
    }
}
