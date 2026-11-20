package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.MachineMenu;
import io.github.theskyfell.outworld.core.menu.Menus;
import io.github.theskyfell.outworld.core.network.Network;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The assembler's menu: a recipe machine's, plus the list of recipes it can make and the one chosen.
 * The server sends the list once when the screen opens ({@link RecipeList}) and the choice whenever it
 * changes ({@link Chosen}); the screen sends the player's pick back ({@link Choose}).
 */
public final class AssemblerMenu extends MachineMenu {
    private final AssemblerBlockEntity assembler;
    private final Player player;
    // Server: what this player's screen has been sent.
    private boolean listSent;
    private boolean choiceSent;
    private Identifier sentChoice;
    // Client: what the server sent.
    private List<AssemblerBlockEntity.Choice> choices = List.of();
    private Identifier chosen;
    private int version;

    /** On the client, from the data the server sent. */
    public AssemblerMenu(int containerId, Inventory inventory, OpenData data) {
        this(containerId, inventory, data, null);
    }

    private AssemblerMenu(int containerId, Inventory inventory, OpenData data, AssemblerBlockEntity assembler) {
        super(IndustryContent.ASSEMBLER_MENU.get(), containerId, inventory, data, assembler);
        this.assembler = assembler;
        this.player = inventory.player;
    }

    public static void open(ServerPlayer player, AssemblerBlockEntity assembler, Component title) {
        OpenData data = OpenData.of(assembler);
        Menus.open(player, new SimpleMenuProvider(
                (containerId, inventory, opener) -> new AssemblerMenu(containerId, inventory, data, assembler), title),
                OpenData.STREAM_CODEC, data);
    }

    /** Client: the recipes the assembler can make. */
    public List<AssemblerBlockEntity.Choice> choices() {
        return choices;
    }

    /** Client: the chosen recipe, or null. */
    public Identifier chosen() {
        return chosen;
    }

    /** Client: goes up whenever the list or the choice changes, so the screen knows to look again. */
    public int version() {
        return version;
    }

    /** Client: the player picked a recipe (or picked the chosen one again, to choose none). */
    public void pick(Identifier recipe) {
        chosen = recipe;
        version++;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (assembler == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!listSent) {
            listSent = true;
            List<Entry> entries = new ArrayList<>();
            for (AssemblerBlockEntity.Choice choice : AssemblerBlockEntity.choices(serverPlayer.level().getServer())) {
                entries.add(new Entry(choice.recipe().toString(),
                        BuiltInRegistries.ITEM.getKey(choice.result().getItem()).toString(), choice.result().getCount()));
            }
            Network.sendToPlayer(serverPlayer, new RecipeList(containerId, entries));
        }
        Identifier current = assembler.chosenRecipe();
        if (!choiceSent || !Objects.equals(current, sentChoice)) {
            choiceSent = true;
            sentChoice = current;
            Network.sendToPlayer(serverPlayer, new Chosen(containerId, current == null ? "" : current.toString()));
        }
    }

    /** Server: a player's pick from the screen. */
    public static void handleChoose(Choose choose, ServerPlayer sender) {
        if (!(sender.containerMenu instanceof AssemblerMenu menu) || menu.containerId != choose.containerId()
                || menu.assembler == null || !menu.stillValid(sender)) {
            return;
        }
        if (choose.recipe().isEmpty()) {
            menu.assembler.choose(null, sender);
            return;
        }
        Identifier recipe = Identifier.tryParse(choose.recipe());
        if (recipe != null && AssemblerBlockEntity.canMake(sender.level().getServer(), recipe)) {
            menu.assembler.choose(recipe, sender);
        }
    }

    /** Client: the list arrived. */
    public static void onRecipeList(RecipeList list, Player player) {
        if (player != null && player.containerMenu instanceof AssemblerMenu menu && menu.containerId == list.containerId()) {
            List<AssemblerBlockEntity.Choice> received = new ArrayList<>();
            for (Entry entry : list.entries()) {
                Identifier recipe = Identifier.tryParse(entry.recipe());
                Identifier itemId = Identifier.tryParse(entry.item());
                Item item = itemId == null ? null : BuiltInRegistries.ITEM.getValue(itemId);
                if (recipe != null && item != null) {
                    received.add(new AssemblerBlockEntity.Choice(recipe, new ItemStack(item, entry.count())));
                }
            }
            menu.choices = List.copyOf(received);
            menu.version++;
        }
    }

    /** Client: the choice changed (or the screen just opened). */
    public static void onChosen(Chosen chosen, Player player) {
        if (player != null && player.containerMenu instanceof AssemblerMenu menu && menu.containerId == chosen.containerId()) {
            menu.chosen = chosen.recipe().isEmpty() ? null : Identifier.tryParse(chosen.recipe());
            menu.version++;
        }
    }

    /** One recipe in the list: its id, and the id and count of what it makes. */
    public record Entry(String recipe, String item, int count) {
        public static final StreamCodec<ByteBuf, Entry> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Entry::recipe,
                ByteBufCodecs.STRING_UTF8, Entry::item,
                ByteBufCodecs.VAR_INT, Entry::count,
                Entry::new);
    }

    /** Server to client: every recipe the assembler can make. */
    public record RecipeList(int containerId, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<RecipeList> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "assembler_recipes"));
        public static final StreamCodec<ByteBuf, RecipeList> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RecipeList::containerId,
                Entry.CODEC.apply(ByteBufCodecs.list()), RecipeList::entries,
                RecipeList::new);

        @Override
        public Type<RecipeList> type() {
            return TYPE;
        }
    }

    /** Server to client: the chosen recipe's id, or "" for none. */
    public record Chosen(int containerId, String recipe) implements CustomPacketPayload {
        public static final Type<Chosen> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "assembler_chosen"));
        public static final StreamCodec<ByteBuf, Chosen> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Chosen::containerId,
                ByteBufCodecs.STRING_UTF8, Chosen::recipe,
                Chosen::new);

        @Override
        public Type<Chosen> type() {
            return TYPE;
        }
    }

    /** Client to server: the player picked this recipe, or "" for none. */
    public record Choose(int containerId, String recipe) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "assembler_choose"));
        public static final StreamCodec<ByteBuf, Choose> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Choose::containerId,
                ByteBufCodecs.STRING_UTF8, Choose::recipe,
                Choose::new);

        @Override
        public Type<Choose> type() {
            return TYPE;
        }
    }
}
