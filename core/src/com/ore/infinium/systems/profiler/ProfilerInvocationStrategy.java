package com.ore.infinium.systems.profiler;

import com.artemis.BaseSystem;
import com.artemis.SystemInvocationStrategy;
import com.artemis.utils.Bag;
import com.artemis.utils.ImmutableBag;

/**
 * {@link SystemInvocationStrategy} that will create a profiler for all systems that don't already have one
 * Can be used in addition to or instead of {@link com.artemis.annotations.Profile} annotation
 * <p>
 * In addition creates {@link net.mostlyoriginal.plugin.profiler.SystemProfiler} with name "Frame" for total frame time
 * It can be accessed with {@link net.mostlyoriginal.plugin.profiler.SystemProfiler#get(String)}
 *
 * @author piotr-j
 * @author Daan van Yperen
 */
public class ProfilerInvocationStrategy extends SystemInvocationStrategy {
    private boolean initialized = false;

    protected net.mostlyoriginal.plugin.profiler.SystemProfiler frameProfiler;
    protected net.mostlyoriginal.plugin.profiler.SystemProfiler[] profilers;

    @Override
    protected void process(Bag<BaseSystem> systems) {

        if (!initialized) {
            initialize();
            initialized = true;
        }

        frameProfiler.start();
        processProfileSystems(systems);
        frameProfiler.stop();
    }

    private void processProfileSystems(Bag<BaseSystem> systems) {
        final Object[] systemsData = systems.getData();
        for (int i = 0; i < systems.size(); i++) {
            final BaseSystem system = (BaseSystem) systemsData[i];
            processProfileSystem(profilers[i], system);
            updateEntityStates();
        }
    }

    private void processProfileSystem(net.mostlyoriginal.plugin.profiler.SystemProfiler profiler, BaseSystem system) {
        if (profiler != null) {
            profiler.start();
        }
        system.process();
        if (profiler != null) {
            profiler.stop();
        }
    }

    protected void initialize() {
        createFrameProfiler();
        createSystemProfilers();
    }

    private void createSystemProfilers() {
        final ImmutableBag<BaseSystem> systems = world.getSystems();
        profilers = new net.mostlyoriginal.plugin.profiler.SystemProfiler[systems.size()];
        for (int i = 0; i < systems.size(); i++) {
            profilers[i] = createSystemProfiler(systems.get(i));
        }
    }

    private net.mostlyoriginal.plugin.profiler.SystemProfiler createSystemProfiler(BaseSystem system) {
        net.mostlyoriginal.plugin.profiler.SystemProfiler old =
                net.mostlyoriginal.plugin.profiler.SystemProfiler.getFor(system);
        if (old == null) {
            old = net.mostlyoriginal.plugin.profiler.SystemProfiler.createFor(system, world);
        }
        return old;
    }

    private void createFrameProfiler() {
        frameProfiler = net.mostlyoriginal.plugin.profiler.SystemProfiler.create("Frame");
        frameProfiler.setColor(1, 1, 1, 1);
    }
}
