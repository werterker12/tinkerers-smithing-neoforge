package folk.sisby.tinkerers_smithing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import net.minecraft.world.World;
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
		super("", CraftingRecipeCategory.EQUIPMENT, getPreviewResult(baseItem), assembleIngredients(baseItem, addition, additionCount));
		this.baseItem = baseItem;
		this.addition = addition;
		this.additionCount = additionCount;
	}

	public RecipeEntry<ShapelessRepairRecipe> toEntry() {
		return new RecipeEntry<>(repairRecipeId(baseItem, addition), this);
	}

	private static ItemStack getPreviewResult(Item baseItem) {
		ItemStack stack = baseItem.getDefaultStack().copy();
		stack.setDamage(1); // Helps signal to vanilla players that the recipe is durability related.
		return stack;
	}

	private static DefaultedList<Ingredient> assembleIngredients(Item item, Ingredient addition, int additionCount) {
		DefaultedList<Ingredient> ingredients = DefaultedList.of();
		Ingredient additionWithAir = Ingredient.ofEntries(Arrays.stream(ArrayUtils.addAll(addition.entries, Ingredient.ofItems(Items.AIR).entries)));
		ingredients.add(Ingredient.ofItems(item));
		for (int i = 0; i < additionCount; i++) {
			ingredients.add(i > 0 ? additionWithAir : addition);
		}
		return ingredients;
	}

	private ItemStack findBase(CraftingRecipeInput recipeInput) {
		List<ItemStack> bases = recipeInput.getStacks().stream().filter(s -> s.isOf(baseItem)).toList();
		return bases.size() == 1 && !bases.get(0).hasEnchantments() && bases.get(0).isDamaged() ? bases.get(0) : null;
	}

	@Override
	public boolean matches(CraftingRecipeInput recipeInput, World world) {
		ItemStack base = findBase(recipeInput);
		long units = recipeInput.getStacks().stream().filter(addition).count();
		long empty = recipeInput.getStacks().stream().filter(ItemStack::isEmpty).count();
		if (base == null || units <= 0 || units > additionCount || empty != (recipeInput.getSize() - units - 1)) return false;

		return base.getDamage() - ((int) Math.ceil((base.getMaxDamage() * (units - 1)) / (double) additionCount)) > 0;
	}

	@Override
	public ItemStack craft(CraftingRecipeInput recipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
		ItemStack base = findBase(recipeInput);
		long units = recipeInput.getStacks().stream().filter(addition).count();
		if (base == null || units <= 0 || units > additionCount) return ItemStack.EMPTY;

		ItemStack output = super.craft(recipeInput, wrapperLookup);
		output.applyChanges(base.getComponentChanges());
		output.setDamage(Math.max(0, base.getDamage() - ((int) Math.ceil((base.getMaxDamage() * units) / (double) additionCount))));
		return output;
	}

	@Override
	public boolean fits(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public boolean isIgnoredInRecipeBook() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TinkerersSmithing.SHAPELESS_REPAIR_SERIALIZER;
	}

	@Override
	public @Nullable RecipeSerializer<ShapelessRecipe> getFallbackSerializer() {
		return RecipeSerializer.SHAPELESS;
	}

	public static class Serializer implements RecipeSerializer<ShapelessRepairRecipe> {
		MapCodec<ShapelessRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Registries.ITEM.getCodec().fieldOf("baseItem").forGetter(r -> r.baseItem),
			Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("addition").forGetter(r -> r.addition),
			Codec.INT.fieldOf("additionCount").forGetter(r -> r.additionCount)
		).apply(instance, ShapelessRepairRecipe::new));

		PacketCodec<RegistryByteBuf, ShapelessRepairRecipe> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.registryValue(RegistryKeys.ITEM), r -> r.baseItem,
			Ingredient.PACKET_CODEC, r -> r.addition,
			PacketCodecs.VAR_INT, r -> r.additionCount,
			ShapelessRepairRecipe::new
		);

		@Override
		public MapCodec<ShapelessRepairRecipe> codec() {
			return CODEC;
		}

		@Override
		public PacketCodec<RegistryByteBuf, ShapelessRepairRecipe> packetCodec() {
			return PACKET_CODEC;
		}
	}
}
