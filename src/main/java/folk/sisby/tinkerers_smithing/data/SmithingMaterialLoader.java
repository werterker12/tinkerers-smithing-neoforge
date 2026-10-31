package folk.sisby.tinkerers_smithing.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SmithingMaterialLoader extends MultiJsonDataLoader {
	public static final String KEY_INHERIT_FROM_ITEM = "inheritFromItem";
	public static final String KEY_REPAIR_MATERIALS = "repairMaterials";
	public static final String KEY_ADD_ITEM = "addItem";
	public static final String KEY_REMOVE_ITEM = "removeItem";
	public static final String KEY_UPGRADES_FROM = "upgradesFrom";
	public static final String KEY_UPGRADES_TO = "upgradesTo";
	public static final String KEY_SACRIFICE_VIA = "sacrificesVia";
	public final TinkerersSmithingMaterial.EQUIPMENT_TYPE type;

	public SmithingMaterialLoader(Gson gson, String dataType, TinkerersSmithingMaterial.EQUIPMENT_TYPE type) {
		super(gson, dataType);
		this.type = type;
	}

	public static Item getOrWarnItem(ResourceLocation id, ResourceLocation resourceId) {
		Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
		if (item == null) {
			TinkerersSmithing.LOGGER.warn("[Tinkerer's Smithing] Couldn't load item {} from {} as it wasn't registered!", id, resourceId);
		}
		return item;
	}

	public static void addOrWarnItem(Collection<Item> collection, ResourceLocation id, ResourceLocation resourceId) {
		Item item = getOrWarnItem(id, resourceId);
		if (item != null) collection.add(item);
	}

	public static void removeOrWarnItem(Collection<Item> collection, ResourceLocation id, ResourceLocation resourceId) {
		Item item = getOrWarnItem(id, resourceId);
		if (item != null) collection.remove(item);
	}

	@Override
	protected void apply(Map<ResourceLocation, Collection<Pair<JsonElement, String>>> prepared, ResourceManager manager, ProfilerFiller profiler) {
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Loading Materials!");
		Map<ResourceLocation, TinkerersSmithingMaterial> outputMap = this.getOutputMap();
		outputMap.clear();
		Map<ResourceLocation, List<ResourceLocation>> upgradeFromMap = new HashMap<>();
		prepared.forEach((id, jsons) -> {
			Set<ResourceLocation> upgradesTo = new HashSet<>();
			List<Ingredient> repairMaterials = new ArrayList<>();
			Set<Item> items = new HashSet<>();
			ResourceLocation sacrificesVia = null;

			for (Pair<JsonElement, String> entry : jsons) {
				JsonObject baseObject = entry.getLeft().getAsJsonObject();
				if (baseObject.has(KEY_INHERIT_FROM_ITEM)) {
					baseObject.get(KEY_INHERIT_FROM_ITEM).getAsJsonArray().forEach(inheritElement -> {
						ResourceLocation inheritItemId = ResourceLocation.tryParse(inheritElement.getAsString());
						if (inheritItemId != null) {
							Item inheritItem = getOrWarnItem(inheritItemId, id);
							if (inheritItem != null) {
								Ingredient repairIngredient = getDefaultRepairIngredient(inheritItem);
								if (repairIngredient != null) {
									repairMaterials.add(repairIngredient);
								}
								BuiltInRegistries.ITEM.forEach(matchingItem -> {
									if (matchingMaterials(inheritItem, matchingItem)) items.add(matchingItem);
								});
							}
						}
					});
				}
				if (baseObject.has(KEY_REPAIR_MATERIALS)) {
					baseObject.get(KEY_REPAIR_MATERIALS).getAsJsonArray().forEach(jsonIngredient -> repairMaterials.add(Ingredient.CODEC.decode(JsonOps.INSTANCE, jsonIngredient).getOrThrow().getFirst()));
				}
				if (baseObject.has(KEY_ADD_ITEM)) {
					baseObject.get(KEY_ADD_ITEM).getAsJsonArray().forEach(jsonItemId -> addOrWarnItem(items, ResourceLocation.parse(jsonItemId.getAsString()), id));
				}
				if (baseObject.has(KEY_REMOVE_ITEM)) {
					baseObject.get(KEY_REMOVE_ITEM).getAsJsonArray().forEach(jsonItemId -> removeOrWarnItem(items, ResourceLocation.parse(jsonItemId.getAsString()), id));
				}
				if (baseObject.has(KEY_UPGRADES_FROM)) {
					baseObject.get(KEY_UPGRADES_FROM).getAsJsonArray().forEach(jsonMaterialId -> upgradeFromMap.computeIfAbsent(ResourceLocation.parse(jsonMaterialId.getAsString()), k -> new ArrayList<>()).add(id));
				}
				if (baseObject.has(KEY_UPGRADES_TO)) {
					baseObject.get(KEY_UPGRADES_TO).getAsJsonArray().forEach(jsonMaterialId -> upgradesTo.add(ResourceLocation.parse(jsonMaterialId.getAsString())));
				}
				if (baseObject.has(KEY_SACRIFICE_VIA)) {
					sacrificesVia = ResourceLocation.parse(baseObject.get(KEY_SACRIFICE_VIA).getAsString());
				}
			}

			outputMap.put(id, new TinkerersSmithingMaterial(this.type, upgradesTo, repairMaterials, items, sacrificesVia));
		});
		upgradeFromMap.forEach((identifier, identifiers) -> {
			if (outputMap.containsKey(identifier)) {
				outputMap.get(identifier).upgradesTo.addAll(identifiers);
			}
		});
		// Remove and warn invalid material references
		outputMap.forEach((id, material) -> {
			material.upgradesTo.stream().filter(i -> !outputMap.containsKey(i)).toList().forEach(removeId -> {
				TinkerersSmithing.LOGGER.warn("[Tinkerer's Smithing] Invalid upgrade {} in {}", removeId, id);
				material.upgradesTo.remove(removeId);
			});
			if (material.sacrificesVia != null && !outputMap.containsKey(material.sacrificesVia)) {
				TinkerersSmithing.LOGGER.warn("[Tinkerer's Smithing] Invalid via {} in {}", material.sacrificesVia, id);
				material.sacrificesVia = null;
			}
		});
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Reloaded a set of {} materials", outputMap.size());
	}

	public abstract Ingredient getDefaultRepairIngredient(Item item);

	public abstract boolean matchingMaterials(Item item1, Item item2);

	public abstract Map<ResourceLocation, TinkerersSmithingMaterial> getOutputMap();
}
