/**
 * ***************************************************************************
 * Copyright (C) 2015 by Shaun Reich <sreich02@gmail.com>                   *
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

package com.ore.infinium.systems;

import com.artemis.BaseSystem;
import com.artemis.SystemInvocationStrategy;
import com.artemis.utils.Bag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import net.mostlyoriginal.plugin.profiler.SystemProfiler;

public class GameLoopSystemInvocationStrategy extends SystemInvocationStrategy {

    //systems marked as indicating to be run only during the logic section of the loop
    private final Array<BaseSystem> m_renderSystems = new Array<>();
    private final Array<BaseSystem> m_logicSystems = new Array<>();

    private long m_accumulator;

    //delta time
    private long m_nsPerTick;
    private long m_currentTime = System.nanoTime();

    private boolean m_systemsSorted;

    protected SystemProfiler frameProfiler;
    protected SystemProfiler[] profilers;

    /**
     * @param msPerTick
     *         desired ms per tick you want the logic systems to run at.
     *         Rendering is unbounded/probably bounded by libgdx's
     *         DesktopLauncher
     */
    public GameLoopSystemInvocationStrategy(int msPerTick) {
        m_nsPerTick = TimeUtils.millisToNanos(msPerTick);
    }

    private void addSystems(Bag<BaseSystem> systems) {
        if (!m_systemsSorted) {
            Object[] systemsData = systems.getData();
            for (int i = 0; i < systems.size(); ++i) {
                BaseSystem system = (BaseSystem) systemsData[i];
                if (system instanceof RenderSystemMarker) {
                    m_renderSystems.add(system);
                } else {
                    m_logicSystems.add(system);
                }
            }
        }
    }

    @Override
    protected void process(Bag<BaseSystem> systems) {
        if (!m_systemsSorted) {
            addSystems(systems);
            m_systemsSorted = true;
        }

        //nanoseconds
        long newTime = System.nanoTime();
        //nanoseconds
        long frameTime = newTime - m_currentTime;

        //ms per frame
        final long minMsPerFrame = 250;
        if (frameTime > TimeUtils.millisToNanos(minMsPerFrame)) {
            frameTime = TimeUtils.millisToNanos(minMsPerFrame);    // Note: Avoid spiral of death
        }

        m_currentTime = newTime;
        m_accumulator += frameTime;

        world.setDelta(TimeUtils.millisToNanos(m_nsPerTick));

        while (m_accumulator >= m_nsPerTick) {
            /** Process all entity systems inheriting from {@link RenderSystemMarker} */
            for (int i = 0; i < m_logicSystems.size; i++) {
                //TODO interpolate before this
                m_logicSystems.get(i).process();
                updateEntityStates();
            }

            m_accumulator -= m_nsPerTick;
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

        /**
         * Uncomment this line if you use the world's delta within your systems.
         */
        world.setDelta(TimeUtils.nanosToMillis(frameTime));

        //float alpha = (float) m_accumulator / m_nsPerTick;

        //only clear if we have something to render..aka this world is a rendering one (client)
        //else it's a server, and this will crash due to no gl context, obviously
        if (m_renderSystems.size > 0) {
            //Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClearColor(1, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        }

        for (int i = 0; i < m_renderSystems.size; i++) {
            //TODO interpolate this rendering with the state from the logic run, above
            //State state = currentState * alpha +
            //previousState * ( 1.0 - alpha );

            m_renderSystems.get(i).process();
            updateEntityStates();
        }
    }
}

