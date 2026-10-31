package folk.sisby.tinkerers_smithing.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.recipe.ShapelessRepairRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.DataSlot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin extends ItemCombinerMenu {
	@Shadow
	public int repairItemCountCost;
	@Final
	@Shadow
	private DataSlot cost;

	public AnvilScreenHandlerMixin(@Nullable MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess context) {
		super(type, syncId, playerInventory, context);
	}

	@ModifyExpressionValue(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;calculateIncreasedRepairCost(I)I"))
	private int noLevelsNoWork(int original) {
		return this.cost.get() == 0 ? (original - 1) / 2 : original;
	}

	@ModifyExpressionValue(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;isValidRepairItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
	private boolean overrideRepairMaterials(boolean original) {
		for (RecipeHolder<CraftingRecipe> recipe : this.player.level().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
			if (recipe.value() instanceof ShapelessRepairRecipe srr && this.getSlot(AnvilMenu.INPUT_SLOT).getItem().is(srr.baseItem) && srr.addition.test(this.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem())) {
				return true;
			}
		}
		return false;
	}

	@ModifyVariable(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V", ordinal = 0), ordinal = 0)
	private int unitRepairNoLevels(int original) {
		return original - 1;
	}

	@ModifyVariable(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V", ordinal = 1), ordinal = 0)
	private int combineRepairNoLevels(int original) {
		this.repairItemCountCost = -1;
		return original - 2;
	}

	@ModifyExpressionValue(method = "mayPickup", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;get()I", ordinal = 1))
	private int allowTakingFreeRepairs(int original) {
		return original == 0 && this.repairItemCountCost != 0 ? 1 : original;
	}

	@ModifyVariable(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V", ordinal = 5, shift = At.Shift.AFTER), ordinal = 0)
	private int allowFreeRepairs(int original) {
		if (original == 0 && this.repairItemCountCost != 0) {
			this.cost.set(0); // Remove RepairCost cost
			return 1;
		} else {
			return original;
		}
	}

	@Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z", ordinal = 0), cancellable = true)
	private void applyDeworkMaterial(CallbackInfo ci) {
		ItemStack base = this.getSlot(AnvilMenu.INPUT_SLOT).getItem();
		ItemStack ingredient = this.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
		if (ingredient.is(TinkerersSmithing.DEWORK_INGREDIENTS) && base.getOrDefault(DataComponents.REPAIR_COST, 0) > 0) {
			ItemStack result = base.copy();
			this.repairItemCountCost = 0;
			do {
				result.set(DataComponents.REPAIR_COST, ((result.getOrDefault(DataComponents.REPAIR_COST, 0) + 1) / 2) - 1);
				this.repairItemCountCost++;
			} while (result.getOrDefault(DataComponents.REPAIR_COST, 0) > 0 && this.repairItemCountCost < ingredient.getCount());
			this.resultSlots.setItem(0, result);
			this.cost.set(0);
			this.broadcastChanges();
			ci.cancel();
		}
	}

	@Unique
	private int getSRCost(ItemEnchantments base, ItemEnchantments ingredient) {
		return ingredient.entrySet().stream().map(entry -> {
			Enchantment enchantment = entry.getKey().value();
			int level = entry.getIntValue();
			int baseLevel = base.getLevel(entry.getKey());
			int resultLevel = baseLevel == level ? level + 1 : Math.max(level, baseLevel);
			int rarityCost = enchantment.getAnvilCost();
			return rarityCost * resultLevel;
		}).reduce(0, Integer::sum);
	}

	@Unique
	private boolean doSwapEnchantments(ItemEnchantments base, ItemEnchantments ingredient) {
		return !(this.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().is(Items.ENCHANTED_BOOK)) && getSRCost(base, ingredient) > getSRCost(ingredient, base);
	}

	@ModifyArg(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/ItemEnchantments$Mutable;<init>(Lnet/minecraft/world/item/enchantment/ItemEnchantments;)V", ordinal = 0))
	private ItemEnchantments orderlessCombineSwapBaseTable(ItemEnchantments base) {
		ItemEnchantments ingredient = EnchantmentHelper.getEnchantmentsForCrafting(this.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem());
		return doSwapEnchantments(base, ingredient) ? ingredient : base;
	}

	@ModifyExpressionValue(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentsForCrafting(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/enchantment/ItemEnchantments;", ordinal = 1))
	private ItemEnchantments orderlessCombineSwapIngredientTable(ItemEnchantments ingredient) {
		ItemEnchantments base = EnchantmentHelper.getEnchantmentsForCrafting(this.getSlot(AnvilMenu.INPUT_SLOT).getItem());
		return doSwapEnchantments(base, ingredient) ? base : ingredient;
	}
}
