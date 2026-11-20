package io.github.theskyfell.outworld.core;

import io.github.theskyfell.outworld.core.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Outworld Core: the shared foundation every Outworld feature mod builds on. */
public final class OutworldCore {
    public static final String MOD_ID = "outworld";
    public static final Logger LOG = LoggerFactory.getLogger("Outworld Core");

    private OutworldCore() {
    }

    /** Called once by each loader's entry point. */
    public static void init() {
        LOG.info("Outworld Core starting on {} ({} environment)",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.isDevelopmentEnvironment() ? "development" : "production");
    }
}
