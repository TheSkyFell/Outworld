package io.github.theskyfell.outworld.core.platform;

import java.util.ServiceLoader;

/** Finds the loader-specific implementations of Core's platform interfaces. */
public final class Services {
    public static final Platform PLATFORM = load(Platform.class);

    private Services() {
    }

    public static <T> T load(Class<T> service) {
        return ServiceLoader.load(service, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No implementation of " + service.getName()
                        + " was found. Each loader project needs a META-INF/services file naming one."));
    }
}
