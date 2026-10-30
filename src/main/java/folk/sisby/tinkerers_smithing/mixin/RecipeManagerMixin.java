package folk.sisby.tinkerers_smithing.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingLoader;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.HashMap;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {
	@Unique private ImmutableMap.Builder<Identifier, RecipeEntry<?>> builder = null;

	@ModifyVariable(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 0), index = 5)
	private ImmutableMap.Builder<Identifier, RecipeEntry<?>> AddRuntimeRecipes(ImmutableMap.Builder<Identifier, RecipeEntry<?>> builder) {
		this.builder = builder;
		return builder;
	}

	@ModifyReceiver(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMultimap$Builder;build()Lcom/google/common/collect/ImmutableMultimap;", remap = false))
	private ImmutableMultimap.Builder<RecipeType<?>, RecipeEntry<?>> AddRuntimeRecipes(ImmutableMultimap.Builder<RecipeType<?>, RecipeEntry<?>> recipes) {
		Map<Identifier, Recipe<?>> dataRecipes = new HashMap<>();
		for (RecipeEntry<?> entry : builder.build().values()) {
			dataRecipes.put(entry.id(), entry.value());
		}
		TinkerersSmithing.generateSmithingData(dataRecipes);
		int manualRecipes = 0;
		for (RecipeEntry<?> entry : TinkerersSmithingLoader.INSTANCE.RECIPES) {
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
