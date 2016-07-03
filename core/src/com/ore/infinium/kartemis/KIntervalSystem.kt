package com.ore.infinium.kartemis


abstract class KIntervalSystem(protected val interval: Float): KBaseSystem() {
    private var counter = 0f

    /** Actual delta time since the system was last processed */
    protected val delta: Float
        get() = interval + counter

    override fun checkProcessing(): Boolean {
        counter += world.getDelta();
        if (counter >= interval) {
            counter -= interval;
            return true;
        }
        return false;
    }
}