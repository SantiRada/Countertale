package com.thescar.hygunsplugin.runtime.logic;

import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RuntimeHeatLogic {
	public static final int ACTIVE_FIRE_WINDOW_MS = 300;

	private RuntimeHeatLogic() {
	}

	public static int secondsToMillis(@Nullable Double value, @Nullable Double fallback) {
		double safe = value != null
		              ? value.doubleValue()
		              : (fallback != null
		                 ? fallback.doubleValue()
		                 : 0.0D);
		if (!Double.isFinite(safe) || safe <= 0.0D) {
			return 0;
		}

		return (int) Math.max(1L, Math.round(safe * 1000.0D));
	}

	public static int wrappedNowMillis() {
		return (int) (System.currentTimeMillis() & Integer.MAX_VALUE);
	}

	public static int wrappedDiffMillis(int older, int newer) {
		if (newer >= older) {
			return newer - older;
		}

		return (Integer.MAX_VALUE - older) + newer + 1;
	}

	public static float heatToSeconds(float heat, int overheatTimeMs) {
		if (overheatTimeMs <= 0 || heat <= 0.0F) {
			return 0.0F;
		}

		return clampHeat(heat) * (float) overheatTimeMs / 1000.0F;
	}

	public static boolean isActivelyFiring(@Nonnull HeatDataComponent state, int nowMs) {
		if (state.lastUseMs() <= 0) {
			return false;
		}

		return wrappedDiffMillis(state.lastUseMs(), nowMs) <= ACTIVE_FIRE_WINDOW_MS;
	}

	public static float currentHeat(@Nonnull HeatDataComponent state, int nowMs) {
		return clampHeat(state.heat());
	}

	public static float currentHeatProgress(@Nullable HeatDataComponent state, int nowMs) {
		if (state == null) {
			return 0.0F;
		}

		return currentHeat(state, nowMs);
	}

	public static float currentHeatSeconds(@Nullable HeatDataComponent state, int nowMs) {
		if (state == null) {
			return 0.0F;
		}

		return heatToSeconds(currentHeat(state, nowMs), state.overheatTimeMs());
	}

	public static void setHeat(@Nonnull HeatDataComponent state, float heat, int nowMs) {
		float clampedHeat = clampHeat(heat);
		state.setHeat(clampedHeat);
		state.setLastUseMs(clampedHeat > 0.0F
		                   ? Math.max(0, nowMs)
		                   : 0);
		state.setOverheated(clampedHeat >= 1.0F);
		state.setDirty(true);
	}

	public static boolean tick(@Nonnull HeatDataComponent state, float dtSeconds, int nowMs) {
		if (state.overheatTimeMs() <= 0 || state.cooldownTimeMs() <= 0) {
			return false;
		}

		float current = clampHeat(state.heat());
		boolean active = isActivelyFiring(state, nowMs) && !state.overheated();
		float next = current;
		if (active) {
			next = clampHeat(current + dtSeconds / ((float) state.overheatTimeMs() / 1000.0F));
		} else {
			next = clampHeat(current - dtSeconds / ((float) state.cooldownTimeMs() / 1000.0F));
		}

		boolean changed = Math.abs(next - current) > 0.000001F;
		if (changed) {
			state.setHeat(next);
			state.setDirty(true);
		}

		if (next >= 1.0F) {
			state.setOverheated(true);
		} else if (next <= 0.0F) {
			state.setOverheated(false);
			state.setLastUseMs(0);
			state.setFireStartMs(0);
			state.setFireBaseHeat(0.0F);
		}

		return changed;
	}

	@Nonnull
	public static Result onUse(@Nonnull HeatDataComponent state, boolean overheatEnabled, int overheatTimeMs, int cooldownTimeMs,
	                           int nowMs) {
		if (!overheatEnabled || overheatTimeMs <= 0) {
			state.clear();
			state.setOverheatTimeMs(overheatTimeMs);
			state.setCooldownTimeMs(Math.max(0, cooldownTimeMs));
			return new Result(0.0F, false);
		}

		state.setOverheatTimeMs(overheatTimeMs);
		state.setCooldownTimeMs(Math.max(1, cooldownTimeMs));
		if (state.overheated()) {
			return new Result(currentHeat(state, nowMs), true);
		}

		if (!isActivelyFiring(state, nowMs)) {
			state.setFireStartMs(nowMs);
			state.setFireBaseHeat(state.heat());
		}
		state.setLastUseMs(nowMs);

		return new Result(currentHeat(state, nowMs), false);
	}

	private static float clampHeat(float heat) {
		if (!Float.isFinite(heat)) {
			return 0.0F;
		}

		return Math.max(0.0F, Math.min(1.0F, heat));
	}

	public record Result(float heat, boolean failed) {
	}
}
