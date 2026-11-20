package io.github.theskyfell.outworld.core.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.theskyfell.outworld.core.OutworldCore;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/**
 * A gas, such as hydrogen or oxygen. Gases are their own kind of resource, separate from fluids: they
 * live in gas tanks and move through gas pipes, and fluid storage never holds them.
 *
 * <p>Gases are data, loaded from {@code data/<namespace>/outworld/gas/<name>.json} when a world loads
 * and sent to clients, so modpacks can add their own. A gas's name is the translation key
 * {@code gas.<namespace>.<name>}. Code refers to a gas by its {@link ResourceKey}:
 *
 * <pre>{@code
 * { "color": "#9fd3ff" }
 * }</pre>
 *
 * @param color how the gas looks in screens and pipes, as 0xRRGGBB
 */
public record Gas(int color) {
    public static final ResourceKey<Registry<Gas>> REGISTRY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "gas"));

    /** "#rrggbb", or a plain number. */
    private static final Codec<Integer> COLOR_CODEC = Codec.withAlternative(
            Codec.STRING.comapFlatMap(Gas::parseColor, color -> String.format("#%06x", color)),
            Codec.INT);

    public static final Codec<Gas> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            COLOR_CODEC.fieldOf("color").forGetter(Gas::color)
    ).apply(instance, Gas::new));

    /** The display name of a gas: the translation key {@code gas.<namespace>.<name>}. */
    public static Component name(ResourceKey<Gas> gas) {
        Identifier id = gas.identifier();
        return Component.translatable("gas." + id.getNamespace() + "." + id.getPath());
    }

    /** A gas's key from its id, such as {@code outworld:hydrogen}. */
    public static ResourceKey<Gas> key(Identifier id) {
        return ResourceKey.create(REGISTRY, id);
    }

    private static DataResult<Integer> parseColor(String text) {
        if (text.length() == 7 && text.charAt(0) == '#') {
            try {
                return DataResult.success(Integer.parseInt(text.substring(1), 16));
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return DataResult.error(() -> "Gas colors look like \"#9fd3ff\": " + text);
    }
}
