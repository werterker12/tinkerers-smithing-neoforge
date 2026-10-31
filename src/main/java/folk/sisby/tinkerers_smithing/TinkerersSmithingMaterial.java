package folk.sisby.tinkerers_smithing;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public final class TinkerersSmithingMaterial {
	public final EQUIPMENT_TYPE type;
	public final Set<ResourceLocation> upgradesTo;
	public final List<Ingredient> repairMaterials;
	public final Set<Item> items;
	public ResourceLocation sacrificesVia;

	public TinkerersSmithingMaterial(EQUIPMENT_TYPE type, Set<ResourceLocation> upgradesTo, List<Ingredient> repairMaterials, Set<Item> items, ResourceLocation sacrificesVia) {
		this.type = type;
		this.upgradesTo = upgradesTo;
		this.repairMaterials = repairMaterials;
		this.items = items;
		this.sacrificesVia = sacrificesVia;
	}

	public enum EQUIPMENT_TYPE {
		TOOL,
		ARMOR
	}
}
