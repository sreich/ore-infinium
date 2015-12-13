package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.ComponentMapper;
import com.artemis.annotations.Wire;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
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

/**
 * system that handled all the power circuits/wire connections, can look them up,
 * connect them, remove them, etc., and it also will process them each tick and
 * calculate their current statuses, e.g. how much electricity was generated,
 * consumed, etc...
 */
@Wire
public class PowerCircuitSystem extends BaseSystem {
    static final float WIRE_THICKNESS = 3.0f / OreWorld.PIXELS_PER_METER;

    private OreWorld m_world;

    /**
     * Contains list of each circuit in the world.
     * <p>
     * A circuit contains all the wire connections that are continuous/connected
     * in some form. Circuits would probably average 20 or so unique devices
     * But it could be much much more (and probably will be)
     * <p>
     * When devices that are on different circuits get connected, those
     * devices are merged into the same circuit
     */
    Array<PowerCircuit> m_circuits = new Array<>();

    /**
     * Either a connected entity on a circuit/wire, is a device or a generator. It is *not* both.
     * Devices consumer power, generators...generate
     */
    public class PowerCircuit {
        /**
         * List of wire connections between pairs of devices
         * <p>
         * duplicate entities may exist across all wireConnections
         * e.g. Wire1{ ent1, ent2 }, Wire2 { ent3, ent 1}, but
         * they would still be of the same circuit of course.
         * However, devices are unique across circuits. No devices can bridge multiple circuits,
         * if they do, the circuits are merged.
         * See generators, consumers
         */
        Array<PowerWireConnection> wireConnections = new Array<>();

        /**
         * List of generators for faster checking of changes/recalculations, in addition to the
         * wire connection list
         * Is disjoint from devices.
         */
        IntArray generators = new IntArray();

        /**
         * List of all the devices that consume power, connected on this circuit
         * For faster retrieval of just those, and for calculating the load usages.
         * May be disjoint from generators, but note that generators have potential to consume power as well..
         * so there *could* be generators present in here. But they should only be treated as consumers
         * (as they would have the PowerConsumerComponent, in addition to PowerGeneratorComponent
         *
         * @type s
         */
        IntArray consumers = new IntArray();

        int totalSupply;
        int totalDemand;
    }

    /**
     * Each circuit is composed of >= 1 wire connections, each wire connection is composed of
     * only 2 different devices.
     */
    public class PowerWireConnection {
        int firstEntity;
        int secondEntity;

        public PowerWireConnection(int firstEntity, int secondEntity) {
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
        /*
        * note that only the server should be the one that processes input and
        * output for generations, devices etc...the client cannot accurately calculate this each tick,
        * without desyncing at some point. the server should be the one
        * informing it of the outcomes, and the changes can be sent over the
        * wire and consumed by this system
        */
        if (m_world.isClient()) {
            return;
        }

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
     * connects two power devices together, determines how to handle data structures
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
            for (PowerWireConnection connection : circuit.wireConnections) {
                if ((connection.firstEntity == firstEntity && connection.secondEntity == secondEntity) ||
                    connection.firstEntity == secondEntity && connection.secondEntity == firstEntity) {

                    //connection exists in this circuit already, deny
                    return;
                }

                if (connection.firstEntity == firstEntity || connection.secondEntity == secondEntity ||
                    connection.firstEntity == secondEntity || connection.secondEntity == firstEntity) {
                    //one of the entities of this wire is in this connection, so it's a part of this circuit
                    //we don't care which one. we just add our wire to the mix
                    PowerWireConnection powerWireConnection = new PowerWireConnection(firstEntity, secondEntity);
                    circuit.wireConnections.add(powerWireConnection);

                    addConnection(firstEntity, secondEntity, circuit);

                    return;
                }
            }
        }

        PowerCircuit circuit = new PowerCircuit();

        //connection nonexistent in any circuits, make a new circuit
        addConnection(firstEntity, secondEntity, circuit);
        m_circuits.add(circuit);

        PowerWireConnection powerWireConnection = new PowerWireConnection(firstEntity, secondEntity);
        circuit.wireConnections.add(powerWireConnection);
    }

    /**
     * Disconnect all wireConnections pointing to this entity
     * <p>
     * used in situation such as "this device was destroyed/removed, cleanup any wireConnections that
     * connect to it.
     *
     * @param entityToDisconnect
     */
    public void disconnectAllWiresFromDevice(int entityToDisconnect) {

        for (int itCircuit = 0; itCircuit < m_circuits.size; ++itCircuit) {
            PowerCircuit circuit = m_circuits.get(itCircuit);

            for (int itWire = 0; itWire < circuit.wireConnections.size; ++itWire) {
                PowerWireConnection wireConnection = circuit.wireConnections.get(itWire);

                if (wireConnection.firstEntity == entityToDisconnect ||
                    wireConnection.secondEntity == entityToDisconnect) {
                    circuit.wireConnections.removeIndex(itWire);

                    //if we removed the last wire connection, cleanup this empty circuit
                    if (circuit.wireConnections.size == 0) {
                        m_circuits.removeIndex(itCircuit);
                    }
                }
            }
        }
    }

    /**
     * Searches for a wire in the list of circuits, removes the one under the position,
     * if one such exists.
     * Would be used in situations such as "user clicked remove on a wire, so remove it.
     *
     * @param position
     *         in world coords
     *
     * @return false if disconnect failed (no wire in range). True if it succeeded.
     */

    public boolean disconnectWireAtPosition(Vector2 position) {

        for (int itCircuits = 0; itCircuits < m_circuits.size; ++itCircuits) {
            PowerCircuit circuit = m_circuits.get(itCircuits);

            for (int itWires = 0; itWires < circuit.wireConnections.size; ++itWires) {
                PowerWireConnection connection = circuit.wireConnections.get(itWires);

                int first = connection.firstEntity;
                int second = connection.secondEntity;

                SpriteComponent firstSprite = spriteMapper.get(first);
                SpriteComponent secondSprite = spriteMapper.get(second);
                //todo..rest of the logic..try looking for an intersection between points of these
                //given a certain width..that is the width that is decided upon for wire thickness. (constant)

                Vector2 firstPosition = new Vector2(firstSprite.sprite.getX(), firstSprite.sprite.getY());
                Vector2 secondPosition = new Vector2(secondSprite.sprite.getX(), secondSprite.sprite.getY());

                float circleRadius2 = (float) Math.pow(WIRE_THICKNESS * 4, 2);
                //Vector2 circleCenter = new Vector2(position.x - 0, position.y - (PowerCircuitSystem.WIRE_THICKNESS));
                Vector2 circleCenter =
                        new Vector2(position.x - 0, position.y - (PowerCircuitSystem.WIRE_THICKNESS * 3));
                boolean intersects =
                        Intersector.intersectSegmentCircle(firstPosition, secondPosition, circleCenter, circleRadius2);
                if (intersects) {
                    //wire should be destroyed. remove it from wireConnections.
                    circuit.wireConnections.removeIndex(itWires);

                    //cleanup dead circuit, if we removed the last wire from it.
                    if (circuit.wireConnections.size == 0) {
                        m_circuits.removeIndex(itCircuits);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determine if a given point resides on a line of given thickness
     *
     * @param start
     * @param end
     * @param thickness
     * @param point
     * @param nearest
     *         temporary vector
     *
     * @return
     */

    public static boolean isOnLine(Vector2 start, Vector2 end, float thickness, Vector2 point, Vector2 nearest) {
        float length2 = start.dst2(end);

        if (length2 == 0) {
            return false; // length of line is zero!
        }

        float t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / length2;
        if (t < 0) {
            return false; // not between start and end
        }
        if (t > 1) {
            return false; // not between start and end
        }

        nearest.set(start.x + t * (end.x - start.x), start.y + t * (end.y - start.y));

        if (nearest.dst2(point) > thickness * thickness) {
            return false;
        }

        return true;
    }

    /**
     * Forms a wire connection between any 2 devices (direction does not matter).
     * Note, A single connection creates a circuit, additional wireConnections should only be a part of one circuit.
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
