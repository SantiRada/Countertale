package com.thescar.hygunsplugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class HygunsConfig {
	public static final BuilderCodec<HygunsConfig> CODEC = BuilderCodec.builder(HygunsConfig.class, HygunsConfig::new)
		.append(new KeyedCodec<>("Debug", Codec.BOOLEAN), (c, v) -> c.debug = v, c -> c.debug).add().build();

	private boolean debug = false;

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}
