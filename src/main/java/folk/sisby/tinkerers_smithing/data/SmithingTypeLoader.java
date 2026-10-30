package folk.sisby.tinkerers_smithing.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import folk.sisby.tinkerers_smithing.TinkerersSmithing;
import folk.sisby.tinkerers_smithing.TinkerersSmithingLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagFile;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.profiler.Profiler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SmithingTypeLoader extends MultiJsonDataLoader {
	public static final SmithingTypeLoader INSTANCE = new SmithingTypeLoader(new Gson());
	public static final Identifier ID = Identifier.of(TinkerersSmithing.ID, "smithing_type_loader");
	public static final TagGroupLoader<Item> ITEM_TAG_LOADER = new TagGroupLoader<>(Registries.ITEM::getOrEmpty, "tags/item");
	public static final String AVOIDANCE_PREFIX = "tinkerers_smithing_types/";

	public SmithingTypeLoader(Gson gson) {
		super(gson, "smithing_types");
	}

	@Override
	public String getName() {
		return ID.toString();
	}

	@Override
	protected void apply(Map<Identifier, Collection<Pair<JsonElement, String>>> prepared, ResourceManager manager, Profiler profiler) {
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Loading Types!");
		TinkerersSmithingLoader.INSTANCE.SMITHING_TYPES.clear();
		Map<Identifier, List<TagGroupLoader.TrackedEntry>> typeTags = new HashMap<>();
		prepared.forEach((id, jsons) -> {
			Identifier collisionAvoidingID = Identifier.of(id.getNamespace(), AVOIDANCE_PREFIX + id.getPath());
			jsons.forEach(jsonEntry -> {
				List<TagGroupLoader.TrackedEntry> list = typeTags.computeIfAbsent(collisionAvoidingID, k -> new ArrayList<>());
				TagFile tagFile = TagFile.CODEC.decode(JsonOps.INSTANCE, jsonEntry.getLeft()).getOrThrow().getFirst();
				if (tagFile.replace()) {
					list.clear();
				}
				tagFile.entries().forEach(entry -> list.add(new TagGroupLoader.TrackedEntry(entry, jsonEntry.getRight())));
			});
		});

		Map<Identifier, List<TagGroupLoader.TrackedEntry>> itemTags = ITEM_TAG_LOADER.loadTags(manager);
		Map<Identifier, List<TagGroupLoader.TrackedEntry>> allTags = new HashMap<>();
		allTags.putAll(itemTags);
		allTags.putAll(typeTags);
		Map<Identifier, Collection<Item>> tags = ITEM_TAG_LOADER.buildGroup(allTags);
		tags.entrySet().removeIf(e -> !typeTags.containsKey(e.getKey()));
		// Strip collision avoiding ID
		tags = tags.entrySet().stream().collect(Collectors.toMap(e -> Identifier.of(e.getKey().getNamespace(), StringUtils.removeStart(e.getKey().getPath(), AVOIDANCE_PREFIX)), Map.Entry::getValue));
		TinkerersSmithingLoader.INSTANCE.SMITHING_TYPES.putAll(tags);
		TinkerersSmithing.LOGGER.info("[Tinkerer's Smithing] Reloaded smithing types");
	}
}
