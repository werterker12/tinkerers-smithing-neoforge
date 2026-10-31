package folk.sisby.tinkerers_smithing.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.HashMap;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
	@Unique private ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder = null;

	@ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 0), index = 5)
	private ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> AddRuntimeRecipes(ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder) {
		this.builder = builder;
		return builder;
	}

	@ModifyReceiver(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMultimap$Builder;build()Lcom/google/common/collect/ImmutableMultimap;", remap = false))
	private ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> AddRuntimeRecipes(ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> recipes) {
		Map<ResourceLocation, Recipe<?>> dataRecipes = new HashMap<>();
		for (RecipeHolder<?> entry : builder.build().values()) {
			dataRecipes.put(entry.id(), entry.value());
		}
		TinkerersSmithing.generateSmithingData(dataRecipes);
		int manualRecipes = 0;
		for (RecipeHolder<?> entry : TinkerersSmithingLoader.INSTANCE.RECIPES) {
			if (!dataRecipes.containsKey(entry.id())) {
				recipes.put(entry.value().getType(), entry);
				builder.put(entry.id(), entry);
			} else {
				manualRecipes++;
			}
		}
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Added {} runtime recipes with {} data overrides!", TinkerersSmithingLoader.INSTANCE.RECIPES.size(), manualRecipes);
		return recipes;
	}
}
