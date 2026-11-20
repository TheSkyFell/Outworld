package io.github.theskyfell.outworld.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.platform.Services;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A JSON config file at {@code config/<mod id>/<name>.json}.
 *
 * <p>The config type is a plain class with public fields; the values its no-argument constructor sets
 * are the defaults. A missing file is created with the defaults. After a successful read, the file is
 * written back, which adds any settings that are newer than the file. A file that can't be read is
 * left alone, and the defaults are used.
 *
 * @param <T> the config type
 */
public final class ConfigFile<T> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path path;
    private final Class<T> type;
    private T value;

    private ConfigFile(Path path, Class<T> type) {
        this.path = path;
        this.type = type;
    }

    public static <T> ConfigFile<T> load(String modId, String name, Class<T> type) {
        Path path = Services.PLATFORM.getConfigDirectory().resolve(modId).resolve(name + ".json");
        ConfigFile<T> file = new ConfigFile<>(path, type);
        file.reload();
        return file;
    }

    public T get() {
        return value;
    }

    public Path path() {
        return path;
    }

    /** Reads the file again. */
    public void reload() {
        T defaults = createDefaults();
        if (!Files.exists(path)) {
            value = defaults;
            write();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T loaded = GSON.fromJson(reader, type);
            value = loaded != null ? loaded : defaults;
        } catch (IOException | JsonParseException e) {
            OutworldCore.LOG.error("Could not read config file {}: {}. Using the default settings; the file was left as it is.",
                    path, e.getMessage());
            value = defaults;
            return;
        }
        write();
    }

    private void write() {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(value) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            OutworldCore.LOG.error("Could not write config file {}: {}", path, e.getMessage());
        }
    }

    private T createDefaults() {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Config type " + type.getName() + " needs a public no-argument constructor", e);
        }
    }
}
