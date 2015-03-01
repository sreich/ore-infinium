package com.ore.infinium.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.utils.Array;
import com.ore.infinium.World;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                    *
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
public class PowerCircuitSystem extends EntitySystem {
    private World m_world;

    /**
     * Contains each circuit
     * A circuit contains all the wire connections that are continuous/connected
     * in some form. Circuits would probably average 20 or so unique devices
     * But it could be much much more (and probably will be)
     */
    Array<PowerCircuit> m_circuits = new Array<>();

    public class PowerCircuit {
        /**
         * duplicate entities may exist across all connections
         * e.g. Wire1{ ent1, ent2 }, Wire2 { ent3, ent 1}, but
         * they would still be of the same circuit of course.
         * However, devices are unique across circuits. No devices can bridge multiple circuits,
         * if they do, the circuits are merged.
         * Mostly this is used for rendering. See generators, devices
         */
        Array<WireConnection> connections = new Array<>();
        /**
         * List of generators for faster checking of changes/recalculations, in addition to the
         * wire connection list
         */
        Array<Entity> generators = new Array<>();
        /**
         * List of devices connected on this circuit, in addition to the wire connections.
         * For faster retrieval of just devices, and for calculating the load usages.
         */
        Array<Entity> devices = new Array<>();

        int totalSupply;
        int totalDemand;
    }

    /**
     * Each circuit is composed of
     */
    public class WireConnection {
        Entity first;
        Entity second;

        public WireConnection(Entity first, Entity second) {
            this.first = first;
            this.second = second;
        }
    }

    private ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private ComponentMapper<SpriteComponent> spriteMapper = ComponentMapper.getFor(SpriteComponent.class);
    private ComponentMapper<ItemComponent> itemMapper = ComponentMapper.getFor(ItemComponent.class);
    private ComponentMapper<VelocityComponent> velocityMapper = ComponentMapper.getFor(VelocityComponent.class);
    private ComponentMapper<TagComponent> tagMapper = ComponentMapper.getFor(TagComponent.class);
    private ComponentMapper<PowerDeviceComponent> powerDeviceMapper = ComponentMapper.getFor(PowerDeviceComponent.class);
    private ComponentMapper<PowerGeneratorComponent> powerGeneratorMapper = ComponentMapper.getFor(PowerGeneratorComponent.class);

    public PowerCircuitSystem(World world) {
        m_world = world;
    }

    public void addedToEngine(Engine engine) {
    }

    public void removedFromEngine(Engine engine) {
    }

    /**
     * connects two power devices together, determines how to handle data structures
     * in between
     *
     * @param first
     * @param second
     */
    void connectDevices(Entity first, Entity second) {
        for (PowerCircuit circuit : m_circuits) {
            //

            //check which circuit this connection between 2 devices belongs to
            //if none of the two devices are in a circuit, it is a new circuit
            for (WireConnection connection : circuit.connections) {
                if ((connection.first == first && connection.second == second) ||
                        connection.first == second && connection.second == first) {

                    //connection exists in this circuit already, deny
                    return;
                }

                if (connection.first == first || connection.second == second
                        || connection.first == second || connection.second == first) {
                    //one of the entities of this wire is in this connection, so it's a part of this circuit
                    //we don't care which one. we just add our wire to the mix
                    WireConnection wireConnection = new WireConnection(first, second);
                    circuit.connections.add(wireConnection);

                    addConnection(first, second, circuit);

                    return;
                }
            }
        }

        PowerCircuit circuit = new PowerCircuit();

        //connection nonexistent in any circuits, make a new circuit
        addConnection(first, second, circuit);
        m_circuits.add(circuit);

        WireConnection wireConnection = new WireConnection(first, second);
        circuit.connections.add(wireConnection);
    }

    private void addConnection(Entity first, Entity second, PowerCircuit circuit) {
        if (powerDeviceMapper.get(first) != null) {
            circuit.devices.add(first);
        }

        if (powerDeviceMapper.get(second) != null) {
            circuit.devices.add(second);
        }

        if (powerGeneratorMapper.get(first) != null) {
            circuit.generators.add(first);
        }

        if (powerGeneratorMapper.get(second) != null) {
            circuit.generators.add(second);
        }
    }

//todo sufficient until we get a spatial hash or whatever

    /*
    private Entity entityAtPosition(Vector2 pos) {

        ImmutableArray<Entity> entities = m_world.engine.getEntitiesFor(Family.all(PowerComponent.class).get());
        SpriteComponent spriteComponent;
        TagComponent tagComponent;
        for (int i = 0; i < entities.size(); ++i) {
            tagComponent = tagMapper.get(entities.get(i));

            if (tagComponent != null && tagComponent.tag.equals("itemPlacementGhost")) {
                continue;
            }

            spriteComponent = spriteMapper.get(entities.get(i));

            Rectangle rectangle = new Rectangle(spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() * 0.5f),
                    spriteComponent.sprite.getY() - (spriteComponent.sprite.getHeight() * 0.5f),
                    spriteComponent.sprite.getWidth(), spriteComponent.sprite.getHeight());

            if (rectangle.contains(pos)) {
                return entities.get(i);
            }
        }

        return null;
    }

    public void update(float delta) {

    }
    */
}
