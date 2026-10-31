package folk.sisby.tinkerers_smithing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.tinkerers_smithing.data.SmithingUnitCostManager;
import folk.sisby.tinkerers_smithing.recipe.SacrificeUpgradeRecipe;
import folk.sisby.tinkerers_smithing.recipe.ShapelessRepairRecipe;
import folk.sisby.tinkerers_smithing.recipe.ShapelessUpgradeRecipe;
import folk.sisby.tinkerers_smithing.recipe.SmithingUpgradeRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static folk.sisby.tinkerers_smithing.TinkerersSmithing.ID;
import static folk.sisby.tinkerers_smithing.TinkerersSmithing.LOGGER;

public class TinkerersSmithingLoader {
	public static final TinkerersSmithingLoader INSTANCE = new TinkerersSmithingLoader();

	public final Map<ResourceLocation, Collection<Item>> SMITHING_TYPES = new HashMap<>();
	public final Map<ResourceLocation, TinkerersSmithingMaterial> TOOL_MATERIALS = new HashMap<>();
	public final Map<ResourceLocation, TinkerersSmithingMaterial> ARMOR_MATERIALS = new HashMap<>();
	public final Map<Item, SmithingUnitCostManager.UnitCostOverride> COST_OVERRIDES = new HashMap<>();

	public final List<RecipeHolder<?>> RECIPES = new ArrayList<>();

	public static ResourceLocation recipeId(String recipeType, String... names) {
		return ResourceLocation.fromNamespaceAndPath(ID, recipeType + "/" + String.join("/", names));
	}

	public static ResourceLocation recipeId(String recipeType, ResourceLocation... ids) {
		return recipeId(recipeType, Arrays.stream(ids).map(id -> id.getNamespace().equals("minecraft") ? id.getPath() : id.getNamespace() + "/" + id.getPath()).toArray(String[]::new));
	}

	public static ResourceLocation recipeId(String recipeType, Item... items) {
		return recipeId(recipeType, Arrays.stream(items).map(BuiltInRegistries.ITEM::getKey).toArray(ResourceLocation[]::new));
	}

	public static ResourceLocation appendId(ResourceLocation id, String name) {
		return ResourceLocation.parse(id.toString() + "/" + name);
	}

	public static ResourceLocation repairRecipeId(Item baseItem, Ingredient ingredient) {
		if (ingredient.values.length == 0) {
			throw new IllegalArgumentException("Ingredients for Tinkerer's Smithing recipes can't be empty! When repairing item %s".formatted(BuiltInRegistries.ITEM.getKey(baseItem)));
		}
		ResourceLocation ingredientId = ingredient.values[0] instanceof Ingredient.ItemValue se
			? BuiltInRegistries.ITEM.getKey(se.item().getItem())
			: ingredient.values[0] instanceof Ingredient.TagValue te
			? te.tag().location()
			: ResourceLocation.parse("error");
		return recipeId("repair", BuiltInRegistries.ITEM.getKey(baseItem), ingredientId);
	}

	public void generateItemSmithingData(Map<ResourceLocation, Recipe<?>> recipes) {
		new LoaderRun().generateItemSmithingData(recipes);
		SMITHING_TYPES.clear();
		TOOL_MATERIALS.clear();
		ARMOR_MATERIALS.clear();
		COST_OVERRIDES.clear();
	}

	public class LoaderRun {
		public final List<ResourceLocation> WARN_NO_RECIPE = new ArrayList<>();
		public final List<ResourceLocation> WARN_NO_MATERIALS = new ArrayList<>();
		public final Map<ResourceLocation, Holder<ArmorMaterial>> WARN_DEFAULT_MATERIAL_ARMOR = new HashMap<>();
		public final Map<ResourceLocation, Tier> WARN_DEFAULT_MATERIAL_TOOL = new HashMap<>();
		public int INFO_ADDED_COSTS = 0;
		public int INFO_ADDED_COST_ITEMS = 0;
		public int INFO_ADDED_UPGRADES = 0;
		public int INFO_ADDED_UPGRADE_ITEMS = 0;
		public int INFO_ADDED_SACRIFICES = 0;
		public int INFO_ADDED_SACRIFICE_ITEMS = 0;

		public List<TinkerersSmithingMaterial> getAllMaterials() {
			List<TinkerersSmithingMaterial> outList = new ArrayList<>();
			outList.addAll(TOOL_MATERIALS.values());
			outList.addAll(ARMOR_MATERIALS.values());
			return outList;
		}

		public List<Ingredient> getMaterialRepairIngredients(BiConsumer<ResourceLocation, Holder<ArmorMaterial>> armorDefaults, BiConsumer<ResourceLocation, Tier> toolDefaults, Item item) {
			List<Ingredient> outList = new ArrayList<>();
			boolean noneMatch = true;

			for (TinkerersSmithingMaterial material : getAllMaterials()) {
				if (material.items.contains(item)) {
					noneMatch = false;
					material.repairMaterials.stream().filter(i -> !i.isEmpty()).forEach(outList::add);
				}
			}

			if (noneMatch && item instanceof ArmorItem ai && ai.getMaterial() != null) {
				armorDefaults.accept(BuiltInRegistries.ITEM.getKey(item), ai.getMaterial());
			}
			if (noneMatch && item instanceof TieredItem ti && ti.getTier() != null) {
				toolDefaults.accept(BuiltInRegistries.ITEM.getKey(item), ti.getTier());
			}

			if (outList.isEmpty()) {
				if (item.getDefaultInstance().isDamageableItem() && item instanceof ArmorItem ai) {
					ArmorMaterial material = ai.getMaterial().value();
					if (material != null) {
						Ingredient repairIngredient = material.repairIngredient().get();
						if (repairIngredient != null && !repairIngredient.isEmpty()) {
							outList.add(repairIngredient);
						}
					}
				}
				if (item.getDefaultInstance().isDamageableItem() && item instanceof TieredItem ti) {
					Tier material = ti.getTier();
					if (material != null) {
						Ingredient repairIngredient = material.getRepairIngredient();
						if (repairIngredient != null && !repairIngredient.isEmpty()) {
							outList.add(repairIngredient);
						}
					}
				}
			}

			return outList;
		}

		public Collection<Item> getUpgradePaths(Item item) {
			Set<Item> outSet = new HashSet<>();

			List<Collection<Item>> types = new ArrayList<>();
			SMITHING_TYPES.forEach((id, items) -> {
				if (items.contains(item)) types.add(items);
			});

			if (!types.isEmpty()) {
				for (TinkerersSmithingMaterial material : getAllMaterials()) {
					if (material.items.contains(item)) {
						Map<ResourceLocation, TinkerersSmithingMaterial> map = material.type == TinkerersSmithingMaterial.EQUIPMENT_TYPE.ARMOR ? ARMOR_MATERIALS : TOOL_MATERIALS;
						for (ResourceLocation id : material.upgradesTo) {
							TinkerersSmithingMaterial upgradeMaterial = map.get(id);
							if (upgradeMaterial.sacrificesVia == null) {
								for (Item upgradeItem : upgradeMaterial.items) {
									if (types.stream().anyMatch(type -> type.contains(upgradeItem))) {
										outSet.add(upgradeItem);
									}
								}
							}
						}
					}
				}
			}

			if (!outSet.isEmpty()) {
				INFO_ADDED_UPGRADE_ITEMS++;
				INFO_ADDED_UPGRADES += outSet.size();
			}

			return outSet;
		}

		public Map<Ingredient, Integer> getUnitCosts(Item item, Map<ResourceLocation, Recipe<?>> recipes) {
			Map<Ingredient, Integer> outMap = new HashMap<>();

			ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

			SmithingUnitCostManager.UnitCostOverride override = COST_OVERRIDES.get(item);
			List<Ingredient> repairIngredients = getMaterialRepairIngredients(WARN_DEFAULT_MATERIAL_ARMOR::put, WARN_DEFAULT_MATERIAL_TOOL::put, item);

			if ((override == null || !override.replace()) && repairIngredients.isEmpty() && item.getDefaultInstance().isDamageableItem()) {
				WARN_NO_MATERIALS.add(itemId);
			}

			if ((override == null || !override.replace()) && !repairIngredients.isEmpty()) {

				// Naively calculate unit cost by testing the recipe with the same ID as the item itself (ignoring path directories)
				List<ResourceLocation> recipeIds = new ArrayList<>();
				recipes.keySet().stream().filter(id -> id.getNamespace().equals(itemId.getNamespace()) && itemId.getPath().equals(id.getPath().substring(id.getPath().lastIndexOf('/') + 1))).forEach(recipeIds::add);
				RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
				for (ResourceLocation recipeId : recipeIds) {
					Recipe<?> recipe = recipes.get(recipeId);
					if (recipe != null) {
						if (recipe.getResultItem(registryAccess).is(item)) {
							for (Ingredient repairIngredient : repairIngredients) {
								int unitCost = Math.toIntExact(recipe.getIngredients().stream()
									.filter(craftingIngredient -> TinkerersSmithingConfig.INSTANCE.matchesOrEquivalent(repairIngredient, craftingIngredient))
									.count());
								if (unitCost > 0) {
									outMap.put(repairIngredient, unitCost);
								}
							}
						}
					}
				}
				if (recipeIds.isEmpty()) {
					WARN_NO_RECIPE.add(itemId);
				}
			}
			if (override != null) {
				outMap.putAll(override.costs());
				if (override.replace()) {
					WARN_DEFAULT_MATERIAL_TOOL.remove(itemId);
					WARN_DEFAULT_MATERIAL_ARMOR.remove(itemId);
				}
			}

			if (!outMap.isEmpty()) {
				INFO_ADDED_COST_ITEMS++;
				INFO_ADDED_COSTS += outMap.size();
			}

			return outMap;
		}

		public Map<Item, Pair<Integer, Multimap<Integer, Item>>> getSacrificePaths(Item item, Map<Item, Map<Ingredient, Integer>> unitCosts) {
			Map<Item, Pair<Integer, Multimap<Integer, Item>>> outMap = new HashMap<>();

			List<Collection<Item>> types = new ArrayList<>();
			SMITHING_TYPES.forEach((id, items) -> {
				if (items.contains(item)) types.add(items);
			});

			if (!types.isEmpty()) {
				for (TinkerersSmithingMaterial material : getAllMaterials()) {
					if (material.items.contains(item)) {
						Map<ResourceLocation, TinkerersSmithingMaterial> map = material.type == TinkerersSmithingMaterial.EQUIPMENT_TYPE.ARMOR ? ARMOR_MATERIALS : TOOL_MATERIALS;
						Map<ResourceLocation, TinkerersSmithingMaterial> otherMap = material.type == TinkerersSmithingMaterial.EQUIPMENT_TYPE.ARMOR ? TOOL_MATERIALS : ARMOR_MATERIALS;
						for (ResourceLocation upgradeId : material.upgradesTo) {
							TinkerersSmithingMaterial upgradeMaterial = map.get(upgradeId);
							if (upgradeMaterial.sacrificesVia != null) {
								List<Item> sacrificesViaItems = new ArrayList<>(map.get(upgradeMaterial.sacrificesVia).items);
								List<Item> sacrificeItems = new ArrayList<>(map.get(upgradeId).items);

								if (otherMap.containsKey(upgradeId)) {
									TinkerersSmithingMaterial otherUpgradeMaterial = otherMap.get(upgradeId);
									if (otherUpgradeMaterial.sacrificesVia.equals(upgradeMaterial.sacrificesVia)) {
										sacrificesViaItems.addAll(otherMap.get(otherUpgradeMaterial.sacrificesVia).items);
										sacrificeItems.addAll(otherUpgradeMaterial.items);
									}
								}

								for (Item upgradeItem : upgradeMaterial.items) {
									if (types.stream().noneMatch(type -> type.contains(upgradeItem))) continue;
									Multimap<Integer, Item> sacrifices = HashMultimap.create();
									int upgradeViaCost = 0;
									for (Item viaItem : map.get(upgradeMaterial.sacrificesVia).items) {
										if (types.stream().anyMatch(type -> type.contains(viaItem))) {
											upgradeViaCost = unitCosts.get(viaItem).values().stream().findFirst().orElse(0);
										}
									}
									if (upgradeViaCost > 0) {
										for (Item sacrificeItem : sacrificeItems) {
											List<Collection<Item>> sacrificeTypes = new ArrayList<>();
											for (Collection<Item> items : SMITHING_TYPES.values()) {
												if (items.contains(sacrificeItem)) sacrificeTypes.add(items);
											}
											int sacrificesViaCost = 0;
											for (Item viaItem : sacrificesViaItems) {
												if (sacrificeTypes.stream().anyMatch(type -> type.contains(viaItem))) {
													sacrificesViaCost = unitCosts.get(viaItem).values().stream().findFirst().orElse(0);
												}
											}
											if (sacrificesViaCost > 0) {
												sacrifices.put(sacrificesViaCost, sacrificeItem);
											}
										}
										if (!sacrifices.isEmpty()) {
											outMap.put(upgradeItem, Pair.of(upgradeViaCost, sacrifices));
										}
									}
								}
							}
						}
					}
				}
			}

			if (!outMap.isEmpty()) {
				INFO_ADDED_SACRIFICE_ITEMS++;
				INFO_ADDED_SACRIFICES += outMap.size();
			}

			return outMap;
		}

		public void generateItemSmithingData(Map<ResourceLocation, Recipe<?>> recipes) {
			LOGGER.info("[Tinkerer's Smithing] Data Initializing.");
			RECIPES.clear();
			Map<Item, Map<Ingredient, Integer>> unitCosts = new HashMap<>();
			for (Item item : BuiltInRegistries.ITEM) {
				Map<Ingredient, Integer> unitCost = getUnitCosts(item, recipes);
				if (item.getDefaultInstance().getMaxDamage() > 0) {
					unitCost.forEach((unit, count) -> RECIPES.add(new ShapelessRepairRecipe(item, unit, count).toEntry()));
				}
				unitCosts.put(item, unitCost);
			}
			for (Item base : BuiltInRegistries.ITEM) {
				for (Item result : getUpgradePaths(base)) {
					unitCosts.get(result).forEach((addition, additionCount) -> {
						RECIPES.add(new SmithingUpgradeRecipe(base, addition, additionCount, result).toEntry());
						RECIPES.add(new ShapelessUpgradeRecipe(base, addition, additionCount, result).toEntry());
					});
				}
				getSacrificePaths(base, unitCosts).forEach((result, sacrifice) -> sacrifice.getRight().keySet().forEach(additionUnits -> {
					Collection<Item> additions = sacrifice.getRight().get(additionUnits);
					if (result.getDefaultInstance().getMaxDamage() > 0) {
						RECIPES.add(new SacrificeUpgradeRecipe(base, Ingredient.of(additions.toArray(Item[]::new)), additionUnits, result, sacrifice.getLeft()).toEntry());
					}
				}));
			}
			LOGGER.info("[Tinkerer's Smithing] Registered {} Tool Materials with {} items: [{}]", TOOL_MATERIALS.size(), TOOL_MATERIALS.values().stream().map(m -> m.items.size()).reduce(Integer::sum).orElse(0), TOOL_MATERIALS.entrySet().stream().map(e -> e.getKey().toString() + "(" + e.getValue().items.size() + ")").collect(Collectors.joining(", ")));
			LOGGER.info("[Tinkerer's Smithing] Registered {} Armor Materials with {} items: [{}].", ARMOR_MATERIALS.size(), ARMOR_MATERIALS.values().stream().map(m -> m.items.size()).reduce(Integer::sum).orElse(0), ARMOR_MATERIALS.entrySet().stream().map(e -> e.getKey().toString() + "(" + e.getValue().items.size() + ")").collect(Collectors.joining(", ")));
			LOGGER.info("[Tinkerer's Smithing] Registered {} Equipment Types with {} items: [{}]", SMITHING_TYPES.size(), SMITHING_TYPES.values().stream().map(Collection::size).reduce(Integer::sum).orElse(0), SMITHING_TYPES.entrySet().stream().map(e -> e.getKey().toString() + "(" + e.getValue().size() + ")").collect(Collectors.joining(", ")));
			LOGGER.info("[Tinkerer's Smithing] Applied {} Unit Costs to {} items", INFO_ADDED_COSTS, INFO_ADDED_COST_ITEMS);
			LOGGER.info("[Tinkerer's Smithing] Applied {} Upgrade Paths to {} items", INFO_ADDED_UPGRADES, INFO_ADDED_UPGRADE_ITEMS);
			LOGGER.info("[Tinkerer's Smithing] Applied {} Sacrifice Paths to {} items", INFO_ADDED_SACRIFICES, INFO_ADDED_SACRIFICE_ITEMS);
			if (!WARN_DEFAULT_MATERIAL_ARMOR.isEmpty()) {
				Set<Holder<ArmorMaterial>> armorMats = new HashSet<>(WARN_DEFAULT_MATERIAL_ARMOR.values());
				LOGGER.warn("[Tinkerer's Smithing] Found {} unregistered armor materials with {} armor items: [{}] - items: [{}]", armorMats.size(), WARN_DEFAULT_MATERIAL_ARMOR.size(), armorMats.stream().map(h -> h.unwrapKey().map(k -> k.location().toString()).orElse("unknown")).collect(Collectors.joining(", ")), WARN_DEFAULT_MATERIAL_ARMOR.entrySet().stream()
					.collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList()))).entrySet().stream() // Invert Map
					.map(e -> "%s[%s]".formatted(e.getKey().unwrapKey().map(k -> k.location().toString()).orElse("unknown"), e.getValue().stream().map(ResourceLocation::toString).collect(Collectors.joining(", ")))).collect(Collectors.joining(", "))); // Stringify
			}
			if (!WARN_DEFAULT_MATERIAL_TOOL.isEmpty()) {
				Set<Tier> toolMats = new HashSet<>(WARN_DEFAULT_MATERIAL_TOOL.values());
				LOGGER.warn("[Tinkerer's Smithing] Found {} unregistered tool materials with {} tool items: [{}] - items: [{}]", toolMats.size(), WARN_DEFAULT_MATERIAL_TOOL.size(), toolMats.stream().map(Object::toString).collect(Collectors.joining(", ")), WARN_DEFAULT_MATERIAL_TOOL.entrySet().stream()
					.collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList()))).entrySet().stream() // Invert Map
					.map(e -> "%s[%s]".formatted(e.getKey().toString(), e.getValue().stream().map(ResourceLocation::toString).collect(Collectors.joining(", ")))).collect(Collectors.joining(", "))); // Stringify
			}
			if (!WARN_NO_RECIPE.isEmpty())
				LOGGER.warn("[Tinkerer's Smithing] Found {} equipment items without unit cost recipes: [{}]", WARN_NO_RECIPE.size(), WARN_NO_RECIPE.stream().map(ResourceLocation::toString).collect(Collectors.joining(", ")));
			if (!WARN_NO_MATERIALS.isEmpty())
				LOGGER.warn("[Tinkerer's Smithing] Found {} damageable items without repair materials: [{}]", WARN_NO_MATERIALS.size(), WARN_NO_MATERIALS.stream().map(ResourceLocation::toString).collect(Collectors.joining(", ")));
			LOGGER.info("[Tinkerer's Smithing] Data Initialized!");
		}
	}
}
