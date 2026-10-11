package com.celik.kingdoms;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import static com.celik.kingdoms.Rules.*;

/** Public icon bridge for placeholders and integrations. */
public final class KingdomIcons {
    private static final AtlasSprite PAPER_SPRITE = new AtlasSprite(Identifier.parse("minecraft:items"), Identifier.parse("minecraft:item/paper"));
    private static final Map<Identifier, AtlasSprite> SPRITES = loadSprites();

    private static Map<Identifier, AtlasSprite> loadSprites() {
        try (var input = KingdomIcons.class.getResourceAsStream("/kingdoms-icon-sprites.json")) {
            if (input == null) throw new IllegalStateException("Missing kingdom icon sprite mapping");
            var root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.get("minecraft").getAsString().equals("26.2")) throw new IllegalStateException("Wrong kingdom icon sprite mapping version");
            Map<Identifier, AtlasSprite> sprites = new HashMap<>();
            root.getAsJsonObject("sprites").entrySet().forEach(entry -> {
                var sprite = entry.getValue().getAsJsonObject();
                sprites.put(Identifier.parse(entry.getKey()), new AtlasSprite(Identifier.parse(sprite.get("atlas").getAsString()), Identifier.parse(sprite.get("sprite").getAsString())));
            });
            if (!PAPER_SPRITE.equals(sprites.get(Identifier.parse("minecraft:paper")))) throw new IllegalStateException("Missing paper fallback sprite");
            return Map.copyOf(sprites);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot load kingdom icon sprite mapping", e);
        }
    }

    /** Resolve a verified, static vanilla sprite; unknown client assets use the paper fallback. */
    static AtlasSprite sprite(Identifier itemId) {
        return SPRITES.getOrDefault(itemId, PAPER_SPRITE);
    }
    public static ItemStack stack(Kingdom kingdom) {
        Identifier id = Identifier.tryParse(kingdom == null ? "" : kingdom.iconItem);
        Item item = id == null ? Items.PAPER : BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR || item instanceof net.minecraft.world.item.BlockItem) item = Items.PAPER;
        return new ItemStack(item);
    }

    /** Returns Minecraft 26.2's real inline atlas object component, with item data on hover. */
    public static Component component(Kingdom kingdom) {
        ItemStack stack = stack(kingdom);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return Component.object(sprite(itemId), Component.literal("◆"))
            .withStyle(style -> style.withColor(ChatFormatting.WHITE)
                .withHoverEvent(new HoverEvent.ShowItem(ItemStackTemplate.fromNonEmptyStack(stack))));
    }

    public static String material(Kingdom kingdom) {
        return BuiltInRegistries.ITEM.getKey(stack(kingdom).getItem()).toString();
    }

    /** An inline item object followed by the small-caps display name, or small-caps none without a kingdom. */
    public static MutableComponent styled(Kingdom kingdom) {
        return kingdom==null?Component.literal(Ui.caps("none")):Component.empty().append(component(kingdom)).append(Component.literal(" "+Ui.caps(kingdom.display())));
    }

    private KingdomIcons() {}
}
