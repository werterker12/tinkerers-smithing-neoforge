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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Mod(TinkerersSmithing.ID)
public class TinkerersSmithing {
    public static final String ID = "tinkerers_smithing_byw";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    public static final TagKey<Item> DEWORK_INGREDIENTS = TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ID, "dework_ingredients"));
    public static final TagKey<Item> BROKEN_BLACKLIST = TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath(ID, "broken_blacklist"));
    public static final TagKey<Enchantment> KEEPERS_IMMUNE = TagKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(ID, "keepers_immune"));

    // Must load before recipes do - registered as server data reloaders
    public static final List<net.minecraft.server.packs.resources.PreparableReloadListener> RECIPE_DEPENDENCY_RELOADERS = List.of(
        SmithingToolMaterialLoader.INSTANCE,
        SmithingArmorMaterialLoader.INSTANCE,
        SmithingUnitCostManager.INSTANCE,
        SmithingTypeLoader.INSTANCE
    );

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, ID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapelessRepairRecipe>> SHAPELESS_REPAIR_SERIALIZER =
        RECIPE_SERIALIZERS.register("repair", ShapelessRepairRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SacrificeUpgradeRecipe>> SACRIFICE_UPGRADE_SERIALIZER =
        RECIPE_SERIALIZERS.register("sacrifice", SacrificeUpgradeRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SmithingUpgradeRecipe>> SMITHING_UPGRADE_SERIALIZER =
        RECIPE_SERIALIZERS.register("smithing", SmithingUpgradeRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShapelessUpgradeRecipe>> SHAPELESS_UPGRADE_SERIALIZER =
        RECIPE_SERIALIZERS.register("shapeless", ShapelessUpgradeRecipe.Serializer::new);

    public TinkerersSmithing(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(TinkerersSmithingConfig.getType(), TinkerersSmithingConfig.SPEC);

        // Register recipe serializers
        RECIPE_SERIALIZERS.register(modBus);

        // Register networking payload
        modBus.addListener(this::registerPayloads);

        LOGGER.info("[Tinkerer's Smithing] Initialized.");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(ID).playToClient(
            S2CPing.TYPE,
            S2CPing.STREAM_CODEC,
            (packet, ctx) -> {}
        );
    }

    public static void generateSmithingData(Map<ResourceLocation, Recipe<?>> recipes) {
        TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Generating Smithing Data!");
        TinkerersSmithingLoader.INSTANCE.generateItemSmithingData(recipes);
    }

    public static boolean isBroken(ItemStack stack) {
        return !stack.is(BROKEN_BLACKLIST)
            && stack.getDamageValue() > 0
            && stack.getDamageValue() >= stack.getMaxDamage() - (stack.getItem() instanceof ElytraItem ? 1 : 0);
    }

    public static boolean isBroken(net.minecraft.core.component.DataComponentMap map) {
        return map.getOrDefault(DataComponents.DAMAGE, 0) > 0
            && map.getOrDefault(DataComponents.DAMAGE, 0) >= map.getOrDefault(DataComponents.MAX_DAMAGE, 0);
    }

    public static boolean isKeeper(ItemStack stack) {
        return !stack.is(BROKEN_BLACKLIST)
            && (stack.has(DataComponents.CUSTOM_NAME) || stack.isEnchanted() || stack.getItem() instanceof ElytraItem);
    }
}
