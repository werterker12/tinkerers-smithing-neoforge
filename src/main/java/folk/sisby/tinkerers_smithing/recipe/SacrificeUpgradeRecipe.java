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
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.Nullable;

import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.appendId;
import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.recipeId;

public class SacrificeUpgradeRecipe extends SmithingTransformRecipe implements ServerRecipe<SmithingTransformRecipe> {
	public final Item baseItem;
	public final int additionUnits;
	public final Item resultItem;
	public final int resultUnits;

	public SacrificeUpgradeRecipe(Item baseItem, Ingredient addition, int additionUnits, Item resultItem, int resultUnits) {
		super(Ingredient.of(), Ingredient.of(baseItem), addition, getPreviewResult(resultItem, additionUnits, resultUnits));
		this.baseItem = baseItem;
		this.additionUnits = additionUnits;
		this.resultItem = resultItem;
		this.resultUnits = resultUnits;
	}

	public RecipeHolder<SacrificeUpgradeRecipe> toEntry() {
		return new RecipeHolder<>(appendId(recipeId("sacrifice", resultItem, baseItem), String.valueOf(additionUnits)), this);
	}

	private static ItemStack getPreviewResult(Item resultItem, int additionUnits, int resultUnits) {
		ItemStack stack = resultItem.getDefaultInstance().copy();
		stack.setDamageValue(resultDamage(resultItem, additionUnits, resultUnits, 0, 1));
		return stack;
	}

	public static int resultDamage(Item resultItem, int additionUnits, int resultUnits, int additionDamage, int additionMaxDamage) {
		if (resultItem.getDefaultInstance().getMaxDamage() == 0) return additionUnits * (additionDamage / (double) additionMaxDamage) >= resultUnits ? 0 : 1;
		return (int) Math.ceil(resultItem.getDefaultInstance().getMaxDamage() - ((additionMaxDamage - additionDamage) * ((double) additionUnits * resultItem.getDefaultInstance().getMaxDamage()) / ((double) additionMaxDamage * resultUnits)));
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput recipeInput, HolderLookup.Provider registries) {
		ItemStack output = super.assemble(recipeInput, registries);
		ItemStack addition = recipeInput.addition();
		int damage = resultDamage(output.getItem(), additionUnits, resultUnits, addition.getDamageValue(), addition.getMaxDamage());
		if (damage > output.getMaxDamage()) return ItemStack.EMPTY;
		output.setDamageValue(damage);
		return output;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TinkerersSmithing.SACRIFICE_UPGRADE_SERIALIZER.get();
	}

	@Override
	public @Nullable RecipeSerializer<SmithingTransformRecipe> getFallbackSerializer() {
		return RecipeSerializer.SMITHING_TRANSFORM;
	}

	public static class Serializer implements RecipeSerializer<SacrificeUpgradeRecipe> {
		MapCodec<SacrificeUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("baseItem").forGetter(r -> r.baseItem),
			Ingredient.CODEC_NONEMPTY.fieldOf("addition").forGetter(r -> r.addition),
			Codec.INT.fieldOf("additionUnits").forGetter(r -> r.additionUnits),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("resultItem").forGetter(r -> r.resultItem),
			Codec.INT.fieldOf("resultUnits").forGetter(r -> r.resultUnits)
		).apply(instance, SacrificeUpgradeRecipe::new));

		StreamCodec<RegistryFriendlyByteBuf, SacrificeUpgradeRecipe> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.registry(Registries.ITEM), r -> r.baseItem,
			Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition,
			ByteBufCodecs.VAR_INT, r -> r.additionUnits,
			ByteBufCodecs.registry(Registries.ITEM), r -> r.resultItem,
			ByteBufCodecs.VAR_INT, r -> r.resultUnits,
			SacrificeUpgradeRecipe::new
		);

		@Override
		public MapCodec<SacrificeUpgradeRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, SacrificeUpgradeRecipe> streamCodec() {
			return PACKET_CODEC;
		}
	}
}
