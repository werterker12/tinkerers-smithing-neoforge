package folk.sisby.tinkerers_smithing.mixin;

import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {
	@Inject(method = "getItemEnchantmentLevel", at = @At("HEAD"), cancellable = true)
	private static void brokenNoEnchantments(Holder<Enchantment> enchantment, ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (!(enchantment.is(TinkerersSmithing.KEEPERS_IMMUNE)) && TinkerersSmithing.isBroken(stack)) {
			cir.setReturnValue(0); // Breaks XP from grinding broken enchanted gear, but hopefully nothing else.
			cir.cancel();
		}
	}
}
