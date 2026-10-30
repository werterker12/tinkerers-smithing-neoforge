package folk.sisby.tinkerers_smithing.packet;

import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record S2CPing() implements CustomPayload {
	public static final Id<S2CPing> ID = new Id<>(Identifier.of(TinkerersSmithing.ID, "ping"));
	public static final PacketCodec<RegistryByteBuf, S2CPing> CODEC = PacketCodec.unit(new S2CPing());

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
