package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.ore.infinium.OreWorld;
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
@Wire
public class PowerCircuitSystem extends BaseSystem {
    private OreWorld m_world;

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
        IntArray generators = new IntArray();

        /**
         * List of all the devices that consume power, connected on this circuit
         * For faster retrieval of just those, and for calculating the load usages.
         * May be disjoint from generators, but generators have potential to consume power as well..
         *
         * @type s
         */
        IntArray consumers = new IntArray();

        int totalSupply;
        int totalDemand;
    }

    /**
     * Each circuit is composed of > 1 wire connections, each wire connection is composed of
     * only 2 devices.
     */
    public class WireConnection {
        int firstEntity;
        int secondEntity;

        public WireConnection(int firstEntity, int secondEntity) {
            this.firstEntity = firstEntity;
            this.secondEntity = secondEntity;
        }
    }

    private ComponentMapper<PlayerComponent> playerMapper;
    private ComponentMapper<SpriteComponent> spriteMapper;
    private ComponentMapper<ItemComponent> itemMapper;
    private ComponentMapper<VelocityComponent> velocityMapper;
    private ComponentMapper<PowerDeviceComponent> powerDeviceMapper;
    private ComponentMapper<PowerConsumerComponent> powerConsumerMapper;
    private ComponentMapper<PowerGeneratorComponent> powerGeneratorMapper;

    public PowerCircuitSystem(OreWorld world) {
        m_world = world;
    }

    /**
     * Process the system.
     */
    @Override
    protected void processSystem() {
        for (PowerCircuit circuit : m_circuits) {

            circuit.totalDemand = 0;
            circuit.totalSupply = 0;

            for (int j = 0; j < circuit.generators.size; ++j) {
                int generator = circuit.generators.get(j);

                PowerGeneratorComponent generatorComponent = powerGeneratorMapper.get(generator);
                circuit.totalSupply += generatorComponent.powerSupplyRate;
            }

            for (int j = 0; j < circuit.consumers.size; ++j) {
                int consumer = circuit.consumers.get(j);

                PowerConsumerComponent consumerComponent = powerConsumerMapper.get(consumer);
                circuit.totalSupply += consumerComponent.powerDemandRate;
            }
        }
    }

    //fixme does not inform the server of these connections!!! or anything wirey for that matter.

    /**
     * connects two power consumers together, determines how to handle data structures
     * in between
     *
     * @param firstEntity
     * @param secondEntity
     */
    void connectDevices(int firstEntity, int secondEntity) {
        for (PowerCircuit circuit : m_circuits) {
            //

            //check which circuit this connection between 2 consumers belongs to
            //if none of the two consumers are in a circuit, it is a new circuit
            for (WireConnection connection : circuit.connections) {
                if ((connection.firstEntity == firstEntity && connection.secondEntity == secondEntity) ||
                    connection.firstEntity == secondEntity && connection.secondEntity == firstEntity) {

                    //connection exists in this circuit already, deny
                    return;
                }

                if (connection.firstEntity == firstEntity || connection.secondEntity == secondEntity ||
                    connection.firstEntity == secondEntity || connection.secondEntity == firstEntity) {
                    //one of the entities of this wire is in this connection, so it's a part of this circuit
                    //we don't care which one. we just add our wire to the mix
                    WireConnection wireConnection = new WireConnection(firstEntity, secondEntity);
                    circuit.connections.add(wireConnection);

                    addConnection(firstEntity, secondEntity, circuit);

                    return;
                }
            }
        }

        PowerCircuit circuit = new PowerCircuit();

        //connection nonexistent in any circuits, make a new circuit
        addConnection(firstEntity, secondEntity, circuit);
        m_circuits.add(circuit);

        WireConnection wireConnection = new WireConnection(firstEntity, secondEntity);
        circuit.connections.add(wireConnection);
    }

    /**
     * Forms a wire connection between any 2 devices (direction does not matter).
     * Note, A single connection creates a circuit, additional connections should only be a part of one circuit.
     *
     * @param firstEntity
     * @param secondEntity
     * @param circuit
     */
    private void addConnection(int firstEntity, int secondEntity, PowerCircuit circuit) {
        //cannot connect to a non-device
        assert powerDeviceMapper.has(firstEntity) && powerDeviceMapper.has(secondEntity);

        if (powerConsumerMapper.get(firstEntity) != null && !circuit.consumers.contains(firstEntity)) {
            circuit.consumers.add(firstEntity);
        }

        if (powerConsumerMapper.get(secondEntity) != null && !circuit.consumers.contains(secondEntity)) {
            circuit.consumers.add(secondEntity);
        }

        if (powerGeneratorMapper.get(firstEntity) != null && !circuit.generators.contains(firstEntity)) {
            circuit.generators.add(firstEntity);
        }

        if (powerGeneratorMapper.get(secondEntity) != null && !circuit.generators.contains(secondEntity)) {
            circuit.generators.add(secondEntity);
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

            Rectangle rectangle = new Rectangle(spriteComponent.sprite.getX() - (spriteComponent.sprite.getWidth() *
            0.5f),
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
