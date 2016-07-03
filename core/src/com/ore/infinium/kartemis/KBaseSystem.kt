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

import com.artemis.BaseSystem
import com.artemis.Component
import com.artemis.World
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


abstract class KBaseSystem : BaseSystem() {

    private val typeToMapper = hashMapOf<KClass<*>, KComponentMapper<*>>()
    private val typeToSystem = hashMapOf<KClass<*>, ReadOnlyProperty<KBaseSystem, BaseSystem>>()

    /**
     * Gets a mapper for a component type. This is safe to call from constructor code, e.g. initial field assignments.
     * If no World instance is available yet, the mapper will be initialized when a world becomes available.
     *
     * @param T the component type
     */
    protected inline fun <reified T : Component> mapper(): KComponentMapper<T> = mapper(T::class)

    /**
     * Gets a mapper for a component type. This is safe to call from constructor code, e.g. initial field assignments.
     * If no World instance is available yet, the mapper will be initialized when a world becomes available.
     *
     * @param T the component type
     * @param type the component class
     */
    protected fun <T : Component> mapper(type: KClass<T>): KComponentMapper<T> =
            typeToMapper.getOrPut(type, { KComponentMapper<T>() }) as KComponentMapper<T>

    /**
     * Gets a delegate for a system type. This is useful for declaring fields that hold system instances without
     * needing to declare them `lateinit var`.
     *
     * @param T the system types
     */
    protected inline fun <reified T : BaseSystem> system(): ReadOnlyProperty<KBaseSystem, T> = system(T::class)

    /**
     * Gets a delegate for a system type. This is useful for declaring fields that hold system instances without
     * needing to declare them `lateinit var`.
     *
     * @param T the system type
     * @param type the system class
     */
    protected fun <T : BaseSystem> system(type: KClass<T>): ReadOnlyProperty<KBaseSystem, T> =
        typeToSystem.getOrPut(type, {
            object : ReadOnlyProperty<KBaseSystem, T> {
                private var system: T? = null;

                override fun getValue(thisRef: KBaseSystem, property: KProperty<*>): T {
                    if (system == null)
                        system = thisRef.world.getSystem(type.java);
                    return system!!;
                }
            }
        }) as ReadOnlyProperty<KBaseSystem, T>

    override fun setWorld(world: World) {
        super.setWorld(world)
        typeToMapper.forEach { kClass, kComponentMapper ->
            kComponentMapper.mapper = world.getMapper(kClass.java as Class<Component>) }
    }
}
