package folk.sisby.tinkerers_smithing.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SmithingTypeLoader extends MultiJsonDataLoader {
	public static final SmithingTypeLoader INSTANCE = new SmithingTypeLoader(new Gson());
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TinkerersSmithing.ID, "smithing_type_loader");
	public static final TagLoader<Item> ITEM_TAG_LOADER = new TagLoader<>(BuiltInRegistries.ITEM::getOptional, "tags/item");
	public static final String AVOIDANCE_PREFIX = "tinkerers_smithing_types/";

	public SmithingTypeLoader(Gson gson) {
		super(gson, "smithing_types");
	}

	@Override
	public String getName() {
		return ID.toString();
	}

	@Override
	protected void apply(Map<ResourceLocation, Collection<Pair<JsonElement, String>>> prepared, ResourceManager manager, ProfilerFiller profiler) {
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Loading Types!");
		TinkerersSmithingLoader.INSTANCE.SMITHING_TYPES.clear();
		Map<ResourceLocation, List<TagLoader.EntryWithSource>> typeTags = new HashMap<>();
		prepared.forEach((id, jsons) -> {
			ResourceLocation collisionAvoidingID = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), AVOIDANCE_PREFIX + id.getPath());
			jsons.forEach(jsonEntry -> {
				List<TagLoader.EntryWithSource> list = typeTags.computeIfAbsent(collisionAvoidingID, k -> new ArrayList<>());
				TagFile tagFile = TagFile.CODEC.decode(JsonOps.INSTANCE, jsonEntry.getLeft()).getOrThrow().getFirst();
				if (tagFile.replace()) {
					list.clear();
				}
				tagFile.entries().forEach(entry -> list.add(new TagLoader.EntryWithSource(entry, jsonEntry.getRight())));
			});
		});

		Map<ResourceLocation, List<TagLoader.EntryWithSource>> itemTags = ITEM_TAG_LOADER.load(manager);
		Map<ResourceLocation, List<TagLoader.EntryWithSource>> allTags = new HashMap<>();
		allTags.putAll(itemTags);
		allTags.putAll(typeTags);
		Map<ResourceLocation, Collection<Item>> tags = ITEM_TAG_LOADER.build(allTags);
		tags.entrySet().removeIf(e -> !typeTags.containsKey(e.getKey()));
		// Strip collision avoiding ID
		tags = tags.entrySet().stream().collect(Collectors.toMap(e -> ResourceLocation.fromNamespaceAndPath(e.getKey().getNamespace(), StringUtils.removeStart(e.getKey().getPath(), AVOIDANCE_PREFIX)), Map.Entry::getValue));
		TinkerersSmithingLoader.INSTANCE.SMITHING_TYPES.putAll(tags);
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Reloaded smithing types");
	}
}
