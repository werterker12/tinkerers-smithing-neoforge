package folk.sisby.tinkerers_smithing;

import com.mojang.serialization.JsonOps;
import folk.sisby.kaleido.api.WrappedConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import net.minecraft.recipe.Ingredient;

import java.util.Map;

public class TinkerersSmithingConfig extends WrappedConfig {
	@Comment("Maps items to equivalent tags that other mods substitute in crafting recipes")
	public Map<String, String> ingredientSubstitutions = ValueMap.builder("")
		.put("minecraft:gold_ingot", "c:ingots/gold")
		.put("minecraft:gold_nugget", "c:nuggets/gold")
		.put("minecraft:iron_ingot", "c:ingots/iron")
		.put("minecraft:iron_nugget", "c:nuggets/iron")
		.put("minecraft:netherite_ingot", "c:ingots/netherite")
		.put("minecraft:copper_ingot", "c:ingots/copper")
		.put("minecraft:amethyst_shard", "c:gems/amethyst")
		.put("minecraft:diamond", "c:gems/diamond")
		.put("minecraft:emerald", "c:gems/emerald")
		.put("minecraft:chest", "c:chests/wooden")
		.put("minecraft:cobblestone", "c:cobblestones")
		.put("minecraft:string", "c:strings")
		.build();

	public boolean matchesOrEquivalent(Ingredient fromRepair, Ingredient fromCrafting) {
		String transformedJsonString = Ingredient.ALLOW_EMPTY_CODEC.encodeStart(JsonOps.INSTANCE, fromRepair).getOrThrow().toString();
		if (transformedJsonString.equals(Ingredient.ALLOW_EMPTY_CODEC.encodeStart(JsonOps.INSTANCE, fromCrafting).getOrThrow().toString())) return true;
		for (Map.Entry<String, String> substitution : ingredientSubstitutions.entrySet()) {
			transformedJsonString = transformedJsonString.replace("{\"item\":\"%s\"}".formatted(substitution.getKey()), "{\"tag\":\"%s\"}".formatted(substitution.getValue()));
		}
		return transformedJsonString.equals(Ingredient.ALLOW_EMPTY_CODEC.encodeStart(JsonOps.INSTANCE, fromCrafting).getOrThrow().toString());
	}
}
