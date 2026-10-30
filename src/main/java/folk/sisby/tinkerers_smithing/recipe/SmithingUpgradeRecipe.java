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
import net.minecraft.recipe.SmithingTransformRecipe;
import net.minecraft.recipe.input.SmithingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.Nullable;

import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.recipeId;

public class SmithingUpgradeRecipe extends SmithingTransformRecipe implements ServerRecipe<SmithingTransformRecipe> {
	public final Item baseItem;
	public final int additionCount;
	public final Item resultItem;

	public SmithingUpgradeRecipe(Item baseItem, Ingredient addition, int additionCount, Item resultItem) {
		super(Ingredient.empty(), Ingredient.ofItems(baseItem), addition, getPreviewResult(resultItem, additionCount));
		this.baseItem = baseItem;
		this.additionCount = additionCount;
		this.resultItem = resultItem;
	}

	public RecipeEntry<SmithingUpgradeRecipe> toEntry() {
		return new RecipeEntry<>(recipeId("smithing", resultItem, baseItem), this);
	}

	private static ItemStack getPreviewResult(Item resultItem, int additionCount) {
		ItemStack stack = resultItem.getDefaultStack().copy();
		stack.setDamage(resultDamage(resultItem, additionCount, 1));
		return stack;
	}

	public static int resultDamage(Item resultItem, int additionCount, int usedCount) {
		if (resultItem.getDefaultStack().getMaxDamage() == 0) return additionCount == usedCount ? 0 : 1;
		return Math.min(resultItem.getDefaultStack().getMaxDamage() - 1, (int) Math.floor(resultItem.getDefaultStack().getMaxDamage() * ((additionCount - usedCount) / 4.0)));
	}

	@Override
	public ItemStack craft(SmithingRecipeInput recipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
		ItemStack output = super.craft(recipeInput, wrapperLookup);
		int usedCount = Math.min(additionCount, recipeInput.addition().getCount());
		int damage = resultDamage(output.getItem(), additionCount, usedCount);
		if (damage > output.getMaxDamage()) return ItemStack.EMPTY;
		output.setDamage(damage);
		return output;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TinkerersSmithing.SMITHING_UPGRADE_SERIALIZER;
	}

	@Override
	public @Nullable RecipeSerializer<SmithingTransformRecipe> getFallbackSerializer() {
		return RecipeSerializer.SMITHING_TRANSFORM;
	}

	public static class Serializer implements RecipeSerializer<SmithingUpgradeRecipe> {
		MapCodec<SmithingUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Registries.ITEM.getCodec().fieldOf("baseItem").forGetter(r -> r.baseItem),
			Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("addition").forGetter(r -> r.addition),
			Codec.INT.fieldOf("additionCount").forGetter(r -> r.additionCount),
			Registries.ITEM.getCodec().fieldOf("resultItem").forGetter(r -> r.resultItem)
		).apply(instance, SmithingUpgradeRecipe::new));

		PacketCodec<RegistryByteBuf, SmithingUpgradeRecipe> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.registryValue(RegistryKeys.ITEM), r -> r.baseItem,
			Ingredient.PACKET_CODEC, r -> r.addition,
			PacketCodecs.VAR_INT, r -> r.additionCount,
			PacketCodecs.registryValue(RegistryKeys.ITEM), r -> r.resultItem,
			SmithingUpgradeRecipe::new
		);

		@Override
		public MapCodec<SmithingUpgradeRecipe> codec() {
			return CODEC;
		}

		@Override
		public PacketCodec<RegistryByteBuf, SmithingUpgradeRecipe> packetCodec() {
			return PACKET_CODEC;
		}
	}
}
