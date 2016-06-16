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

package com.ore.infinium.systems

import com.artemis.BaseSystem
import com.artemis.SystemInvocationStrategy
import com.artemis.utils.Bag
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.TimeUtils
import com.ore.infinium.OreSettings
import com.ore.infinium.systems.client.RenderSystemMarker
import com.ore.infinium.util.format
import com.ore.infinium.util.indices
import java.util.*

class GameLoopSystemInvocationStrategy
/**
 * @param msPerTick
 * *         desired ms per tick you want the logic systems to run at.
 * *         Rendering is unbounded/probably bounded by libgdx's
 * *         DesktopLauncher
 */
(msPerTick: Int, private val m_isServer: Boolean) : SystemInvocationStrategy() {

    //systems marked as indicating to be run only during the logic section of the loop
    private val m_renderSystems = mutableListOf<SystemAndProfiler>()
    private val m_logicSystems = mutableListOf<SystemAndProfiler>()

    private inner class SystemAndProfiler(internal var system: BaseSystem, internal var profiler: SystemProfiler)

    private var m_accumulatorNs: Long = 0

    //delta time
    private val m_nsPerTick = TimeUtils.millisToNanos(msPerTick.toLong())

    private var m_currentTimeNs = System.nanoTime()

    //minimum tick to chug along as, when we get really slow.
    //this way we're still rendering even though logic is taking up
    //an overbearing portion of our frame time
    private val minMsPerFrame: Long = 250
    private val minNsPerFrame = TimeUtils.millisToNanos(minMsPerFrame)

    private var m_systemsSorted: Boolean = false

    private fun addSystems(systems: Bag<BaseSystem>) {
        if (!m_systemsSorted) {
            val systemsData = systems.data
            for (i in systems.indices) {
                val system = systemsData[i] as BaseSystem
                if (system is RenderSystemMarker) {
                    //m_renderSystems.add(SystemAndProfiler(system, createSystemProfiler(system)))
                    m_renderSystems.add(createSystemAndProfiler(system))
                } else {
                    //m_logicSystems.add(SystemAndProfiler(system, createSystemProfiler(system)))
                    m_logicSystems.add(createSystemAndProfiler(system))
                }
            }
        }
    }

    private fun createSystemAndProfiler(system: BaseSystem): SystemAndProfiler {
        val profiler = SystemProfiler()
        profiler.initialize(system, world)

        val perfStat = PerfStat(profiler.counter.name)

        val hash = if (m_isServer) {
            serverPerfCounter
        } else {
            clientPerfCounter
        }

        hash[system] = perfStat

        return SystemAndProfiler(system, profiler)
    }

    override fun initialize() {
    }

    /*
    private fun createFrameProfiler() {
        frameProfiler = SystemProfiler.create("Frame Profiler")
        frameProfiler.setColor(1f, 1f, 1f, 1f)
    }
    */

    private fun processProfileSystem(systemAndProfiler: SystemAndProfiler) =
            systemAndProfiler.apply {
                profiler.start()
                system.process()
                profiler.stop()

                profiler.counter.tick()
            }

//
//    private fun createSystemProfiler(system: BaseSystem): SystemProfiler? {
//        var old: SystemProfiler? = null
//
//        if (!m_isServer) {
//            old = SystemProfiler.getFor(system)
//            if (old == null) {
//                old = SystemProfiler.createFor(system, world)
//            }
//        }
//
//        return old
//    }
//    */

    override fun process(systems: Bag<BaseSystem>) {
        if (!m_isServer) {
            //    frameProfiler.start()
        }

        if (!m_systemsSorted) {
            addSystems(systems)
            m_systemsSorted = true
        }

        val newTimeNs = System.nanoTime()
        //nanoseconds
        var frameTimeNs = newTimeNs - m_currentTimeNs

        if (frameTimeNs > minNsPerFrame) {
            frameTimeNs = minNsPerFrame    // Note: Avoid spiral of death
        }

        m_currentTimeNs = newTimeNs
        m_accumulatorNs += frameTimeNs

        //convert from nanos to millis then to seconds, to get fractional second dt
        world.setDelta(TimeUtils.nanosToMillis(m_nsPerTick) / 1000.0f)

        while (m_accumulatorNs >= m_nsPerTick) {
            /** Process all entity systems inheriting from [RenderSystemMarker]  */
            for (i in m_logicSystems.indices) {
                val systemAndProfiler = m_logicSystems[i]
                //TODO interpolate before this
                //        processProfileSystem(systemAndProfiler.profiler, systemAndProfiler.system)
                systemAndProfiler.system.process()
                updateEntityStates()
            }

            m_accumulatorNs -= m_nsPerTick
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
        if (!m_isServer) {
            Gdx.gl.glClearColor(.1f, .1f, .1f, 1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }

        for (i in m_renderSystems.indices) {
            //TODO interpolate this rendering with the state from the logic run, above
            //State state = currentState * alpha +
            //previousState * ( 1.0 - alpha );

            val systemAndProfiler = m_renderSystems[i]
            //TODO interpolate before this
            //processProfileSystem(systemAndProfiler.profiler, systemAndProfiler.system)
            processProfileSystem(systemAndProfiler)

            updateEntityStates()
        }

        if (OreSettings.profilerEnabled) {
            profilerStats()
            updateProfilers()
        }

        if (!m_isServer) {
//            f.rameProfiler.stop()
        }
    }

    private fun updateProfilers() {
        val hash = if (m_isServer) {
            serverPerfCounter
        } else {
            clientPerfCounter
        }

        //obviously, no rendering systems here..
        if (!m_isServer) {
            updateProfilerStatsForSystems(m_renderSystems, hash)
        }

        updateProfilerStatsForSystems(m_logicSystems,hash)
    }

    private fun updateProfilerStatsForSystems(m_renderSystems: List<SystemAndProfiler>, hash: HashMap<BaseSystem, PerfStat>) {
            for (systemAndProfiler in m_renderSystems) {
                val counter = systemAndProfiler.profiler.counter

                val perfStat = hash[systemAndProfiler.system]!!
                perfStat.timeMin = counter.time.min
                perfStat.timeMax = counter.time.max
                perfStat.timeAverage = counter.time.average
                perfStat.loadMin = counter.load.min
                perfStat.loadMax = counter.load.max
                perfStat.loadAverage = counter.load.average
            }

    }

    class PerfStat(val systemName: String, var timeMin: Float = 0f, var timeMax: Float = 0f, var timeAverage: Float = 0f,
                   var loadMin: Float = 0f, var loadMax: Float = 0f, var loadAverage: Float = 0f)

    //separated to reduce theoretical thread lock-stepping/stuttering
    //since client will want to reach in and grab server perf stats,
    //which will require synchronization to do so reliably (or you could
    //be grabbing half stats from prior frames)
    val serverPerfCounter = hashMapOf<BaseSystem, PerfStat>()
    val clientPerfCounter = hashMapOf<BaseSystem, PerfStat>()

    private fun profilerStats(): List<String> {
        val list = mutableListOf<String>()
        for (systemAndProfiler in m_logicSystems) {
            val counter = systemAndProfiler.profiler.counter

            val s = """tmin: ${counter.time.min.format()}
                    tmax: ${counter.time.max.format()}
                    tavg: ${counter.time.average.format()}
                    lmin: ${counter.load.min.format()}
                    lmin: ${counter.load.max.format()}
                    lmin: ${counter.load.average.format()}
                    """

            list.add(s)
        }

        return list
    }
}

