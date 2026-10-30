package folk.sisby.tinkerers_smithing.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Shadow
	private ItemStack currentStack;

	@ModifyExpressionValue(method = "renderHeldItemTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Text;empty()Lnet/minecraft/text/MutableText;", ordinal = 0))
	private MutableText showBrokenHeldItemTooltip(MutableText text) {
		if (TinkerersSmithing.isBroken(currentStack)) {
			return Text.empty().append(Text.translatable("item.tinkerers_smithing.broken").setStyle(Style.EMPTY.withColor(Formatting.DARK_RED).withItalic(false))).append(Text.literal(" ")).append(text);
		}
		return text;
	}
}
