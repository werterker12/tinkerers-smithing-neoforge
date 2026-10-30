package folk.sisby.tinkerers_smithing.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
	@Inject(
		method = "interact",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/Entity;interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
		),
		cancellable = true
	)
	private void dontSwingBrokenToolsEntity(CallbackInfoReturnable<ActionResult> ci, @Local(ordinal = 1) ItemStack stack) {
		// this mixin is only responsible for making the client not swing their hand when trying to use a broken tool
		// (without it, they will swing the tool on some mobs like bogged or snow golem)
		if (TinkerersSmithing.isKeeper(stack) && TinkerersSmithing.isBroken(stack)) {
			ci.setReturnValue(ActionResult.PASS);
		}
	}

	@Inject(
		method = "interact",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/entity/Entity;interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
		),
		cancellable = true
	)
	private void dontSwingBrokenToolsBlock(CallbackInfoReturnable<ActionResult> ci, @Local(ordinal = 1) ItemStack stack) {
		// this mixin is only responsible for making the client not swing their hand when trying to use a broken tool
		// (without it, they will swing the tool on some mobs like bogged or snow golem)
		if (TinkerersSmithing.isKeeper(stack) && TinkerersSmithing.isBroken(stack)) {
			ci.setReturnValue(ActionResult.PASS);
		}
	}
}
