package folk.sisby.tinkerers_smithing.data;

import com.google.gson.Gson;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingLoader;
import folk.sisby.tinkerers_smithing.TinkerersSmithingMaterial;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class SmithingArmorMaterialLoader extends SmithingMaterialLoader {
	public static final SmithingArmorMaterialLoader INSTANCE = new SmithingArmorMaterialLoader(new Gson());
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TinkerersSmithing.ID, "smithing_armor_material_loader");

	public SmithingArmorMaterialLoader(Gson gson) {
		super(gson, "smithing_armor_materials", TinkerersSmithingMaterial.EQUIPMENT_TYPE.ARMOR);
	}

	@Override
	public Ingredient getDefaultRepairIngredient(Item item) {
		if (item.getDefaultInstance().isDamageableItem() && item instanceof ArmorItem ai) {
			ArmorMaterial material = ai.getMaterial().value();
			if (material != null) {
				Ingredient repairIngredient = material.repairIngredient().get();
				if (repairIngredient != null && !repairIngredient.isEmpty()) {
					return repairIngredient;
				}
			}
		}
		return null;
	}

	@Override
	public boolean matchingMaterials(Item item1, Item item2) {
		return item1 instanceof ArmorItem ai1 && item2 instanceof ArmorItem ai2 && ai1.getMaterial() == ai2.getMaterial();
	}

	@Override
	public Map<ResourceLocation, TinkerersSmithingMaterial> getOutputMap() {
		return TinkerersSmithingLoader.INSTANCE.ARMOR_MATERIALS;
	}

	@Override
	public String getName() {
		return ID.toString();
	}
}
