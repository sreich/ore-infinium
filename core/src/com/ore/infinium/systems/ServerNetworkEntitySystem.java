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
        IntArray entitiesSpawned = new IntArray(false, 16);
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
            for (int i = 0; i < playerEntity.entitiesSpawned.size; i++) {
                int ent = playerEntity.entitiesSpawned.get(i);

                if (ent == entityId) {
                    playerEntity.entitiesSpawned.removeIndex(i);
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
            //playerEntity.entitiesSpawned;

            PlayerComponent playerComponent = playerMapper.get(playerEntity.playerEntityId);
            LoadedViewport.PlayerViewportBlockRegion viewport = playerComponent.loadedViewport.blockRegionInViewport();

            //get the entities that actually exist in this viewport
            IntBag fill = new IntBag();
            m_spatialSystem.m_tree.get(fill, viewport.x, viewport.y, viewport.width, viewport.height);

            //hack copy to intarray only because the quadtree uses an intbag
            IntArray entitiesInRegion = new IntArray(false, 100);
            for (int i = 0; i < fill.size(); i++) {
                entitiesInRegion.add(fill.get(i));
            }

            //remove the set of entities we think we have spawned, from the ones that are actually there.
            entitiesInRegion.removeAll(playerEntity.entitiesSpawned);

            //add these new ones in..
            playerEntity.entitiesSpawned.addAll(entitiesInRegion);

            if (entitiesInRegion.size > 0) {
                //send what is remaining...these are entities the client doesn't yet have, we send them in a batch
                m_networkServerSystem.sendSpawnMultipleEntities(entitiesInRegion, playerComponent.connectionPlayerId);
            }
        }
    }
}
