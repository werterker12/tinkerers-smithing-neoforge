package folk.sisby.tinkerers_smithing.mixin;

import folk.sisby.tinkerers_smithing.recipe.SmithingUpgradeRecipe;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingMenu.class)
public abstract class SmithingScreenHandlerMixin extends ItemCombinerMenu {
	@Shadow
	@Nullable
	private RecipeHolder<SmithingRecipe> selectedRecipe;
	@Unique
	private int ingredientsUsed = 0;

	public SmithingScreenHandlerMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess context) {
		super(type, syncId, playerInventory, context);
	}

	@Inject(method = "createResult", at = @At("TAIL"))
	public void cacheIngredientsUsed(CallbackInfo ci) {
		int additionCount = -1;
		if (this.selectedRecipe != null && this.selectedRecipe.value() instanceof SmithingUpgradeRecipe sur) {
			additionCount = sur.additionCount;
		}
		ingredientsUsed = additionCount == -1 ? 1 : Math.min(additionCount, this.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().getCount());
	}

	@Inject(method = "onTake", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/SmithingMenu;shrinkStackInSlot(I)V", ordinal = 0))
	public void specialDecrementIngredient(Player player, ItemStack stack, CallbackInfo ci) {
		this.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().shrink(ingredientsUsed - 1);
	}
}
