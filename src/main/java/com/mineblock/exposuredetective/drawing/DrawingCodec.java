package com.mineblock.exposuredetective.drawing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


public final class DrawingCodec {
	private static final byte[] PNG_SIG = new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10};
	private static final int EDRW_MAGIC = 0x45445257;
	private static final int EDRW_VERSION = 1;

	private DrawingCodec() {
	}

	public static byte[] encode(DrawingRaster raster) throws IOException {
		int size = DrawingRaster.SIZE;
		
		byte[] raw = new byte[size * (1 + size * 4)];
		int ri = 0;
		for (int y = 0; y < size; y++) {
			raw[ri++] = 0; 
			for (int x = 0; x < size; x++) {
				int color = raster.getColor(x, y);
				if (color == 0) {
					raw[ri++] = 0;
					raw[ri++] = 0;
					raw[ri++] = 0;
					raw[ri++] = 0;
				} else {
					int a = raster.isAlwaysVisible(x, y) ? 255 : 128;
					raw[ri++] = (byte) ((color >> 16) & 0xFF); 
					raw[ri++] = (byte) ((color >> 8) & 0xFF);  
					raw[ri++] = (byte) (color & 0xFF);         
					raw[ri++] = (byte) a;
				}
			}
		}

		byte[] compressed = deflate(raw);
		ByteArrayOutputStream out = new ByteArrayOutputStream(compressed.length + 128);
		out.write(PNG_SIG);
		writeChunk(out, "IHDR", ihdr(size, size));
		writeChunk(out, "IDAT", compressed);
		writeChunk(out, "IEND", new byte[0]);
		return out.toByteArray();
	}

	public static DrawingRaster decode(byte[] data) throws IOException {
		if (data == null || data.length < 8) {
			throw new IOException("Empty drawing data");
		}
		
		if (startsWithPng(data)) {
			return decodePng(data);
		}
		return decodeLegacyEdrw(data);
	}

	private static boolean startsWithPng(byte[] data) {
		if (data.length < 8) return false;
		for (int i = 0; i < 8; i++) {
			if (data[i] != PNG_SIG[i]) return false;
		}
		return true;
	}

	private static DrawingRaster decodePng(byte[] data) throws IOException {
		int pos = 8;
		int width = -1, height = -1;
		ByteArrayOutputStream idat = new ByteArrayOutputStream();
		while (pos + 8 <= data.length) {
			int len = readInt(data, pos);
			pos += 4;
			String type = new String(data, pos, 4);
			pos += 4;
			if (pos + len + 4 > data.length) {
				throw new IOException("Truncated PNG chunk " + type);
			}
			byte[] chunk = Arrays.copyOfRange(data, pos, pos + len);
			pos += len;
			pos += 4; 
			if ("IHDR".equals(type)) {
				width = readInt(chunk, 0);
				height = readInt(chunk, 4);
				int bitDepth = chunk[8] & 0xFF;
				int colorType = chunk[9] & 0xFF;
				if (bitDepth != 8 || colorType != 6) {
					throw new IOException("Only 8-bit RGBA PNG supported");
				}
			} else if ("IDAT".equals(type)) {
				idat.write(chunk);
			} else if ("IEND".equals(type)) {
				break;
			}
		}
		if (width != DrawingRaster.SIZE || height != DrawingRaster.SIZE) {
			throw new IOException("Unexpected PNG size " + width + "x" + height);
		}
		byte[] raw = inflate(idat.toByteArray());
		int[] colors = new int[DrawingRaster.PIXEL_COUNT];
		boolean[] vis = new boolean[DrawingRaster.PIXEL_COUNT];
		int expected = height * (1 + width * 4);
		if (raw.length < expected) {
			throw new IOException("Inflated PNG too small");
		}
		int ri = 0;
		for (int y = 0; y < height; y++) {
			ri++; 
			for (int x = 0; x < width; x++) {
				int r = raw[ri++] & 0xFF;
				int g = raw[ri++] & 0xFF;
				int b = raw[ri++] & 0xFF;
				int a = raw[ri++] & 0xFF;
				int idx = y * width + x;
				if (a == 0) {
					colors[idx] = 0;
					vis[idx] = false;
				} else {
					colors[idx] = 0xFF000000 | (r << 16) | (g << 8) | b;
					vis[idx] = a >= 192;
				}
			}
		}
		return new DrawingRaster(colors, vis);
	}

	private static DrawingRaster decodeLegacyEdrw(byte[] data) throws IOException {
		try {
			byte[] inflated = inflate(data);
			ByteBuffer buf = ByteBuffer.wrap(inflated).order(ByteOrder.BIG_ENDIAN);
			int magic = buf.getInt();
			if (magic != EDRW_MAGIC) {
				
				throw new IOException("Not EDRW");
			}
			int version = buf.get() & 0xFF;
			if (version != EDRW_VERSION) {
				throw new IOException("Unsupported EDRW version " + version);
			}
			int size = buf.get() & 0xFF;
			if (size != DrawingRaster.SIZE) {
				throw new IOException("Bad EDRW size");
			}
			int[] colors = new int[DrawingRaster.PIXEL_COUNT];
			for (int i = 0; i < colors.length; i++) {
				colors[i] = buf.getInt();
			}
			byte[] bits = new byte[(DrawingRaster.PIXEL_COUNT + 7) / 8];
			buf.get(bits);
			return new DrawingRaster(colors, DrawingRaster.unpackVisibility(bits));
		} catch (Exception e) {
			
			return decodeLegacyGzipEdrw(data);
		}
	}

	private static DrawingRaster decodeLegacyGzipEdrw(byte[] data) throws IOException {
		try (java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(data));
			 java.io.DataInputStream in = new java.io.DataInputStream(gzip)) {
			int magic = in.readInt();
			if (magic != EDRW_MAGIC) {
				throw new IOException("Bad drawing magic");
			}
			int version = in.readUnsignedByte();
			if (version != EDRW_VERSION) {
				throw new IOException("Unsupported drawing version: " + version);
			}
			int size = in.readUnsignedByte();
			if (size != DrawingRaster.SIZE) {
				throw new IOException("Unexpected drawing size: " + size);
			}
			int[] colors = new int[DrawingRaster.PIXEL_COUNT];
			for (int i = 0; i < colors.length; i++) {
				colors[i] = in.readInt();
			}
			byte[] bits = in.readNBytes((DrawingRaster.PIXEL_COUNT + 7) / 8);
			return new DrawingRaster(colors, DrawingRaster.unpackVisibility(bits));
		}
	}

	private static byte[] ihdr(int w, int h) {
		ByteBuffer buf = ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN);
		buf.putInt(w);
		buf.putInt(h);
		buf.put((byte) 8); 
		buf.put((byte) 6); 
		buf.put((byte) 0); 
		buf.put((byte) 0); 
		buf.put((byte) 0); 
		return buf.array();
	}

	private static void writeChunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
		writeInt(out, data.length);
		byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		out.write(typeBytes);
		out.write(data);
		CRC32 crc = new CRC32();
		crc.update(typeBytes);
		crc.update(data);
		writeInt(out, (int) crc.getValue());
	}

	private static void writeInt(ByteArrayOutputStream out, int v) {
		out.write((v >>> 24) & 0xFF);
		out.write((v >>> 16) & 0xFF);
		out.write((v >>> 8) & 0xFF);
		out.write(v & 0xFF);
	}

	private static int readInt(byte[] data, int off) {
		return ((data[off] & 0xFF) << 24)
				| ((data[off + 1] & 0xFF) << 16)
				| ((data[off + 2] & 0xFF) << 8)
				| (data[off + 3] & 0xFF);
	}

	private static byte[] deflate(byte[] raw) {
		Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
		def.setInput(raw);
		def.finish();
		byte[] buf = new byte[raw.length + 64];
		ByteArrayOutputStream bos = new ByteArrayOutputStream(raw.length);
		while (!def.finished()) {
			int n = def.deflate(buf);
			bos.write(buf, 0, n);
		}
		def.end();
		return bos.toByteArray();
	}

	private static byte[] inflate(byte[] compressed) throws IOException {
		Inflater inf = new Inflater();
		inf.setInput(compressed);
		byte[] buf = new byte[8192];
		ByteArrayOutputStream bos = new ByteArrayOutputStream(compressed.length * 2);
		try {
			while (!inf.finished()) {
				int n = inf.inflate(buf);
				if (n == 0 && inf.needsInput()) {
					break;
				}
				bos.write(buf, 0, n);
			}
		} catch (DataFormatException e) {
			throw new IOException("PNG inflate failed", e);
		} finally {
			inf.end();
		}
		return bos.toByteArray();
	}
}
