/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich @gmail.com>                   *
 * Adopted from previously MIT-licensed code, by:

 * @author piotr-j
 * *
 * @author Daan van Yperen
 * * *
 * * This program is free software; you can redistribute it and/or            *
 * * modify it under the terms of the GNU General Public License as           *
 * * published by the Free Software Foundation; either version 2 of           *
 * * the License, or (at your option) any later version.                      *
 * * *
 * * This program is distributed in the hope that it will be useful,          *
 * * but WITHOUT ANY WARRANTY; without even the implied warranty of           *
 * * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the            *
 * * GNU General Public License for more details.                             *
 * * *
 * * You should have received a copy of the GNU General Public License        *
 * * along with this program.  If not, see //www.gnu.org/licenses/>.    *
 * * ***************************************************************************
 */

package com.ore.infinium.systems

import com.artemis.BaseSystem
import com.artemis.SystemInvocationStrategy
import com.artemis.utils.Bag
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.TimeUtils

class GameLoopSystemInvocationStrategy
/**
 * @param msPerTick
 * *         desired ms per tick you want the logic systems to run at.
 * *         Rendering is unbounded/probably bounded by libgdx's
 * *         DesktopLauncher
 */
(msPerTick: Int, private val m_isServer: Boolean) : SystemInvocationStrategy() {

    //systems marked as indicating to be run only during the logic section of the loop
    private val m_renderSystems = Array<SystemAndProfiler>()
    private val m_logicSystems = Array<SystemAndProfiler>()

    private inner class SystemAndProfiler(internal var system: BaseSystem/*, internal var profiler: SystemProfiler?*/)

    private var m_accumulator: Long = 0

    //delta time
    private val m_nsPerTick: Long
    private var m_currentTime = System.nanoTime()

    private var m_systemsSorted: Boolean = false

//    protected lateinit var frameProfiler: SystemProfiler
    private var initialized = false

    init {
        m_nsPerTick = TimeUtils.millisToNanos(msPerTick.toLong())
    }

    private fun addSystems(systems: Bag<BaseSystem>) {
        if (!m_systemsSorted) {
            val systemsData = systems.data
            for (i in 0..systems.size() - 1) {
                val system = systemsData[i] as BaseSystem
                if (system is RenderSystemMarker) {
                    //m_renderSystems.add(SystemAndProfiler(system, createSystemProfiler(system)))
                    m_renderSystems.add(SystemAndProfiler(system))
                } else {
                    //m_logicSystems.add(SystemAndProfiler(system, createSystemProfiler(system)))
                    m_logicSystems.add(SystemAndProfiler(system))
                }
            }
        }
    }

    override fun initialize() {
        if (!m_isServer) {
     //       createFrameProfiler()
        }
    }

    /*
    private fun createFrameProfiler() {
        frameProfiler = SystemProfiler.create("Frame Profiler")
        frameProfiler.setColor(1f, 1f, 1f, 1f)
    }

    private fun processProfileSystem(profiler: SystemProfiler?, system: BaseSystem) {
        profiler?.start()

        system.process()

        profiler?.stop()
    }

    private fun createSystemProfiler(system: BaseSystem): SystemProfiler? {
        var old: SystemProfiler? = null

        if (!m_isServer) {
            old = SystemProfiler.getFor(system)
            if (old == null) {
                old = SystemProfiler.createFor(system, world)
            }
        }

        return old
    }
    */

    override fun process(systems: Bag<BaseSystem>) {

        if (!m_isServer) {
        //    frameProfiler.start()
        }

        //fixme isn't this(initialized) called automatically??
        if (!initialized) {
            initialize()
            initialized = true
        }

        if (!m_systemsSorted) {
            addSystems(systems)
            m_systemsSorted = true
        }

        val newTime = System.nanoTime()
        //nanoseconds
        var frameTime = newTime - m_currentTime

        //ms per frame
        val minMsPerFrame: Long = 250
        if (frameTime > TimeUtils.millisToNanos(minMsPerFrame)) {
            frameTime = TimeUtils.millisToNanos(minMsPerFrame)    // Note: Avoid spiral of death
        }

        m_currentTime = newTime
        m_accumulator += frameTime

        //convert from nanos to millis then to seconds, to get fractional second dt
        world.setDelta(TimeUtils.nanosToMillis(m_nsPerTick) / 1000.0f)

        while (m_accumulator >= m_nsPerTick) {
            /** Process all entity systems inheriting from [RenderSystemMarker]  */
            for (i in 0..m_logicSystems.size - 1) {
                val systemAndProfiler = m_logicSystems.get(i)
                //TODO interpolate before this
        //        processProfileSystem(systemAndProfiler.profiler, systemAndProfiler.system)
                systemAndProfiler.system.process()
                updateEntityStates()
            }

            m_accumulator -= m_nsPerTick
        }

        //Gdx.app.log("frametime", Double.toString(frameTime));
        //Gdx.app.log("alpha", Double.toString(alpha));
        //try {
        //    int sleep = (int)Math.max(newTime + CLIENT_FIXED_TIMESTEP - TimeUtils.millis()/1000.0, 0.0);
        //    Gdx.app.log("", "sleep amnt: " + sleep);
        //    Thread.sleep(sleep);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}

        //float alpha = (float) m_accumulator / m_nsPerTick;

        //only clear if we have something to render..aka this world is a rendering one (client)
        //else it's a server, and this will crash due to no gl context, obviously
        if (m_renderSystems.size > 0) {
            Gdx.gl.glClearColor(.1f, .1f, .1f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }

        for (i in 0..m_renderSystems.size - 1) {
            //TODO interpolate this rendering with the state from the logic run, above
            //State state = currentState * alpha +
            //previousState * ( 1.0 - alpha );

            val systemAndProfiler = m_renderSystems.get(i)
            //TODO interpolate before this
            //processProfileSystem(systemAndProfiler.profiler, systemAndProfiler.system)
            systemAndProfiler.system.process();

            updateEntityStates()
        }

        if (!m_isServer) {
//            f.rameProfiler.stop()
        }
    }
}

