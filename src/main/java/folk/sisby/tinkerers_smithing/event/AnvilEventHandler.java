package folk.sisby.tinkerers_smithing.event;

import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.recipe.ShapelessRepairRecipe;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

@EventBusSubscriber(modid = TinkerersSmithing.ID)
public class AnvilEventHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        if (left.isEmpty() || right.isEmpty()) {
            return;
        }

        // 1. Deworking: Copper / Dework Ingredients reduce Prior Work Penalty
        if (right.is(TinkerersSmithing.DEWORK_INGREDIENTS) && left.getOrDefault(DataComponents.REPAIR_COST, 0) > 0) {
            ItemStack result = left.copy();
            int materialUsed = 0;
            do {
                result.set(DataComponents.REPAIR_COST, ((result.getOrDefault(DataComponents.REPAIR_COST, 0) + 1) / 2) - 1);
                materialUsed++;
            } while (result.getOrDefault(DataComponents.REPAIR_COST, 0) > 0 && materialUsed < right.getCount());

            applyRename(event, left, result);
            event.setOutput(result);
            event.setCost(0);
            event.setMaterialCost(materialUsed);
            return;
        }

        // 2. Unit Material Repair
        if (!left.isDamageableItem() || left.getDamageValue() <= 0) {
            return;
        }

        Player player = event.getPlayer();
        Level level = player != null ? player.level() : null;

        ShapelessRepairRecipe matchingRecipe = findRepairRecipe(level, left, right);
        boolean isValidMaterial = left.getItem().isValidRepairItem(left, right) || matchingRecipe != null;

        if (!isValidMaterial) {
            return;
        }

        int maxDamage = left.getMaxDamage();
        int repairAmountPerItem;
        if (matchingRecipe != null && matchingRecipe.additionCount > 0) {
            repairAmountPerItem = (int) Math.ceil((double) maxDamage / matchingRecipe.additionCount);
        } else {
            repairAmountPerItem = Math.max(1, maxDamage / 4);
        }

        int damage = left.getDamageValue();
        int materialUsed = 0;
        while (damage > 0 && materialUsed < right.getCount()) {
            damage -= repairAmountPerItem;
            materialUsed++;
        }

        ItemStack output = left.copy();
        output.setDamageValue(Math.max(0, damage));
        applyRename(event, left, output);

        event.setOutput(output);
        event.setCost(0);
        event.setMaterialCost(materialUsed);
    }

    private static ShapelessRepairRecipe findRepairRecipe(Level level, ItemStack left, ItemStack right) {
        if (level == null) return null;
        for (RecipeHolder<CraftingRecipe> recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (recipe.value() instanceof ShapelessRepairRecipe srr
                    && left.is(srr.baseItem)
                    && srr.addition.test(right)) {
                return srr;
            }
        }
        return null;
    }

    private static void applyRename(AnvilUpdateEvent event, ItemStack original, ItemStack output) {
        String name = event.getName();
        if (name != null && !StringUtil.isBlank(name)) {
            if (!name.equals(original.getHoverName().getString())) {
                output.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            }
        } else if (name != null && StringUtil.isBlank(name) && original.has(DataComponents.CUSTOM_NAME)) {
            output.remove(DataComponents.CUSTOM_NAME);
        }
    }
}

