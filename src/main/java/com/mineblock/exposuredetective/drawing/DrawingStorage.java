package com.mineblock.exposuredetective.drawing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mineblock.exposuredetective.ExposuredetectiveMod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;


public final class DrawingStorage {
	private static final String DIR = "exposuredetective/drawings";
	private static final Map<UUID, DrawingRaster> MEMORY_CACHE = new ConcurrentHashMap<>();

	private DrawingStorage() {
	}

	public static Path drawingsDir(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve(DIR);
	}

	public static Path fileFor(MinecraftServer server, UUID id) {
		return drawingsDir(server).resolve(id.toString() + ".png");
	}

	public static Path legacyFileFor(MinecraftServer server, UUID id) {
		return drawingsDir(server).resolve(id.toString() + ".edrw");
	}

	public static void ensureDir(MinecraftServer server) throws IOException {
		Files.createDirectories(drawingsDir(server));
	}

	public static void save(MinecraftServer server, UUID id, DrawingRaster raster) throws IOException {
		ensureDir(server);
		byte[] data = DrawingCodec.encode(raster);
		Files.write(fileFor(server, id), data);
		
		Files.deleteIfExists(legacyFileFor(server, id));
		MEMORY_CACHE.put(id, DrawingRaster.copyOf(raster));
	}

	public static DrawingRaster load(MinecraftServer server, UUID id) {
		DrawingRaster cached = MEMORY_CACHE.get(id);
		if (cached != null) {
			return DrawingRaster.copyOf(cached);
		}
		Path png = fileFor(server, id);
		Path edrw = legacyFileFor(server, id);
		Path file = Files.isRegularFile(png) ? png : (Files.isRegularFile(edrw) ? edrw : null);
		if (file == null) {
			return null;
		}
		try {
			byte[] data = Files.readAllBytes(file);
			DrawingRaster raster = DrawingCodec.decode(data);
			MEMORY_CACHE.put(id, DrawingRaster.copyOf(raster));
			
			if (file.equals(edrw)) {
				try {
					save(server, id, raster);
				} catch (IOException ignored) {
				}
			}
			return raster;
		} catch (Exception e) {
			ExposuredetectiveMod.LOGGER.warn("Failed to load drawing {}", id, e);
			return null;
		}
	}

	
	public static void saveReplacing(MinecraftServer server, UUID id, UUID previousId, DrawingRaster raster, boolean deletePrevious) throws IOException {
		save(server, id, raster);
		if (deletePrevious && previousId != null && !previousId.equals(id)) {
			delete(server, previousId);
		}
	}

	public static void delete(MinecraftServer server, UUID id) {
		MEMORY_CACHE.remove(id);
		try {
			Files.deleteIfExists(fileFor(server, id));
			Files.deleteIfExists(legacyFileFor(server, id));
		} catch (IOException e) {
			ExposuredetectiveMod.LOGGER.warn("Failed to delete drawing {}", id, e);
		}
	}

	public static void clearCache() {
		MEMORY_CACHE.clear();
	}
}
