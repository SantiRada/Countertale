package com.thescar.hygunsplugin.ui.hud.style;

public final class Gradients {
	public static final ColorGradient OVERHEAT = new ColorGradient(
		new GradientStop(0.0F, "#FFFFFF"), new GradientStop(0.333F, "#FFFF00"),
		new GradientStop(0.666F, "#FF8000"), new GradientStop(1.0F, "#FF0000")
	);

	private Gradients() {
	}
}
