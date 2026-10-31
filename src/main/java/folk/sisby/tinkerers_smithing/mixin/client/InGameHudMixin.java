package folk.sisby.tinkerers_smithing.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public class InGameHudMixin {
	@Shadow
	private ItemStack lastToolHighlight;

	@ModifyExpressionValue(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;empty()Lnet/minecraft/network/chat/MutableComponent;", ordinal = 0))
	private MutableComponent showBrokenHeldItemTooltip(MutableComponent text) {
		if (TinkerersSmithing.isBroken(lastToolHighlight)) {
			return Component.empty().append(Component.translatable("item.tinkerers_smithing.broken").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(false))).append(Component.literal(" ")).append(text);
		}
		return text;
	}
}
