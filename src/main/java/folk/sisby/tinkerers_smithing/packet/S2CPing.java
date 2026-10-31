package folk.sisby.tinkerers_smithing.packet;

import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CPing() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<S2CPing> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TinkerersSmithing.ID, "ping"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2CPing> STREAM_CODEC =
        StreamCodec.unit(new S2CPing());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
