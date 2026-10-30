package folk.sisby.tinkerers_smithing.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public interface ServerRecipe<T extends Recipe<?>> {
	Codec<Recipe<?>> CODEC = Registries.RECIPE_SERIALIZER.getCodec().dispatch(r -> r instanceof ServerRecipe<?> sr && sr.getFallbackSerializer() != null ? sr.getFallbackSerializer() : r.getSerializer() , RecipeSerializer::codec);

	default @Nullable RecipeSerializer<T> getFallbackSerializer() {
		return null;
	}

	static List<RecipeEntry<?>> applyFallbacks(List<RecipeEntry<?>> recipes) {
		List<RecipeEntry<?>> safeRecipes = new ArrayList<>();
		for (RecipeEntry<?> recipe : recipes) {
			if (recipe.value() instanceof ServerRecipe<?> sr) {
				if (sr.getFallbackSerializer() != null) {
					safeRecipes.add(new RecipeEntry<>(recipe.id(), CODEC.decode(JsonOps.INSTANCE, CODEC.encodeStart(JsonOps.INSTANCE, recipe.value()).getOrThrow()).getOrThrow().getFirst()));
				}
			} else {
				safeRecipes.add(recipe);
			}
		}
		return safeRecipes;
	}
}
