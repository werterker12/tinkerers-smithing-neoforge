package folk.sisby.tinkerers_smithing.mixin;

import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(SimpleReloadInstance.class)
public class SimpleResourceReloadMixin {
	@ModifyVariable(method = "create", at = @At("HEAD"), index = 1, argsOnly = true)
	private static List<PreparableReloadListener> addRecipeDataDependencies(List<PreparableReloadListener> original) {
		List<PreparableReloadListener> reloaders = new ArrayList<>(original);
		List<PreparableReloadListener> toAdd = new ArrayList<>(TinkerersSmithing.RECIPE_DEPENDENCY_RELOADERS);
		reloaders.removeAll(toAdd);
		for (int i = 0; i < reloaders.size(); i++) {
			if (reloaders.get(i) instanceof RecipeManager) {
				reloaders.addAll(i, toAdd);
				break;
			}
		}
		return reloaders;
	}
}
