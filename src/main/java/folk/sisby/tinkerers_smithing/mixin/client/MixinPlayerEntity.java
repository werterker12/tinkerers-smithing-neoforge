package folk.sisby.tinkerers_smithing.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class MixinPlayerEntity {
	@Inject(
		method = "interactOn",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/Entity;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
		),
		cancellable = true
	)
	private void dontSwingBrokenToolsEntity(CallbackInfoReturnable<InteractionResult> ci, @Local(ordinal = 1) ItemStack stack) {
		// this mixin is only responsible for making the client not swing their hand when trying to use a broken tool
		// (without it, they will swing the tool on some mobs like bogged or snow golem)
		if (TinkerersSmithing.isKeeper(stack) && TinkerersSmithing.isBroken(stack)) {
			ci.setReturnValue(InteractionResult.PASS);
		}
	}
}
