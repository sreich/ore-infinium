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

@file:Suppress("NOTHING_TO_INLINE")

package com.ore.infinium.util

import com.artemis.*
import com.artemis.managers.TagManager
import com.artemis.utils.Bag
import com.artemis.utils.IntBag
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

/**
 * indicates an invalid/unset entity id
 */
const val INVALID_ENTITY_ID = -1

inline fun isValidEntity(entityId: Int) = entityId != INVALID_ENTITY_ID
inline fun isInvalidEntity(entityId: Int) = entityId == INVALID_ENTITY_ID

typealias OreEntityId = Int

fun World.oreInject(obj: Any) {
    this.inject(obj)
}

inline fun <reified T : BaseSystem> World.system() =
        getSystem(T::class.java)

interface OreEntitySubscriptionListener : EntitySubscription.SubscriptionListener {
    override fun inserted(entities: IntBag) = Unit
    override fun removed(entities: IntBag) = Unit
}

/**
 * A marker interface that indicates that this system should only be
 * processed by the render portion of the game loop. Separating the logic
 * and the render ticks, so that we can decide how often to process them (how
 * many ms per frame, etc)
 */
interface RenderSystemMarker


/**
 * Denotes that a component property should not be copied
 */
@Retention
@Target(AnnotationTarget.PROPERTY)
annotation class DoNotCopy

interface CopyableComponent<T : CopyableComponent<T>> {

    /**
     * copy a component (similar to copy constructor)
     *
     * @param component
     *         component to copy from, into this instance
     */
    fun copyFrom(component: T): Unit
}

/**
 * Denotes that a component property should not be printed on-screen in debug mode
 */
@Retention
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class DoNotPrint

fun TagManager.getEntityId(tag: String): Int {
    return this.getEntity(tag)!!.id
}

fun World.entities(aspect: Aspect.Builder): IntBag =
        this.aspectSubscriptionManager.get(aspect).entities

//object ArtemisExtensions {
fun allOf(vararg types: KClass<out Component>): Aspect.Builder =
        Aspect.all(types.map { it.java })

fun anyOf(vararg types: KClass<out Component>): Aspect.Builder =
        Aspect.one(types.map { it.java })

fun noneOf(vararg types: KClass<out Component>): Aspect.Builder =
        Aspect.exclude(types.map { it.java })

fun <T : Component> ComponentMapper<T>.opt(entityId: Int): T? = if (has(entityId)) get(entityId) else null

inline fun <T : Component> ComponentMapper<T>.ifPresent(entityId: Int, function: (T) -> Unit): Unit {
    if (has(entityId))
        function(get(entityId))
}

fun TagManager.getTagNullable(entityId: Entity): String? {
    return this.getTag(entityId) ?: null
}

/*
inline fun <T> IntBag.forEachIndexed(action: (Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}
*/

inline fun IntBag.forEach(action: (Int) -> Unit): Unit {
    for (i in indices) action(this.get(i))
}


//public inline fun <T> Array<out T>.forEach(action: (T) -> Unit): Unit {
//    for (element in this) action(element)
//}

fun IntBag.toMutableList(): MutableList<Int> {
    val list = mutableListOf<Int>()
    this.forEach { list.add(it) }
    return list
}

val IntBag.indices: IntRange get() = 0..size() - 1
val <T : Any> Bag<T>.indices: IntRange get() = 0..size() - 1

/*
inline fun <T> Array<out T>.forEachIndexed(action: (Int, T) -> Unit): Unit {
    var index = 0
    for (item in this) action(index++, item)
}
*/

private class MapperProperty<T : Component>(val type: Class<T>) : ReadOnlyProperty<BaseSystem, ComponentMapper<T>> {
    private var cachedMapper: ComponentMapper<T>? = null

    override fun getValue(thisRef: BaseSystem, property: KProperty<*>): ComponentMapper<T> {
        if (cachedMapper == null) {
            val worldField = BaseSystem::class.java.getDeclaredField("world")
            worldField.isAccessible = true

            val world = worldField.get(thisRef) as World?
            world ?: throw IllegalStateException("world is not initialized yet")
            cachedMapper = world.getMapper(type)
        }
        return cachedMapper!!
    }
}

private class SystemProperty<T : BaseSystem>(val type: Class<T>) : ReadOnlyProperty<BaseSystem, T> {
    private var cachedSystem: T? = null

    override fun getValue(thisRef: BaseSystem, property: KProperty<*>): T {
        if (cachedSystem == null) {
            val worldField = BaseSystem::class.java.getDeclaredField("world")
            worldField.isAccessible = true

            val world = worldField.get(thisRef) as World?
            world ?: throw IllegalStateException("world is not initialized yet")

            cachedSystem = world.getSystem(type)
        }
        return cachedSystem!!
    }
}

/**
 * Gets a delegate that returns the `ComponentMapper` for the given component type.
 *
 * @param T the component type.
 */
inline fun <reified T : Component> BaseSystem.mapper(): ReadOnlyProperty<BaseSystem, ComponentMapper<T>> = mapper(
        T::class)

/**
 * Gets a delegate that returns the `ComponentMapper` for the given component type.
 *
 * @param T the component type.
 * @param type the component class.
 */
fun <T : Component> BaseSystem.mapper(type: KClass<T>): ReadOnlyProperty<BaseSystem, ComponentMapper<T>> = MapperProperty<T>(
        type.java)

/**
 * Gets a delegate that returns the `ComponentMapper` for the given component type, and adds the component type
 * to the system's aspect configuration. Note that this must be called from constructor code, or it won't be effective!
 *
 * @param T the component type.
 */
inline fun <reified T : Component> BaseEntitySystem.require(): ReadOnlyProperty<BaseSystem, ComponentMapper<T>> = require(
        T::class)

/**
 * Gets a delegate that returns the `ComponentMapper` for the given component type, and adds the component type
 * to the system's aspect configuration. Note that this must be called from constructor code, or it won't be effective!
 *
 * @param T the component type.
 * @param type the component class.
 */
fun <T : Component> BaseEntitySystem.require(type: KClass<T>): ReadOnlyProperty<BaseSystem, ComponentMapper<T>> {
    val aspectConfigurationField = BaseEntitySystem::class.java.getDeclaredField("aspectConfiguration")
    aspectConfigurationField.isAccessible = true

    val aspectConfiguration = aspectConfigurationField.get(this) as Aspect.Builder
    aspectConfiguration.all(type.java)

    return MapperProperty<T>(type.java)
}

/**
 * Gets a delegate that returns the `EntitySystem` of the given type.
 *
 * @param T the system type.
 */
inline fun <reified T : BaseSystem> BaseSystem.system(): ReadOnlyProperty<BaseSystem, T> = system(T::class)

/**
 * Gets a delegate that returns the `EntitySystem` of the given type.
 *
 * @param T the system type.
 * @param type the system class.
 */
fun <T : BaseSystem> BaseSystem.system(type: KClass<T>): ReadOnlyProperty<BaseSystem, T> = SystemProperty<T>(type.java)

private val cacheByType = hashMapOf<Class<*>, PropertyCache>()

private fun getCache(clazz: Class<*>): PropertyCache =
        cacheByType.getOrPut(clazz, { PropertyCache(clazz.kotlin) })


/**
 * Cache that stores properties of component implementations.
 */
private class PropertyCache(clazz: KClass<*>) {
    val copyProperties = clazz.members.mapNotNull { it as? KMutableProperty }
            .filter { !it.annotations.any { it.annotationClass == DoNotCopy::class } }.toTypedArray()

    val printProperties = clazz.members.mapNotNull { it as? KProperty }
            .filter {
                !it.annotations.any { it.annotationClass == DoNotPrint::class } &&
                        !it.getter.annotations.any { it.annotationClass == DoNotPrint::class }
            }.toTypedArray()
}

/**
 * copy a component (similar to copy constructor)
 *
 * @param component
 *         component to copy from, into this instance
 */
fun <T : Component> T.copyFrom(component: T) {
    if (this is CopyableComponent<*>) {
        this.internalCopyFrom<InternalCopyableComponent>(component)
    } else {
        this.defaultCopyFrom(component)
    }
}

/**
 * copy a component (similar to copy constructor)
 *
 * @param component
 *         component to copy from, into this instance
 */
fun <T : Component> T.defaultCopyFrom(component: T): Unit {
    getCache(javaClass).copyProperties.forEach { it.setter.call(this, it.getter.call(component)) }
}

// Just hacking around Kotlin generics...
@Suppress("UNCHECKED_CAST")
private fun <T : CopyableComponent<T>> Any.internalCopyFrom(component: Any) {
    (this as T).copyFrom(component as T)
}

private class InternalCopyableComponent : CopyableComponent<InternalCopyableComponent> {
    override fun copyFrom(component: InternalCopyableComponent) {
        assert(false)
    }
}

// TODO: Might want to introduce PrintableComponent interface
fun <T : Component> T.printString(): String {
    return this.defaultPrintString()
}

fun <T : Component> T.defaultPrintString(): String =
        getCache(javaClass).printProperties.map { "${javaClass.simpleName}.${it.name} = ${it.getter.call(this)}" }
                .joinToString(separator = "\n", postfix = "\n")

//}
