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

package com.ore.infinium

import com.artemis.annotations.Wire
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window

@Wire
class DebugProfilerView(stage: Stage, private val m_skin: Skin, //the hotbar inventory, for drag and drop
                        private val m_world: OreWorld) {
    var profilerVisible: Boolean
        get() = m_window.isVisible
        set(value) {
            m_window.setVisible(value)
        }

    private val m_window: Window

    init {
        val container = Table(m_skin)
        container.setFillParent(true)
        container.center() //top().right().setSize(800, 100);
        container.defaults().space(4f)
        container.padLeft(10f).padTop(10f)

        m_window = Window("Profiler", m_skin)
        //fixme;not centering or anything, all hardcoded :(
        m_window.setPosition(900f, 100f)
        m_window.top().right().setSize(400f, 500f)
        //        window.defaults().space(4);
        //window.pack();
        m_window.add(container).fill().expand()

        profilerVisible = false
       
        stage.addActor(m_window)

    }
}
