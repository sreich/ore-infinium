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

package com.ore.infinium.kartemis

import com.artemis.Aspect
import com.artemis.Component
import com.artemis.EntitySubscription
import com.artemis.World
import com.artemis.utils.IntBag
import com.ore.infinium.util.forEach

abstract class KEntitySystem(val aspectConfiguration: Aspect.Builder = Aspect.all()) : KBaseSystem(), EntitySubscription.SubscriptionListener {
    val subscription: EntitySubscription
        get() = world.aspectSubscriptionManager.get(aspectConfiguration)

    /**
     * Gets a mapper for a component type. This is safe to call from constructor code, e.g. initial field assignments.
     * If no World instance is available yet, the mapper will be initialized when a world becomes available.
     *
     * The given component type will be added to the aspect this entity system processes.
     *
     * @param T the component type
     */
    protected inline fun <reified T : Component> require(): KComponentMapper<T> {
        aspectConfiguration.all(T::class.java)
        return mapper(T::class)
    }

    override fun setWorld(world: World) {
        super.setWorld(world)

        subscription.addSubscriptionListener(this);
    }

    override fun inserted(entities: IntBag) =
        entities.forEach { inserted(it) }

    override fun removed(entities: IntBag) =
        entities.forEach { removed(it) }

    open fun inserted(entity: Int) {
    }

    open fun removed(entity: Int) {
    }
}