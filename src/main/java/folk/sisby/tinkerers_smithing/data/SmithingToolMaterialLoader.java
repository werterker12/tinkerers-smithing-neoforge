package folk.sisby.tinkerers_smithing.data;

import com.google.gson.Gson;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingLoader;
import folk.sisby.tinkerers_smithing.TinkerersSmithingMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class SmithingToolMaterialLoader extends SmithingMaterialLoader {
	public static final SmithingToolMaterialLoader INSTANCE = new SmithingToolMaterialLoader(new Gson());
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TinkerersSmithing.ID, "smithing_tool_material_loader");

	public SmithingToolMaterialLoader(Gson gson) {
		super(gson, "smithing_tool_materials", TinkerersSmithingMaterial.EQUIPMENT_TYPE.TOOL);
	}

	@Override
	public Ingredient getDefaultRepairIngredient(Item item) {
		if (item.getDefaultInstance().isDamageableItem() && item instanceof TieredItem ti) {
			Tier material = ti.getTier();
			if (material != null) {
				Ingredient repairIngredient = material.getRepairIngredient();
				if (repairIngredient != null && !repairIngredient.isEmpty()) {
					return repairIngredient;
				}
			}
		}
		return null;
	}

	@Override
	public boolean matchingMaterials(Item item1, Item item2) {
		return item1 instanceof TieredItem ti1 && item2 instanceof TieredItem ti2 && ti1.getTier() == ti2.getTier();
	}

	@Override
	public Map<ResourceLocation, TinkerersSmithingMaterial> getOutputMap() {
		return TinkerersSmithingLoader.INSTANCE.TOOL_MATERIALS;
	}

	@Override
	public String getName() {
		return ID.toString();
	}
}
