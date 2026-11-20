package io.github.theskyfell.outworld.core.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.theskyfell.outworld.core.OutworldCore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;

/**
 * The shape of a multiblock: which blocks go where around its controller. Structures are data, loaded
 * from {@code data/<namespace>/outworld/multiblock/<name>.json} when a world loads and sent to clients
 * (for the build preview). A controller names its structure with a {@link ResourceKey}.
 *
 * <p>The file draws the structure as layers, bottom to top, as seen from above with north at the top
 * and the controller facing north. Each layer is a list of rows from north to south; each character
 * in a row is one block, from west to east. {@code @} marks the controller, a space means any block,
 * and every other character is defined in {@code key}:
 *
 * <pre>{@code
 * {
 *   "pattern": [
 *     ["III", "III", "III"],
 *     ["I@I", "I_I", "III"],
 *     ["III", "III", "III"]
 *   ],
 *   "key": {
 *     "I": "minecraft:iron_block",
 *     "_": "#minecraft:air",
 *     "G": { "blocks": "#c:glass_blocks", "role": "window" }
 *   }
 * }
 * }</pre>
 *
 * <p>A key is a block, a list of blocks or a {@code #tag}, optionally with a role that the controller
 * can look up (where its energy or item ports are, say). In the world the structure turns with its
 * controller: a controller facing east matches the drawing turned a quarter clockwise.
 */
public final class MultiblockStructure {
    public static final ResourceKey<Registry<MultiblockStructure>> REGISTRY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "multiblock"));

    /** Marks the controller in a pattern. */
    public static final char CONTROLLER = '@';
    /** Any block may be here. */
    public static final char ANY = ' ';
    /** The most blocks a structure may span along each axis. */
    public static final int MAX_SIZE = 32;

    /** What may go at one position of the structure. */
    public record Part(HolderSet<Block> blocks, Optional<String> role) {
        private static final Codec<HolderSet<Block>> BLOCKS_CODEC = RegistryCodecs.holderSet(Registries.BLOCK);
        private static final Codec<Part> FULL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BLOCKS_CODEC.fieldOf("blocks").forGetter(Part::blocks),
                Codec.STRING.optionalFieldOf("role").forGetter(Part::role)
        ).apply(instance, Part::new));
        /** A full part object, or just its blocks. */
        public static final Codec<Part> CODEC = Codec.withAlternative(FULL_CODEC,
                BLOCKS_CODEC.xmap(blocks -> new Part(blocks, Optional.empty()), Part::blocks));

        /** A short name for messages: the block's name, or the tag. */
        public Component describe() {
            Optional<TagKey<Block>> tag = blocks.unwrapKey();
            if (tag.isPresent()) {
                return Component.literal("#" + tag.get().location());
            }
            if (blocks.size() == 0) {
                return Component.literal("nothing");
            }
            Component first = blocks.get(0).value().getName();
            return blocks.size() == 1 ? first : Component.empty().append(first).append(" (or " + (blocks.size() - 1) + " others)");
        }
    }

    /** One position of the structure, relative to the controller when it faces north. */
    public record Entry(BlockPos offset, Part part) {
    }

    /** How a structure compares with the world. */
    public enum Status {
        /** Every block is in place. */
        COMPLETE,
        /** A block is missing or wrong. */
        MISSING,
        /** Part of the structure is in a chunk that isn't loaded, so it can't be checked yet. */
        NOT_LOADED
    }

    /**
     * The result of {@link #match}.
     *
     * @param problem where checking stopped, unless complete
     * @param expected what should be at {@code problem}, when a block is missing
     * @param roles the positions of each role, when complete
     */
    public record Match(Status status, BlockPos problem, Part expected, Map<String, List<BlockPos>> roles) {
        public boolean complete() {
            return status == Status.COMPLETE;
        }
    }

    private record Raw(List<List<String>> pattern, Map<String, Part> key) {
        static final Codec<Raw> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().listOf().fieldOf("pattern").forGetter(Raw::pattern),
                Codec.unboundedMap(Codec.STRING, Part.CODEC).fieldOf("key").forGetter(Raw::key)
        ).apply(instance, Raw::new));
    }

    public static final Codec<MultiblockStructure> CODEC = Raw.CODEC.comapFlatMap(MultiblockStructure::build, structure -> structure.raw);

    private final Raw raw;
    private final List<Entry> entries;
    private final Vec3i size;
    private final BlockPos controllerInPattern;

    private MultiblockStructure(Raw raw, List<Entry> entries, Vec3i size, BlockPos controllerInPattern) {
        this.raw = raw;
        this.entries = List.copyOf(entries);
        this.size = size;
        this.controllerInPattern = controllerInPattern;
    }

    private static DataResult<MultiblockStructure> build(Raw raw) {
        List<List<String>> layers = raw.pattern();
        if (layers.isEmpty() || layers.getFirst().isEmpty() || layers.getFirst().getFirst().isEmpty()) {
            return DataResult.error(() -> "A multiblock pattern needs at least one layer, row and block");
        }
        int depth = layers.getFirst().size();
        int width = layers.getFirst().getFirst().length();
        for (List<String> layer : layers) {
            if (layer.size() != depth) {
                return DataResult.error(() -> "Every layer of a multiblock pattern needs " + depth + " rows");
            }
            for (String row : layer) {
                if (row.length() != width) {
                    return DataResult.error(() -> "Every row of a multiblock pattern needs " + width + " blocks: \"" + row + "\"");
                }
            }
        }
        if (layers.size() > MAX_SIZE || depth > MAX_SIZE || width > MAX_SIZE) {
            return DataResult.error(() -> "A multiblock may span at most " + MAX_SIZE + " blocks each way");
        }

        Map<Character, Part> key = new HashMap<>();
        for (Map.Entry<String, Part> entry : raw.key().entrySet()) {
            String symbol = entry.getKey();
            if (symbol.length() != 1 || symbol.charAt(0) == CONTROLLER || symbol.charAt(0) == ANY) {
                return DataResult.error(() -> "Multiblock keys must be single characters other than '@' and ' ': \"" + symbol + "\"");
            }
            key.put(symbol.charAt(0), entry.getValue());
        }

        BlockPos controller = null;
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < depth; z++) {
                String row = layers.get(y).get(z);
                for (int x = 0; x < width; x++) {
                    char symbol = row.charAt(x);
                    if (symbol == CONTROLLER) {
                        if (controller != null) {
                            return DataResult.error(() -> "A multiblock pattern needs exactly one '@' (the controller)");
                        }
                        controller = new BlockPos(x, y, z);
                    } else if (symbol != ANY && !key.containsKey(symbol)) {
                        return DataResult.error(() -> "Multiblock pattern uses '" + symbol + "', which isn't in the key");
                    }
                }
            }
        }
        if (controller == null) {
            return DataResult.error(() -> "A multiblock pattern needs exactly one '@' (the controller)");
        }

        List<Entry> entries = new ArrayList<>();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < depth; z++) {
                String row = layers.get(y).get(z);
                for (int x = 0; x < width; x++) {
                    Part part = key.get(row.charAt(x));
                    if (part != null) {
                        entries.add(new Entry(new BlockPos(x, y, z).subtract(controller), part));
                    }
                }
            }
        }
        return DataResult.success(new MultiblockStructure(raw, entries, new Vec3i(width, layers.size(), depth), controller));
    }

    /** Every position that must hold a particular block, relative to a controller facing north. */
    public List<Entry> entries() {
        return entries;
    }

    /** Width (west to east), height and depth (north to south) of the pattern. */
    public Vec3i size() {
        return size;
    }

    /** Where the controller is in the pattern, counted from its north-west bottom corner. */
    public BlockPos controllerInPattern() {
        return controllerInPattern;
    }

    /** Where an entry is in the world, for a controller at {@code controller} facing {@code facing}. */
    public static BlockPos place(Entry entry, BlockPos controller, Direction facing) {
        return controller.offset(entry.offset().rotate(rotation(facing)));
    }

    /** Every position the structure covers in the world, apart from the controller and "any block" spots. */
    public List<BlockPos> positions(BlockPos controller, Direction facing) {
        List<BlockPos> positions = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            positions.add(place(entry, controller, facing));
        }
        return positions;
    }

    /**
     * Compares the structure with the world around a controller. Never loads chunks: if part of the
     * structure is in an unloaded chunk, the result is {@link Status#NOT_LOADED}.
     */
    public Match match(Level level, BlockPos controller, Direction facing) {
        Map<String, List<BlockPos>> roles = new LinkedHashMap<>();
        for (Entry entry : entries) {
            BlockPos pos = place(entry, controller, facing);
            if (!level.isLoaded(pos)) {
                return new Match(Status.NOT_LOADED, pos, entry.part(), Map.of());
            }
            if (!level.getBlockState(pos).is(entry.part().blocks())) {
                return new Match(Status.MISSING, pos, entry.part(), Map.of());
            }
            entry.part().role().ifPresent(role -> roles.computeIfAbsent(role, r -> new ArrayList<>()).add(pos));
        }
        return new Match(Status.COMPLETE, null, null, roles);
    }

    /** The turn that takes the pattern (drawn facing north) to a controller facing {@code facing}. */
    public static Rotation rotation(Direction facing) {
        return switch (facing) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
}
