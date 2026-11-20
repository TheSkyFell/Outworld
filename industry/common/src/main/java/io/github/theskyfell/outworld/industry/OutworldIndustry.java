package io.github.theskyfell.outworld.industry;

import io.github.theskyfell.outworld.core.platform.Services;
import io.github.theskyfell.outworld.core.registry.Registrar;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Outworld Industry: power, processing and machines. Depends only on Outworld Core. */
public final class OutworldIndustry {
    public static final String MOD_ID = "outworld_industry";
    public static final Logger LOG = LoggerFactory.getLogger("Outworld Industry");

    private OutworldIndustry() {
    }

    /** Everything this mod registers, in the order the loader layer should register it. */
    public static List<Registrar<?>> registrars() {
        return IndustryContent.registrars();
    }

    /** Called once by each loader's entry point, after its registrars are handed to the loader. */
    public static void init() {
        LOG.info("Outworld Industry starting on {}", Services.PLATFORM.getPlatformName());
    }
}
