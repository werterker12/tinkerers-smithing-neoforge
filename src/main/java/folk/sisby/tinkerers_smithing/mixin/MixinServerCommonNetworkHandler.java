package folk.sisby.tinkerers_smithing.mixin;

import folk.sisby.tinkerers_smithing.packet.S2CPing;
import folk.sisby.tinkerers_smithing.recipe.ServerRecipe;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerCommonPacketListenerImpl.class)
public class MixinServerCommonNetworkHandler {
    @ModifyArg(
        method = "send(Lnet/minecraft/network/protocol/Packet;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerCommonPacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"),
        index = 0
    )
    private Packet<?> useServerRecipeFallbacks(Packet<?> packet) {
        ServerCommonPacketListenerImpl self = (ServerCommonPacketListenerImpl) (Object) this;
        if (self instanceof ServerGamePacketListenerImpl handler && packet instanceof ClientboundUpdateRecipesPacket srp) {
            // Check if the client supports our custom mod payload by testing if S2CPing channel is known.
            // On vanilla/non-modded clients the channel won't be registered, so we fall back to vanilla serializers.
            boolean clientSupported = handler.hasChannel(S2CPing.TYPE);
            if (!clientSupported) {
                return new ClientboundUpdateRecipesPacket(ServerRecipe.applyFallbacks(srp.getRecipes()));
            }
        }
        return packet;
    }
}
