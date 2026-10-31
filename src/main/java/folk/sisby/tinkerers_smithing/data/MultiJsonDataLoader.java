package folk.sisby.tinkerers_smithing.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class MultiJsonDataLoader extends SimplePreparableReloadListener<Map<ResourceLocation, Collection<Pair<JsonElement, String>>>> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String FILE_SUFFIX = ".json";
	private final Gson gson;
	private final String dataType;

	public MultiJsonDataLoader(Gson gson, String dataType) {
		this.gson = gson;
		this.dataType = dataType;
	}

	@Override
	protected Map<ResourceLocation, Collection<Pair<JsonElement, String>>> prepare(ResourceManager manager, ProfilerFiller profiler) {
		Map<ResourceLocation, Collection<Pair<JsonElement, String>>> outMap = Maps.newHashMap();

		for (Map.Entry<ResourceLocation, List<Resource>> entry : manager.listResourceStacks(this.dataType, id -> id.getPath().endsWith(".json")).entrySet()) {
			ResourceLocation fileId = entry.getKey();
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), fileId.getPath().substring(dataType.length() + 1, fileId.getPath().length() - FILE_SUFFIX.length()));

			for (Resource resource : entry.getValue()) {
				try {
					try (Reader reader = resource.openAsReader()) {
						JsonElement jsonContents = GsonHelper.fromJson(this.gson, reader, JsonElement.class);
						if (jsonContents.isJsonObject() && !ICondition.conditionsMatched(JsonOps.INSTANCE, jsonContents)) {
							continue;
						}
						outMap.computeIfAbsent(id, k -> new ArrayList<>()).add(Pair.of(jsonContents, resource.sourcePackId()));
					}
				} catch (IllegalArgumentException | IOException | JsonParseException e) {
					LOGGER.error("Couldn't parse data file {} from {}", id, fileId, e);
				}
			}
		}

		return outMap;
	}
}
