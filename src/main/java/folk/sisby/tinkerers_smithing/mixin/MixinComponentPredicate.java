package folk.sisby.tinkerers_smithing.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.predicate.ComponentPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ComponentPredicate.class)
public class MixinComponentPredicate {
	@WrapOperation(method = "test(Lnet/minecraft/component/ComponentMap;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/component/ComponentMap;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;"))
	private Object brokenFailEnchantmentPredicates(ComponentMap instance, ComponentType<?> componentType, Operation<Object> original) {
		if (componentType == DataComponentTypes.ENCHANTMENTS && TinkerersSmithing.isBroken(instance)) {
			return ItemEnchantmentsComponent.DEFAULT;
		}
		return original.call(instance, componentType);
	}
}
