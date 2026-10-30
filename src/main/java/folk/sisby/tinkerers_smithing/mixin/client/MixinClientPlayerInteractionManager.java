package folk.sisby.tinkerers_smithing.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {
	@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
	private void dontSwingBrokenItemsBlock(CallbackInfoReturnable<ActionResult> ci, @Local ClientPlayerEntity player, @Local Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (TinkerersSmithing.isKeeper(stack) && TinkerersSmithing.isBroken(stack)) {
			ci.setReturnValue(ActionResult.FAIL);
		}
	}
}
