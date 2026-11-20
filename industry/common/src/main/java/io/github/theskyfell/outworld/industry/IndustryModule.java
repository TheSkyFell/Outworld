package io.github.theskyfell.outworld.industry;

import io.github.theskyfell.outworld.core.module.OutworldModule;
import io.github.theskyfell.outworld.core.network.Payloads;
import io.github.theskyfell.outworld.core.platform.Services;
import io.github.theskyfell.outworld.core.registry.Registrar;
import io.github.theskyfell.outworld.industry.test.TestMachine;
import io.github.theskyfell.outworld.industry.test.TestPing;
import io.github.theskyfell.outworld.industry.test.TestPong;
import java.util.List;

/** Outworld Industry, as handed to Core's loader layer. */
public final class IndustryModule implements OutworldModule {
    @Override
    public String modId() {
        return OutworldIndustry.MOD_ID;
    }

    @Override
    public List<Registrar<?>> registrars() {
        return IndustryContent.registrars();
    }

    @Override
    public void registerPayloads(Payloads payloads) {
        payloads.serverbound(TestPing.TYPE, TestPing.CODEC, TestMachine::handlePing)
                .clientbound(TestPong.TYPE, TestPong.CODEC);
    }

    @Override
    public void init() {
        OutworldIndustry.LOG.info("Outworld Industry starting on {}", Services.PLATFORM.getPlatformName());
    }
}
