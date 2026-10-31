package folk.sisby.tinkerers_smithing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import org.jetbrains.annotations.Nullable;

import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.recipeId;

public class ShapelessUpgradeRecipe extends ShapelessRecipe implements ServerRecipe<ShapelessRecipe> {
	public final Item baseItem;
	public final Ingredient addition;
	public final int additionCount;
	public final Item resultItem;

	public ShapelessUpgradeRecipe(Item baseItem, Ingredient addition, int additionCount, Item resultItem) {
		super("", CraftingBookCategory.EQUIPMENT, resultItem.getDefaultInstance(), assembleIngredients(baseItem, addition, additionCount));
		this.baseItem = baseItem;
		this.addition = addition;
		this.additionCount = additionCount;
		this.resultItem = resultItem;
	}

	public RecipeHolder<ShapelessUpgradeRecipe> toEntry() {
		return new RecipeHolder<>(recipeId("shapeless", resultItem, baseItem), this);
	}

	private static NonNullList<Ingredient> assembleIngredients(Item item, Ingredient addition, int additionCount) {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		ingredients.add(Ingredient.of(item));
		for (int i = 0; i < additionCount; i++) {
			ingredients.add(addition);
		}
		return ingredients;
	}

	@Override
	public ItemStack assemble(CraftingInput recipeInput, HolderLookup.Provider registries) {
		ItemStack output = super.assemble(recipeInput, registries);
		for (ItemStack stack : recipeInput.items()) {
			if (stack.is(baseItem.asItem())) {
				output.applyComponents(stack.getComponentsPatch());
			}
		}
		return output;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TinkerersSmithing.SHAPELESS_UPGRADE_SERIALIZER.get();
	}

	@Override
	public @Nullable RecipeSerializer<ShapelessRecipe> getFallbackSerializer() {
		return RecipeSerializer.SHAPELESS_RECIPE;
	}

	public static class Serializer implements RecipeSerializer<ShapelessUpgradeRecipe> {
		MapCodec<ShapelessUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("baseItem").forGetter(r -> r.baseItem),
			Ingredient.CODEC_NONEMPTY.fieldOf("addition").forGetter(r -> r.addition),
			Codec.INT.fieldOf("additionCount").forGetter(r -> r.additionCount),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("resultItem").forGetter(r -> r.resultItem)
		).apply(instance, ShapelessUpgradeRecipe::new));

		StreamCodec<RegistryFriendlyByteBuf, ShapelessUpgradeRecipe> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.registry(Registries.ITEM), r -> r.baseItem,
			Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition,
			ByteBufCodecs.VAR_INT, r -> r.additionCount,
			ByteBufCodecs.registry(Registries.ITEM), r -> r.resultItem,
			ShapelessUpgradeRecipe::new
		);

		@Override
		public MapCodec<ShapelessUpgradeRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ShapelessUpgradeRecipe> streamCodec() {
			return PACKET_CODEC;
		}
	}
}
