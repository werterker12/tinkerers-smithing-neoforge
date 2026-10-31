package folk.sisby.tinkerers_smithing.mixin;

import com.mojang.serialization.JsonOps;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;

@Mixin(Ingredient.class)
public class IngredientMixin {
	@Unique
	private boolean isFootgun() {
		Ingredient self = (Ingredient) (Object) this;
		if (Arrays.stream(self.values).anyMatch(e -> e instanceof Ingredient.TagValue) && (BuiltInRegistries.ITEM.getTag(ItemTags.PLANKS).isEmpty() || BuiltInRegistries.ITEM.getTag(ItemTags.PLANKS).get().size() == 0)) {
			TinkerersSmithing.LOGGER.error("[Tinkerer's Smithing] Cowardly refusing to access an unloaded tag ingredient: {}", Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, self), new IllegalStateException("A tag ingredient was accessed before tags are loaded - This can break recipes! Report this to the mod in the trace below."));
			return true;
		}
		return false;
	}

	@Inject(method = "getItems", at = @At("HEAD"), cancellable = true)
	public void getItems(CallbackInfoReturnable<ItemStack[]> cir) {
		if (isFootgun()) {
			cir.setReturnValue(new ItemStack[]{});
			cir.cancel();
		}
	}
}
