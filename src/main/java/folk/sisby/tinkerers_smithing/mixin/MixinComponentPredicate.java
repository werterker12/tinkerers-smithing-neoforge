package folk.sisby.tinkerers_smithing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DataComponentPredicate.class)
public class MixinComponentPredicate {
	@WrapOperation(
		method = "test(Lnet/minecraft/core/component/DataComponentMap;)Z",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/core/component/DataComponentMap;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;")
	)
	private Object brokenFailEnchantmentPredicates(DataComponentMap instance, DataComponentType<?> componentType, Operation<Object> original) {
		if (componentType == DataComponents.ENCHANTMENTS && TinkerersSmithing.isBroken(instance)) {
			return ItemEnchantments.EMPTY;
		}
		return original.call(instance, componentType);
	}
}
