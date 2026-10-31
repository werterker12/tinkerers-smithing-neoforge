package folk.sisby.tinkerers_smithing;

import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TinkerersSmithingConfig {
    public static final TinkerersSmithingConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        Pair<TinkerersSmithingConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(TinkerersSmithingConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private final ModConfigSpec.ConfigValue<List<? extends String>> ingredientSubstitutions;

    public TinkerersSmithingConfig(ModConfigSpec.Builder builder) {
        List<String> defaults = List.of(
            "minecraft:gold_ingot=c:ingots/gold",
            "minecraft:gold_nugget=c:nuggets/gold",
            "minecraft:iron_ingot=c:ingots/iron",
            "minecraft:iron_nugget=c:nuggets/iron",
            "minecraft:netherite_ingot=c:ingots/netherite",
            "minecraft:copper_ingot=c:ingots/copper",
            "minecraft:amethyst_shard=c:gems/amethyst",
            "minecraft:diamond=c:gems/diamond",
            "minecraft:emerald=c:gems/emerald",
            "minecraft:chest=c:chests/wooden",
            "minecraft:cobblestone=c:cobblestones",
            "minecraft:string=c:strings"
        );

        builder.comment("Maps items to equivalent tags that other mods substitute in crafting recipes. Format: item_id=tag_id");
        this.ingredientSubstitutions = builder.defineListAllowEmpty("ingredientSubstitutions", defaults, () -> "", o -> o instanceof String s && s.contains("="));
    }

    public Map<String, String> getSubstitutions() {
        Map<String, String> map = new HashMap<>();
        List<? extends String> list = ingredientSubstitutions.get();
        if (list != null) {
            for (String entry : list) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    map.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return map;
    }

    public boolean matchesOrEquivalent(Ingredient fromRepair, Ingredient fromCrafting) {
        String transformedJsonString = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, fromRepair).result().map(Object::toString).orElse("");
        String craftingJsonString = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, fromCrafting).result().map(Object::toString).orElse("");
        if (transformedJsonString.equals(craftingJsonString)) return true;
        for (Map.Entry<String, String> substitution : getSubstitutions().entrySet()) {
            transformedJsonString = transformedJsonString.replace(
                "{\"item\":\"%s\"}".formatted(substitution.getKey()),
                "{\"tag\":\"%s\"}".formatted(substitution.getValue())
            );
        }
        return transformedJsonString.equals(craftingJsonString);
    }

    public static ModConfig.Type getType() {
        return ModConfig.Type.COMMON;
    }
}
