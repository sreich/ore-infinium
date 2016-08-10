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

package com.ore.infinium.systems.client

import com.artemis.BaseSystem
import com.artemis.annotations.Wire
import com.badlogic.gdx.graphics.OrthographicCamera
import com.ore.infinium.OreWorld
import com.ore.infinium.util.system

@Wire
/**
 * This input system is stuck in a bit of a hairy position...for input, we need two
 * scenarios, direct input event, and polling based. Polling based must be used for things like
 * primary attack.

 * Event based is used for pretty much everything else, like GUI related things (excluding scene2d, that's already
 * basically handled by itself).
 */
class InputSystem(private val camera: OrthographicCamera, private val oreWorld: OreWorld) : BaseSystem() {

    private val powerOverlayRenderSystem by system<PowerOverlayRenderSystem>()
    private val clientNetworkSystem by system<ClientNetworkSystem>()

    override fun processSystem() {
        if (powerOverlayRenderSystem.overlayVisible || !clientNetworkSystem.connected) {
            return
        }

        if (oreWorld.client!!.leftMouseDown) {
            oreWorld.client!!.handlePrimaryAttack()
        } else if (oreWorld.client!!.rightMouseDown) {
            oreWorld.client!!.handleSecondaryAttack()
        }
    }
}
