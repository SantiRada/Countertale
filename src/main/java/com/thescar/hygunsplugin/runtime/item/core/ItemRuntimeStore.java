package com.thescar.hygunsplugin.runtime.item.core;

public record ItemRuntimeStore(String name) {
	public ItemRuntimeStore(String name) {
		this.name = name == null || name.isBlank()
		            ? "global"
		            : name;
	}


	@Override
	public String toString() {
		return "ItemRuntimeStore[" + this.name + "]";
	}
}
