package com.mineblock.exposuredetective.client.ink;

import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import net.minecraft.client.gui.screens.Screen;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

final class PhotographScreenAccess {
	private static final VarHandle X;
	private static final VarHandle Y;
	private static final VarHandle ZOOM;

	static {
		try {
			var lookup = MethodHandles.privateLookupIn(PhotographScreen.class, MethodHandles.lookup());
			X = lookup.findVarHandle(PhotographScreen.class, "x", float.class);
			Y = lookup.findVarHandle(PhotographScreen.class, "y", float.class);
			ZOOM = lookup.findVarHandle(PhotographScreen.class, "zoom", io.github.mortuusars.exposure.client.gui.component.SteppedZoom.class);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Exposure PhotographScreen layout changed", e);
		}
	}

	private PhotographScreenAccess() {
	}

	static float offsetX(PhotographScreen screen) {
		return (float) X.get(screen);
	}

	static float offsetY(PhotographScreen screen) {
		return (float) Y.get(screen);
	}

	static double zoom(PhotographScreen screen) {
		var stepped = (io.github.mortuusars.exposure.client.gui.component.SteppedZoom) ZOOM.get(screen);
		return stepped != null ? stepped.get() : 1.0d;
	}

	static double displayScale(PhotographScreen screen) {
		return screen.height * 0.8d * zoom(screen);
	}

	static boolean isPhotographScreen(Screen screen) {
		return screen instanceof PhotographScreen;
	}
}
