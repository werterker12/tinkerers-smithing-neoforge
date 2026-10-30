package folk.sisby.tinkerers_smithing.client;

import folk.sisby.tinkerers_smithing.packet.S2CPing;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TinkerersSmithingClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("tinkerers_smithing_client");

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(S2CPing.ID, (packet, context) -> {});
	}
}
