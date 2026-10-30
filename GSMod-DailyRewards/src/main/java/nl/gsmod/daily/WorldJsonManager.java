package nl.gsmod.daily;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.conditions.ConditionalOps;

/**
 * Loads JSON definitions per world from {@code <world>/gsmod/<folder>/}. The first time a world starts,
 * the files that came with the mod and its datapacks are copied there, so every world can be edited on
 * its own. Files keep their {@code neoforge:conditions}, so e.g. Mekanism missions only count while
 * Mekanism is installed. Reloaded on world start and with {@code /reload}.
 */
public abstract class WorldJsonManager<T> extends SimpleJsonResourceReloadListener<T> {
    private static @Nullable Path worldRoot;

    private final String folder;
    private final Codec<Optional<T>> conditionalCodec;
    private Map<Identifier, T> fromDatapacks = Map.of();
    private Map<Identifier, String> defaultFiles = Map.of();
    private @Nullable DynamicOps<JsonElement> ops;

    protected WorldJsonManager(Codec<T> codec, String folder) {
        super(codec, FileToIdConverter.json(folder));
        this.folder = folder;
        this.conditionalCodec = ConditionalOps.createConditionalCodec(codec);
    }

    /** Called with the definitions that are in effect now. */
    protected abstract void accept(Map<Identifier, T> definitions, String source);

    /** Set while a world is running; {@code null} otherwise. */
    public static void setWorld(@Nullable Path root) {
        worldRoot = root;
    }

    public static @Nullable Path worldFolder(String folder) {
        return worldRoot == null ? null : worldRoot.resolve(GSModDaily.MODID).resolve(folder);
    }

    @Override
    protected void apply(Map<Identifier, T> loaded, ResourceManager manager, ProfilerFiller profiler) {
        fromDatapacks = Map.copyOf(loaded);
        ops = makeConditionalOps();
        Map<Identifier, String> raw = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : FileToIdConverter.json(folder).listMatchingResources(manager).entrySet()) {
            try {
                raw.put(FileToIdConverter.json(folder).fileToId(entry.getKey()), read(entry.getValue()));
            } catch (IOException e) {
                GSModDaily.LOGGER.warn("Could not read {}", entry.getKey(), e);
            }
        }
        defaultFiles = raw;
        refresh();
    }

    private static String read(Resource resource) throws IOException {
        try (Reader reader = resource.openAsReader()) {
            StringBuilder out = new StringBuilder();
            char[] buffer = new char[4096];
            for (int n; (n = reader.read(buffer)) > 0; ) {
                out.append(buffer, 0, n);
            }
            return out.toString();
        }
    }

    /** Re-reads the world folder (creating it with the defaults if missing), or falls back to datapack data. */
    public void refresh() {
        Path dir = worldFolder(folder);
        if (dir == null || ops == null) {
            accept(fromDatapacks, "datapacks");
            return;
        }
        try {
            if (!Files.isDirectory(dir)) {
                writeDefaults(dir);
            }
            accept(readFolder(dir), dir.toString());
        } catch (IOException e) {
            GSModDaily.LOGGER.error("Could not use {}, falling back to the built-in files", dir, e);
            accept(fromDatapacks, "datapacks");
        }
    }

    private void writeDefaults(Path dir) throws IOException {
        Files.createDirectories(dir);
        for (Map.Entry<Identifier, String> entry : defaultFiles.entrySet()) {
            Identifier id = entry.getKey();
            String relative = id.getNamespace().equals(GSModDaily.MODID) ? id.getPath() : id.getNamespace() + "/" + id.getPath();
            Path file = dir.resolve(relative + ".json");
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
        }
        GSModDaily.LOGGER.info("Created {} with {} default files", dir, defaultFiles.size());
    }

    /**
     * Reads definitions from a folder outside the world, such as {@code config/gsmod_daily/custom_rewards/}.
     * Empty if the folder is missing or nothing has been loaded yet.
     */
    protected Map<Identifier, T> readExtraFolder(Path dir) {
        if (ops == null || !Files.isDirectory(dir)) {
            return Map.of();
        }
        try {
            return readFolder(dir);
        } catch (IOException e) {
            GSModDaily.LOGGER.error("Could not read {}", dir, e);
            return Map.of();
        }
    }

    private Map<Identifier, T> readFolder(Path dir) throws IOException {
        Map<Identifier, T> result = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                String relative = dir.relativize(file).toString().replace('\\', '/');
                String path = relative.substring(0, relative.length() - 5).toLowerCase(Locale.ROOT);
                Identifier id = Identifier.tryBuild(GSModDaily.MODID, path);
                if (id == null) {
                    GSModDaily.LOGGER.warn("Skipping {}: file names may only use a-z, 0-9, _, - and /", file);
                    continue;
                }
                try {
                    JsonElement json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                    conditionalCodec.parse(ops, json)
                            .resultOrPartial(error -> GSModDaily.LOGGER.warn("Skipping {}: {}", file, error))
                            .flatMap(o -> o)
                            .ifPresent(value -> result.put(id, value));
                } catch (RuntimeException e) {
                    GSModDaily.LOGGER.warn("Skipping {}: {}", file, e.getMessage());
                }
            }
        }
        return result;
    }
}
