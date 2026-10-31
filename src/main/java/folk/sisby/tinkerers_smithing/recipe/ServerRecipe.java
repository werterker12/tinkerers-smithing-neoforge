package folk.sisby.tinkerers_smithing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface ServerRecipe<T extends Recipe<?>> {
	Codec<Recipe<?>> CODEC = BuiltInRegistries.RECIPE_SERIALIZER.byNameCodec().dispatch(r -> r instanceof ServerRecipe<?> sr && sr.getFallbackSerializer() != null ? sr.getFallbackSerializer() : r.getSerializer(), RecipeSerializer::codec);

	default @Nullable RecipeSerializer<T> getFallbackSerializer() {
		return null;
	}

	static List<RecipeHolder<?>> applyFallbacks(List<RecipeHolder<?>> recipes) {
		List<RecipeHolder<?>> safeRecipes = new ArrayList<>();
		for (RecipeHolder<?> recipe : recipes) {
			if (recipe.value() instanceof ServerRecipe<?> sr) {
				if (sr.getFallbackSerializer() != null) {
					safeRecipes.add(new RecipeHolder<>(recipe.id(), CODEC.decode(JsonOps.INSTANCE, CODEC.encodeStart(JsonOps.INSTANCE, recipe.value()).getOrThrow()).getOrThrow().getFirst()));
				}
			} else {
				safeRecipes.add(recipe);
			}
		}
		return safeRecipes;
	}
}
