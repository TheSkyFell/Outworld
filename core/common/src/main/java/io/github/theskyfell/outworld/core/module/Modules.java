package io.github.theskyfell.outworld.core.module;

import io.github.theskyfell.outworld.core.platform.Services;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/** The Outworld mods that are loaded in this game. */
public final class Modules {
    private static final List<OutworldModule> LOADED = new CopyOnWriteArrayList<>();

    private Modules() {
    }

    /** Called by the loader layer when it loads a module. */
    public static void add(OutworldModule module) {
        LOADED.add(module);
    }

    public static List<OutworldModule> all() {
        return List.copyOf(LOADED);
    }

    /** For logs and commands, e.g. "outworld 0.1.0, outworld_industry 0.1.0". */
    public static String describe() {
        return LOADED.stream()
                .map(module -> module.modId() + " " + Services.PLATFORM.getModVersion(module.modId()).orElse("(unknown version)"))
                .collect(Collectors.joining(", "));
    }
}
