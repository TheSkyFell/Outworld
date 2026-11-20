package io.github.theskyfell.outworld.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Outworld Core: the shared foundation every Outworld feature mod builds on. */
public final class OutworldCore {
    public static final String MOD_ID = "outworld";
    public static final Logger LOG = LoggerFactory.getLogger("Outworld Core");

    private static CoreConfig config = new CoreConfig();

    private OutworldCore() {
    }

    /** Core's settings, read from {@code config/outworld/core.json} at startup. */
    public static CoreConfig config() {
        return config;
    }

    static void setConfig(CoreConfig loaded) {
        config = loaded;
    }
}
