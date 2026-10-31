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

import static folk.sisby.tinkerers_smithing.TinkerersSmithingLoader.recipeId;

public class SmithingUpgradeRecipe extends SmithingTransformRecipe implements ServerRecipe<SmithingTransformRecipe> {
	public final Item baseItem;
	public final int additionCount;
	public final Item resultItem;

	public SmithingUpgradeRecipe(Item baseItem, Ingredient addition, int additionCount, Item resultItem) {
		super(Ingredient.of(), Ingredient.of(baseItem), addition, getPreviewResult(resultItem, additionCount));
		this.baseItem = baseItem;
		this.additionCount = additionCount;
		this.resultItem = resultItem;
	}

	public RecipeHolder<SmithingUpgradeRecipe> toEntry() {
		return new RecipeHolder<>(recipeId("smithing", resultItem, baseItem), this);
	}

	private static ItemStack getPreviewResult(Item resultItem, int additionCount) {
		ItemStack stack = resultItem.getDefaultInstance().copy();
		stack.setDamageValue(resultDamage(resultItem, additionCount, 1));
		return stack;
	}

	public static int resultDamage(Item resultItem, int additionCount, int usedCount) {
		if (resultItem.getDefaultInstance().getMaxDamage() == 0) return additionCount == usedCount ? 0 : 1;
		return Math.min(resultItem.getDefaultInstance().getMaxDamage() - 1, (int) Math.floor(resultItem.getDefaultInstance().getMaxDamage() * ((additionCount - usedCount) / 4.0)));
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput recipeInput, HolderLookup.Provider registries) {
		ItemStack output = super.assemble(recipeInput, registries);
		int usedCount = Math.min(additionCount, recipeInput.addition().getCount());
		int damage = resultDamage(output.getItem(), additionCount, usedCount);
		if (damage > output.getMaxDamage()) return ItemStack.EMPTY;
		output.setDamageValue(damage);
		return output;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return TinkerersSmithing.SMITHING_UPGRADE_SERIALIZER.get();
	}

	@Override
	public @Nullable RecipeSerializer<SmithingTransformRecipe> getFallbackSerializer() {
		return RecipeSerializer.SMITHING_TRANSFORM;
	}

	public static class Serializer implements RecipeSerializer<SmithingUpgradeRecipe> {
		MapCodec<SmithingUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("baseItem").forGetter(r -> r.baseItem),
			Ingredient.CODEC_NONEMPTY.fieldOf("addition").forGetter(r -> r.addition),
			Codec.INT.fieldOf("additionCount").forGetter(r -> r.additionCount),
			BuiltInRegistries.ITEM.byNameCodec().fieldOf("resultItem").forGetter(r -> r.resultItem)
		).apply(instance, SmithingUpgradeRecipe::new));

		StreamCodec<RegistryFriendlyByteBuf, SmithingUpgradeRecipe> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.registry(Registries.ITEM), r -> r.baseItem,
			Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition,
			ByteBufCodecs.VAR_INT, r -> r.additionCount,
			ByteBufCodecs.registry(Registries.ITEM), r -> r.resultItem,
			SmithingUpgradeRecipe::new
		);

		@Override
		public MapCodec<SmithingUpgradeRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, SmithingUpgradeRecipe> streamCodec() {
			return PACKET_CODEC;
		}
	}
}
