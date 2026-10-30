package folk.sisby.tinkerers_smithing.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import folk.sisby.tinkerers_smithing.data.MultiJsonDataLoader;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.resource.conditions.ResourceConditionsImpl;
import net.fabricmc.fabric.mixin.resource.conditions.SinglePreparationResourceReloaderMixin;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

@Mixin(MultiJsonDataLoader.class)
public class MixinMultiJsonDataLoader extends SinglePreparationResourceReloaderMixin {
	@Shadow(remap = false) @Final private String dataType;

	@Override
	@SuppressWarnings("unchecked")
	protected void fabric_applyResourceConditions(ResourceManager resourceManager, Profiler profiler, Object object, @Nullable RegistryWrapper.WrapperLookup registryLookup) {
		Iterator<Map.Entry<Identifier, Collection<Pair<JsonElement, String>>>> it = ((Map<Identifier, Collection<Pair<JsonElement, String>>>) object).entrySet().iterator();

		while (it.hasNext()) {
			Map.Entry<Identifier, Collection<Pair<JsonElement, String>>> entry = it.next();
			Iterator<Pair<JsonElement, String>> it2 = entry.getValue().iterator();
			while (it2.hasNext()) {
				JsonElement resourceData = it2.next().getLeft();
				if (resourceData.isJsonObject()) {
					JsonObject obj = resourceData.getAsJsonObject();
					if (obj.has(ResourceConditions.CONDITIONS_KEY) && !ResourceConditionsImpl.applyResourceConditions(obj, dataType, entry.getKey(), fabric_getRegistryLookup())) {
						it2.remove();
					}
				}
			}
			if (entry.getValue().isEmpty()) it.remove();
		}
	}
}
