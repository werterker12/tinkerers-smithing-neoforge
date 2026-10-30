package folk.sisby.tinkerers_smithing.mixin;

import folk.sisby.tinkerers_smithing.packet.S2CPing;
import folk.sisby.tinkerers_smithing.recipe.ServerRecipe;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerCommonNetworkHandler.class)
public class MixinServerCommonNetworkHandler {
	@ModifyArg(method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerCommonNetworkHandler;send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V"), index = 0)
	private Packet<?> useServerRecipeFallbacks(Packet<?> packet) {
		ServerCommonNetworkHandler self = (ServerCommonNetworkHandler) (Object) this;
		if (self instanceof ServerPlayNetworkHandler handler && packet instanceof SynchronizeRecipesS2CPacket srp) {
			if (!ServerPlayNetworking.canSend(handler, S2CPing.ID)) {
				return new SynchronizeRecipesS2CPacket(ServerRecipe.applyFallbacks(srp.getRecipes()));
			}
		}
		return packet;
	}
}
