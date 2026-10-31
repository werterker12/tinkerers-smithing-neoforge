package folk.sisby.tinkerers_smithing.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {
	public AnvilScreenMixin(AnvilMenu menu, Inventory playerInventory, Component title, ResourceLocation texture) {
		super(menu, playerInventory, title, texture);
	}

	@Inject(method = "renderLabels", at = @At("TAIL"))
	private void drawRepairCost(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
		ItemStack base = this.menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
		ItemStack ingredient = this.menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		ItemStack result = this.menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
		if (!base.isEmpty() && !result.isEmpty() && this.menu.getCost() == 0) {
			Component text = Component.translatable("container.repair.work", result.getOrDefault(DataComponents.REPAIR_COST, 0) - Math.max(base.getOrDefault(DataComponents.REPAIR_COST, 0), ingredient.getOrDefault(DataComponents.REPAIR_COST, 0)));
			int k = this.imageWidth - 8 - this.font.width(text) - 2;
			graphics.fill(k - 2, 67, this.imageWidth - 8, 79, 1325400064);
			graphics.drawString(this.font, text, k, 69, 8453920);
		}
	}
}
