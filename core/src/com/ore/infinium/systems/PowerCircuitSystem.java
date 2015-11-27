package com.ore.infinium.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.Gdx;
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

    /**
     * Either a connected entity on a circuit/wire, is a device or a generator. It is *not* both.
     * Devices consumer power, generators...generate
     */
    public class PowerCircuit {
        /**
         * duplicate entities may exist across all connections
         * e.g. Wire1{ ent1, ent2 }, Wire2 { ent3, ent 1}, but
         * they would still be of the same circuit of course.
         * However, devices are unique across circuits. No devices can bridge multiple circuits,
         * if they do, the circuits are merged. No 1 connection shall have the same device/generator at both
         * of the endpoints.
         * Mostly this is used for rendering. See generators, consumers
         */
        Array<WireConnection> connections = new Array<>();

        /**
         * List of generators for faster checking of changes/recalculations, in addition to the
         * wire connection list
         * Is disjoint from devices.
         */
        Array<Entity> generators = new Array<>();

        /**
         * List of all the devices that consume power, connected on this circuit
         * For faster retrieval of just those, and for calculating the load usages.
         * May be disjoint from generators, but generators have potential to consume power as well..
         */
        Array<Entity> consumers = new Array<>();

        int totalSupply;
        int totalDemand;
    }

    /**
     * Each circuit is composed of > 1 wire connections, each wire connection is composed of
     * only 2 devices.
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
    private ComponentMapper<PowerConsumerComponent> powerConsumerMapper = ComponentMapper.getFor(PowerConsumerComponent.class);
    private ComponentMapper<PowerGeneratorComponent> powerGeneratorMapper = ComponentMapper.getFor(PowerGeneratorComponent.class);

    public PowerCircuitSystem(World world) {
        m_world = world;
    }

    @Override
    public void addedToEngine(Engine engine) {
        Gdx.app.log("added power circuit system to engine: %s", engine.toString());
    }

    @Override
    public void removedFromEngine(Engine engine) {
    }

    @Override
    public void update(float delta) {
        for (PowerCircuit circuit : m_circuits) {
            circuit.totalDemand = 0;
            circuit.totalSupply = 0;

            for (Entity generator : circuit.generators) {
                PowerGeneratorComponent generatorComponent = powerGeneratorMapper.get(generator);
                circuit.totalSupply += generatorComponent.powerSupplyRate;
            }

            for (Entity consumer : circuit.consumers) {
                PowerConsumerComponent consumerComponent = powerConsumerMapper.get(consumer);
                circuit.totalSupply += consumerComponent.powerDemandRate;
            }

            continue;
        }
    }

    //fixme does not inform the server of these connections!!! or anything wirey for that matter.
    /**
     * connects two power consumers together, determines how to handle data structures
     * in between
     *
     * @param first
     * @param second
     */
    void connectDevices(Entity first, Entity second) {
        for (PowerCircuit circuit : m_circuits) {
            //

            //check which circuit this connection between 2 consumers belongs to
            //if none of the two consumers are in a circuit, it is a new circuit
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

    /**
     * Forms a wire connection between any 2 devices (direction does not matter).
     * Note, A single connection creates a circuit, additional connections should only be a part of one circuit.
     * @param first
     * @param second
     * @param circuit
     */
    private void addConnection(Entity first, Entity second, PowerCircuit circuit) {
        //cannot connect to a non-device
        assert powerDeviceMapper.has(first) && powerDeviceMapper.has(second);

        if (powerConsumerMapper.get(first) != null && !circuit.consumers.contains(first, true)) {
            circuit.consumers.add(first);
        }

        if (powerConsumerMapper.get(second) != null && !circuit.consumers.contains(second, true)) {
            circuit.consumers.add(second);
        }

        if (powerGeneratorMapper.get(first) != null && !circuit.generators.contains(first, true)) {
            circuit.generators.add(first);
        }

        if (powerGeneratorMapper.get(second) != null && !circuit.generators.contains(second, true)) {
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

            if (tagComponent != null && tagComponent.tag.equals("itemPlacementOverlay")) {
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
