package folk.sisby.tinkerers_smithing.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.network.ServerPlayNetworkHandler$1")
public class MixinServerPlayNetworkHandlerEntityInteract {
	@Inject(
		method = "processInteract",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler$Interaction;run(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"
		),
		cancellable = true
	)
	private void ignoreBrokenKeepers(CallbackInfo ci, @Local(ordinal = 1) ItemStack stack) {
		if (TinkerersSmithing.isKeeper(stack) && TinkerersSmithing.isBroken(stack)) {
			ci.cancel();
		}
	}
}
