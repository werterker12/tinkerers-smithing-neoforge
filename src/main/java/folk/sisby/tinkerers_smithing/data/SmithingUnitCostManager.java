package folk.sisby.tinkerers_smithing.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashMap;
import java.util.Map;

public class SmithingUnitCostManager extends SimpleJsonResourceReloadListener {
	public static final SmithingUnitCostManager INSTANCE = new SmithingUnitCostManager(new Gson());
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TinkerersSmithing.ID, "smithing_unit_cost_loader");

	public static final String KEY_REPLACE = "replace";
	public static final String KEY_VALUES = "values";
	public static final String KEY_VALUE_COST = "cost";

	public SmithingUnitCostManager(Gson gson) {
		super(gson, "smithing_unit_costs");
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> prepared, ResourceManager manager, ProfilerFiller profiler) {
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Loading Unit Costs!");
		TinkerersSmithingLoader.INSTANCE.COST_OVERRIDES.clear();
		prepared.forEach((id, json) -> BuiltInRegistries.ITEM.getOptional(id).ifPresentOrElse(item -> {
			boolean replace = json.getAsJsonObject().get(KEY_REPLACE).getAsBoolean();
			Map<Ingredient, Integer> costs = new HashMap<>();
			json.getAsJsonObject().get(KEY_VALUES).getAsJsonArray().forEach(jsonValue -> {
				Ingredient ingredient = Ingredient.CODEC.decode(JsonOps.INSTANCE, jsonValue.getAsJsonObject()).getOrThrow().getFirst();
				int cost = jsonValue.getAsJsonObject().get(KEY_VALUE_COST).getAsInt();
				if (!ingredient.isEmpty()) {
					costs.put(ingredient, cost);
				}
			});
			TinkerersSmithingLoader.INSTANCE.COST_OVERRIDES.put(item, new UnitCostOverride(replace, costs));
		}, () -> TinkerersSmithing.LOGGER.warn("[Tinkerer's Smithing] Failed to override cost for invalid item {}", id)));
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Reloaded unit cost overrides");
	}

	public String getName() {
		return ID.toString();
	}

	public record UnitCostOverride(boolean replace, Map<Ingredient, Integer> costs) {
	}
}
