package folk.sisby.tinkerers_smithing.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Shadow
	public abstract Item getItem();

	@Shadow
	public abstract int getDamageValue();

	@Unique
	private boolean isBroken() {
		return TinkerersSmithing.isBroken((ItemStack) (Object) this);
	}

	@Unique
	private boolean isKeeper() {
		return TinkerersSmithing.isKeeper((ItemStack) (Object) this);
	}

	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void brokenNoMiningSpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
		if (isBroken()) {
			cir.setReturnValue(1.0F);
			cir.cancel();
		}
	}

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void brokenDontUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		if (isBroken()) {
			cir.setReturnValue(InteractionResultHolder.fail((ItemStack) (Object) this));
			cir.cancel();
		}
	}

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void brokenDontUseOnBlock(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (isBroken()) {
			cir.setReturnValue(InteractionResult.FAIL);
			cir.cancel();
		}
	}

	@Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
	private void brokenDontUseOnEntity(Player user, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		if (isBroken()) {
			cir.setReturnValue(InteractionResult.FAIL);
			cir.cancel();
		}
	}

	@Inject(method = "isCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void brokenIsNotSuitable(BlockState state, CallbackInfoReturnable<Boolean> cir) {
		if (isBroken()) {
			cir.setReturnValue(false);
			cir.cancel();
		}
	}

	@Inject(method = {
		"forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
		"forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"
	}, at = @At("HEAD"), cancellable = true)
	private void brokenHasNoAttributes(CallbackInfo ci) {
		if (isBroken()) {
			ci.cancel();
		}
	}

	@ModifyExpressionValue(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isDamageableItem()Z"))
	private boolean brokenNoDamage(boolean original) {
		return original && !(isKeeper() && isBroken());
	}

	@ModifyExpressionValue(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxDamage()I"))
	private int dontBreakDecrementKeepers(int original, int amount, ServerLevel world, @Nullable LivingEntity player, Consumer<Item> breakCallback) {
		if (getDamageValue() >= original && isKeeper()) {
			breakCallback.accept(getItem());
			return Integer.MAX_VALUE;
		} else {
			return original;
		}
	}

	@Inject(method = "getTooltipLines", at = @At(value = "RETURN"), cancellable = true)
	public void brokenShowTooltip(Item.TooltipContext context, @Nullable Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> cir) {
		if (isBroken()) {
			List<Component> list = new ArrayList<>(cir.getReturnValue());
			list.add(Component.translatable("item.tinkerers_smithing.broken").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
			cir.setReturnValue(list);
		}
	}
}
