package folk.sisby.tinkerers_smithing.client.jei;

import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingLoader;
import folk.sisby.tinkerers_smithing.recipe.ShapelessRepairRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JeiPlugin
public class TinkerersSmithingJeiPlugin implements IModPlugin {
	public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(TinkerersSmithing.ID, "jei_plugin");

	@Override
	public ResourceLocation getPluginUid() {
		return UID;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		IVanillaRecipeFactory vanillaFactory = registration.getVanillaRecipeFactory();
		List<IJeiAnvilRecipe> anvilRecipes = new ArrayList<>();

		// Anvil Material Repair recipes from ShapelessRepairRecipe
		if (Minecraft.getInstance().level != null) {
			List<RecipeHolder<ShapelessRepairRecipe>> repairRecipes = Minecraft.getInstance().level.getRecipeManager()
				.getAllRecipesFor(RecipeType.CRAFTING).stream()
				.filter(r -> r.value() instanceof ShapelessRepairRecipe)
				.map(r -> (RecipeHolder<ShapelessRepairRecipe>) (RecipeHolder<?>) r)
				.toList();

			for (RecipeHolder<ShapelessRepairRecipe> holder : repairRecipes) {
				ShapelessRepairRecipe recipe = holder.value();
				ItemStack damaged = recipe.baseItem.getDefaultInstance();
				if (damaged.isDamageableItem()) {
					damaged.setDamageValue(damaged.getMaxDamage() * 3 / 4);
				}
				ItemStack repaired = recipe.baseItem.getDefaultInstance();
				List<ItemStack> repairMaterials = Arrays.asList(recipe.addition.getItems());
				if (!repairMaterials.isEmpty()) {
					ResourceLocation recipeId = TinkerersSmithingLoader.recipeId("anvil_repair", recipe.baseItem);
					anvilRecipes.add(vanillaFactory.createAnvilRecipe(
						damaged,
						repairMaterials,
						List.of(repaired),
						recipeId
					));
				}
			}
		}

		// Anvil Deworking recipes (resetting work penalty / repair cost using dework ingredients)
		List<ItemStack> deworkIngredients = BuiltInRegistries.ITEM.getTag(TinkerersSmithing.DEWORK_INGREDIENTS)
			.map(named -> named.stream().map(Holder::value).map(ItemStack::new).toList())
			.filter(list -> !list.isEmpty())
			.orElseGet(() -> List.of(new ItemStack(Items.COPPER_INGOT)));
		if (!deworkIngredients.isEmpty()) {
			List<Item> deworkableItems = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				ItemStack stack = item.getDefaultInstance();
				if (stack.is(ItemTags.VANISHING_ENCHANTABLE) || stack.isDamageableItem()) {
					deworkableItems.add(item);
				}
			}

			for (Item item : deworkableItems) {
				ItemStack penalized = item.getDefaultInstance();
				penalized.set(DataComponents.REPAIR_COST, 15);
				ItemStack deworked = item.getDefaultInstance();
				deworked.set(DataComponents.REPAIR_COST, 0);

				ResourceLocation deworkId = TinkerersSmithingLoader.recipeId("anvil_dework", item);
				anvilRecipes.add(vanillaFactory.createAnvilRecipe(
					penalized,
					deworkIngredients,
					List.of(deworked),
					deworkId
				));
			}
		}

		if (!anvilRecipes.isEmpty()) {
			registration.addRecipes(RecipeTypes.ANVIL, anvilRecipes);
		}

		// Information pages for dework ingredients explaining repair cost penalty removal
		for (ItemStack ingredient : deworkIngredients) {
			registration.addItemStackInfo(
				ingredient,
				Component.translatable("jei.tinkerers_smithing.dework.desc")
			);
		}
	}
}
