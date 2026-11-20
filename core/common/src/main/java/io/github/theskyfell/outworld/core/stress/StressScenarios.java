package io.github.theskyfell.outworld.core.stress;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/** The stress scenarios {@code /outworld stress} can run, by name. Mods register theirs in {@code init}. */
public final class StressScenarios {
    private static final Map<String, StressScenario> SCENARIOS = new TreeMap<>();

    private StressScenarios() {
    }

    /**
     * Adds a scenario. Names are single words, such as {@code machines}; each mod should use names
     * that describe what is built.
     */
    public static synchronized void register(String name, StressScenario scenario) {
        if (!name.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Stress scenario names are lowercase words: " + name);
        }
        if (SCENARIOS.putIfAbsent(name, scenario) != null) {
            throw new IllegalArgumentException("Stress scenario " + name + " is already registered");
        }
    }

    public static synchronized StressScenario get(String name) {
        return SCENARIOS.get(name);
    }

    public static synchronized Map<String, StressScenario> all() {
        return Collections.unmodifiableMap(new TreeMap<>(SCENARIOS));
    }
}
