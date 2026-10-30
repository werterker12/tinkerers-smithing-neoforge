package folk.sisby.tinkerers_smithing;

import folk.sisby.tinkerers_smithing.data.SmithingArmorMaterialLoader;
import folk.sisby.tinkerers_smithing.data.SmithingToolMaterialLoader;
import folk.sisby.tinkerers_smithing.data.SmithingTypeLoader;
import folk.sisby.tinkerers_smithing.data.SmithingUnitCostManager;
import folk.sisby.tinkerers_smithing.packet.S2CPing;
import folk.sisby.tinkerers_smithing.recipe.SacrificeUpgradeRecipe;
import folk.sisby.tinkerers_smithing.recipe.ShapelessRepairRecipe;
import folk.sisby.tinkerers_smithing.recipe.ShapelessUpgradeRecipe;
import folk.sisby.tinkerers_smithing.recipe.SmithingUpgradeRecipe;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TinkerersSmithing implements ModInitializer {
	public static final String ID = "tinkerers_smithing";
	public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	public static final TinkerersSmithingConfig CONFIG = TinkerersSmithingConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", ID, TinkerersSmithingConfig.class);

	public static final TagKey<Item> DEWORK_INGREDIENTS = TagKey.of(RegistryKeys.ITEM, Identifier.of(ID, "dework_ingredients"));
	public static final TagKey<Item> BROKEN_BLACKLIST = TagKey.of(RegistryKeys.ITEM, Identifier.of(ID, "broken_blacklist"));
	public static final TagKey<Enchantment> KEEPERS_IMMUNE = TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(ID, "keepers_immune"));

	// Must load before recipes do, so we can't use the usual fabric route.
	public static final List<ResourceReloader> RECIPE_DEPENDENCY_RELOADERS = List.of(SmithingToolMaterialLoader.INSTANCE, SmithingArmorMaterialLoader.INSTANCE, SmithingUnitCostManager.INSTANCE, SmithingTypeLoader.INSTANCE);

	public static final RecipeSerializer<ShapelessRepairRecipe> SHAPELESS_REPAIR_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(ID, "repair"), new ShapelessRepairRecipe.Serializer());
	public static final RecipeSerializer<SacrificeUpgradeRecipe> SACRIFICE_UPGRADE_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(ID, "sacrifice"), new SacrificeUpgradeRecipe.Serializer());
	public static final RecipeSerializer<SmithingUpgradeRecipe> SMITHING_UPGRADE_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(ID, "smithing"), new SmithingUpgradeRecipe.Serializer());
	public static final RecipeSerializer<ShapelessUpgradeRecipe> SHAPELESS_UPGRADE_SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(ID, "shapeless"), new ShapelessUpgradeRecipe.Serializer());

	public static void generateSmithingData(Map<Identifier, Recipe<?>> recipes) {
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Generating Smithing Data!");
		TinkerersSmithingLoader.INSTANCE.generateItemSmithingData(recipes);
	}

	public static boolean isBroken(ItemStack stack) {
		return !stack.isIn(BROKEN_BLACKLIST) && stack.getDamage() > 0 && stack.getDamage() >= stack.getMaxDamage() - (stack.getItem() instanceof ElytraItem ? 1 : 0);
	}

	public static boolean isBroken(ComponentMap map) {
		return map.getOrDefault(DataComponentTypes.DAMAGE, 0) > 0 && map.getOrDefault(DataComponentTypes.DAMAGE, 0) >= map.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
	}

	public static boolean isKeeper(ItemStack stack) {
		return !stack.isIn(BROKEN_BLACKLIST) && (stack.contains(DataComponentTypes.CUSTOM_NAME) || stack.hasEnchantments() || stack.getItem() instanceof ElytraItem);
	}

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(S2CPing.ID, S2CPing.CODEC);
		LOGGER.info("[Tinkerer's Smithing] Initialized.");
	}
}
