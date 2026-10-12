package com.mineblock.exposuredetective.network;

import java.util.concurrent.atomic.AtomicBoolean;

import com.mineblock.exposuredetective.ExposuredetectiveMod;

/**
 * Handles registration of all custom packets. Designed to survive MCreator regenerations by being invoked from the user code block.
 */
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
	}
}

