package folk.sisby.tinkerers_smithing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.repairRecipeId;

public class ShapelessRepairRecipe extends ShapelessRecipe implements ServerRecipe<ShapelessRecipe> {
	public final Item baseItem;
	public final Ingredient addition;
	public final int additionCount;

	public ShapelessRepairRecipe(Item baseItem, Ingredient addition, int additionCount) {
		super("", CraftingBookCategory.EQUIPMENT, getPreviewResult(baseItem), assembleIngredients(baseItem, addition, additionCount));
		this.baseItem = baseItem;
		this.addition = addition;
		this.additionCount = additionCount;
	}

	public RecipeHolder<ShapelessRepairRecipe> toEntry() {
		return new RecipeHolder<>(repairRecipeId(baseItem, addition), this);
	}

	private static ItemStack getPreviewResult(Item baseItem) {
		ItemStack stack = baseItem.getDefaultInstance().copy();
		stack.setDamageValue(1); // Helps signal to vanilla players that the recipe is durability related.
		return stack;
	}

	private static NonNullList<Ingredient> assembleIngredients(Item item, Ingredient addition, int additionCount) {
		NonNullList<Ingredient> ingredients = NonNullList.create();
		Ingredient additionWithAir = Ingredient.fromValues(Arrays.stream(ArrayUtils.addAll(addition.values, Ingredient.of(Items.AIR).values)));
		ingredients.add(Ingredient.of(item));
		for (int i = 0; i < additionCount; i++) {
			ingredients.add(i > 0 ? additionWithAir : addition);
		}
		return ingredients;
	}

	private ItemStack findBase(CraftingInput recipeInput) {
		List<ItemStack> bases = recipeInput.items().stream().filter(s -> s.is(baseItem.asItem())).toList();
		return bases.size() == 1 && !bases.get(0).isEnchanted() && bases.get(0).isDamaged() ? bases.get(0) : null;
	}

	@Override
	public boolean matches(CraftingInput recipeInput, net.minecraft.world.level.Level world) {
		ItemStack base = findBase(recipeInput);
		long units = recipeInput.items().stream().filter(addition).count();
		long empty = recipeInput.items().stream().filter(ItemStack::isEmpty).count();
		if (base == null || units <= 0 || units > additionCount || empty != (recipeInput.size() - units - 1)) return false;

		return base.getDamageValue() - ((int) Math.ceil((base.getMaxDamage() * (units - 1)) / (double) additionCount)) > 0;
	}

	@Override
	public ItemStack assemble(CraftingInput recipeInput, HolderLookup.Provider registries) {
		ItemStack base = findBase(recipeInput);
		long units = recipeInput.items().stream().filter(addition).count();
		if (base == null || units <= 0 || units > additionCount) return ItemStack.EMPTY;

		ItemStack output = super.assemble(recipeInput, registries);
		output.applyComponents(base.getComponentsPatch());
		output.setDamageValue(Math.max(0, base.getDamageValue() - ((int) Math.ceil((base.getMaxDamage() * units) / (double) additionCount))));
		return output;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TinkerersSmithing.SHAPELESS_REPAIR_SERIALIZER.get();
	}

	@Override
	public @Nullable RecipeSerializer<ShapelessRecipe> getFallbackSerializer() {
		return RecipeSerializer.SHAPELESS_RECIPE;
	}

	public static class Serializer implements RecipeSerializer<ShapelessRepairRecipe> {
		MapCodec<ShapelessRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("baseItem").forGetter(r -> r.baseItem),
			Ingredient.CODEC_NONEMPTY.fieldOf("addition").forGetter(r -> r.addition),
			Codec.INT.fieldOf("additionCount").forGetter(r -> r.additionCount)
		).apply(instance, ShapelessRepairRecipe::new));

		StreamCodec<RegistryFriendlyByteBuf, ShapelessRepairRecipe> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.registry(Registries.ITEM), r -> r.baseItem,
			Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition,
			ByteBufCodecs.VAR_INT, r -> r.additionCount,
			ShapelessRepairRecipe::new
		);

		@Override
		public MapCodec<ShapelessRepairRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ShapelessRepairRecipe> streamCodec() {
			return PACKET_CODEC;
		}
	}
}
