package com.mineblock.exposuredetective.network;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mineblock.exposuredetective.ExposuredetectiveMod;


public final class ModNetworking {
	private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);

	private ModNetworking() {
	}

	public static void bootstrap() {
		if (BOOTSTRAPPED.getAndSet(true)) {
			return;
		}
		
		ExposuredetectiveMod.addNetworkMessage(InvisibleInkDrawPayload.TYPE, InvisibleInkDrawPayload.STREAM_CODEC, InvisibleInkDrawPayload::handle);
		ExposuredetectiveMod.addNetworkMessage(InvisibleInkDrawBatchPayload.TYPE, InvisibleInkDrawBatchPayload.STREAM_CODEC, InvisibleInkDrawBatchPayload::handle);
		ExposuredetectiveMod.addNetworkMessage(DrawingSavePayload.TYPE, DrawingSavePayload.STREAM_CODEC, DrawingSavePayload::handle);
		ExposuredetectiveMod.addNetworkMessage(DrawingRequestPayload.TYPE, DrawingRequestPayload.STREAM_CODEC, DrawingRequestPayload::handle);
		ExposuredetectiveMod.addNetworkMessage(DrawingSyncPayload.TYPE, DrawingSyncPayload.STREAM_CODEC, DrawingSyncPayload::handle);
	}
}
