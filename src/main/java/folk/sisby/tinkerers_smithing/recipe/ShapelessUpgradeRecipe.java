package folk.sisby.tinkerers_smithing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.recipeId;

public class ShapelessUpgradeRecipe extends ShapelessRecipe implements ServerRecipe<ShapelessRecipe> {
	public final Item baseItem;
	public final Ingredient addition;
	public final int additionCount;
	public final Item resultItem;

	public ShapelessUpgradeRecipe(Item baseItem, Ingredient addition, int additionCount, Item resultItem) {
		super("", CraftingRecipeCategory.EQUIPMENT, resultItem.getDefaultStack(), assembleIngredients(baseItem, addition, additionCount));
		this.baseItem = baseItem;
		this.addition = addition;
		this.additionCount = additionCount;
		this.resultItem = resultItem;
	}

	public RecipeEntry<ShapelessUpgradeRecipe> toEntry() {
		return new RecipeEntry<>(recipeId("shapeless", resultItem, baseItem), this);
	}

	private static DefaultedList<Ingredient> assembleIngredients(Item item, Ingredient addition, int additionCount) {
		DefaultedList<Ingredient> ingredients = DefaultedList.of();
		ingredients.add(Ingredient.ofItems(item));
		for (int i = 0; i < additionCount; i++) {
			ingredients.add(addition);
		}
		return ingredients;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput recipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
		ItemStack output = super.craft(recipeInput, wrapperLookup);
		for (ItemStack stack : recipeInput.getStacks()) {
			if (stack.isOf(baseItem)) {
				output.applyChanges(stack.getComponentChanges());
			}
		}
		return output;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TinkerersSmithing.SHAPELESS_UPGRADE_SERIALIZER;
	}

	@Override
	public @Nullable RecipeSerializer<ShapelessRecipe> getFallbackSerializer() {
		return RecipeSerializer.SHAPELESS;
	}

	public static class Serializer implements RecipeSerializer<ShapelessUpgradeRecipe> {
		MapCodec<ShapelessUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Registries.ITEM.getCodec().fieldOf("baseItem").forGetter(r -> r.baseItem),
			Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("addition").forGetter(r -> r.addition),
			Codec.INT.fieldOf("additionCount").forGetter(r -> r.additionCount),
			Registries.ITEM.getCodec().fieldOf("resultItem").forGetter(r -> r.resultItem)
		).apply(instance, ShapelessUpgradeRecipe::new));

		PacketCodec<RegistryByteBuf, ShapelessUpgradeRecipe> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.registryValue(RegistryKeys.ITEM), r -> r.baseItem,
			Ingredient.PACKET_CODEC, r -> r.addition,
			PacketCodecs.VAR_INT, r -> r.additionCount,
			PacketCodecs.registryValue(RegistryKeys.ITEM), r -> r.resultItem,
			ShapelessUpgradeRecipe::new
		);

		@Override
		public MapCodec<ShapelessUpgradeRecipe> codec() {
			return CODEC;
		}

		@Override
		public PacketCodec<RegistryByteBuf, ShapelessUpgradeRecipe> packetCodec() {
			return PACKET_CODEC;
		}
	}
}
