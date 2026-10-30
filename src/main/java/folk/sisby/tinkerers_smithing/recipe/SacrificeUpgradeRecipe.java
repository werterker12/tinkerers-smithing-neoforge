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

import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.appendId;
import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.recipeId;

public class SacrificeUpgradeRecipe extends SmithingTransformRecipe implements ServerRecipe<SmithingTransformRecipe> {
	public final Item baseItem;
	public final int additionUnits;
	public final Item resultItem;
	public final int resultUnits;

	public SacrificeUpgradeRecipe(Item baseItem, Ingredient addition, int additionUnits, Item resultItem, int resultUnits) {
		super(Ingredient.empty(), Ingredient.ofItems(baseItem), addition, getPreviewResult(resultItem, additionUnits, resultUnits));
		this.baseItem = baseItem;
		this.additionUnits = additionUnits;
		this.resultItem = resultItem;
		this.resultUnits = resultUnits;
	}

	public RecipeEntry<SacrificeUpgradeRecipe> toEntry() {
		return new RecipeEntry<>(appendId(recipeId("sacrifice", resultItem, baseItem), String.valueOf(additionUnits)), this);
	}

	private static ItemStack getPreviewResult(Item resultItem, int additionUnits, int resultUnits) {
		ItemStack stack = resultItem.getDefaultStack().copy();
		stack.setDamage(resultDamage(resultItem, additionUnits, resultUnits, 0, 1));
		return stack;
	}

	public static int resultDamage(Item resultItem, int additionUnits, int resultUnits, int additionDamage, int additionMaxDamage) {
		if (resultItem.getDefaultStack().getMaxDamage() == 0) return additionUnits * (additionDamage / (double) additionMaxDamage) >= resultUnits ? 0 : 1;
		return (int) Math.ceil(resultItem.getDefaultStack().getMaxDamage() - ((additionMaxDamage - additionDamage) * ((double) additionUnits * resultItem.getDefaultStack().getMaxDamage()) / ((double) additionMaxDamage * resultUnits)));
	}

	@Override
	public ItemStack craft(SmithingRecipeInput recipeInput, RegistryWrapper.WrapperLookup wrapperLookup) {
		ItemStack output = super.craft(recipeInput, wrapperLookup);
		ItemStack addition = recipeInput.addition();
		int damage = resultDamage(output.getItem(), additionUnits, resultUnits, addition.getDamage(), addition.getMaxDamage());
		if (damage > output.getMaxDamage()) return ItemStack.EMPTY;
		output.setDamage(damage);
		return output;
	}

	@Override
	public boolean isIgnoredInRecipeBook() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TinkerersSmithing.SACRIFICE_UPGRADE_SERIALIZER;
	}

	@Override
	public @Nullable RecipeSerializer<SmithingTransformRecipe> getFallbackSerializer() {
		return RecipeSerializer.SMITHING_TRANSFORM;
	}

	public static class Serializer implements RecipeSerializer<SacrificeUpgradeRecipe> {
		MapCodec<SacrificeUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Registries.ITEM.getCodec().fieldOf("baseItem").forGetter(r -> r.baseItem),
			Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("addition").forGetter(r -> r.addition),
			Codec.INT.fieldOf("additionUnits").forGetter(r -> r.additionUnits),
			Registries.ITEM.getCodec().fieldOf("resultItem").forGetter(r -> r.resultItem),
			Codec.INT.fieldOf("resultUnits").forGetter(r -> r.resultUnits)
		).apply(instance, SacrificeUpgradeRecipe::new));

		PacketCodec<RegistryByteBuf, SacrificeUpgradeRecipe> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.registryValue(RegistryKeys.ITEM), r -> r.baseItem,
			Ingredient.PACKET_CODEC, r -> r.addition,
			PacketCodecs.VAR_INT, r -> r.additionUnits,
			PacketCodecs.registryValue(RegistryKeys.ITEM), r -> r.resultItem,
			PacketCodecs.VAR_INT, r -> r.resultUnits,
			SacrificeUpgradeRecipe::new
		);

		@Override
		public MapCodec<SacrificeUpgradeRecipe> codec() {
			return CODEC;
		}

		@Override
		public PacketCodec<RegistryByteBuf, SacrificeUpgradeRecipe> packetCodec() {
			return PACKET_CODEC;
		}
	}
}
