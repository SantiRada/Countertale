package com.thescar.hygunsplugin;

import com.thescar.hygunsplugin.commands.HygunsCommand;
import com.thescar.hygunsplugin.config.HygunsConfig;
import com.thescar.hygunsplugin.content.migration.ItemIdVersioning;
import com.thescar.hygunsplugin.content.particles.ParticleColorRuleRegistry;
import com.thescar.hygunsplugin.content.particles.ParticleColorVariantService;
import com.thescar.hygunsplugin.content.particles.ParticleInteractionPaletteRegistry;
import com.thescar.hygunsplugin.content.registry.AmmoBoxAssetGenerator;
import com.thescar.hygunsplugin.content.registry.AmmoPileAssetGenerator;
import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.debug.DebugSettings;
import com.thescar.hygunsplugin.gameplay.player.PlayerEvents;
import com.thescar.hygunsplugin.gameplay.projectile.AutoGuidanceSystem;
import com.thescar.hygunsplugin.gameplay.projectile.ShootProjectile;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.gameplay.zoom.ScopeTickSystem;
import com.thescar.hygunsplugin.interactions.hud.HideAmmoHudInteraction;
import com.thescar.hygunsplugin.interactions.hud.UpdateAmmoHudInteraction;
import com.thescar.hygunsplugin.interactions.inventory.CheckDurabilityInteraction;
import com.thescar.hygunsplugin.interactions.inventory.HoldingInteraction;
import com.thescar.hygunsplugin.interactions.item.AddHeatInteraction;
import com.thescar.hygunsplugin.interactions.item.CheckHeatInteraction;
import com.thescar.hygunsplugin.interactions.item.HeatInteraction;
import com.thescar.hygunsplugin.interactions.item.SetHeatInteraction;
import com.thescar.hygunsplugin.interactions.weapon.*;
import com.thescar.hygunsplugin.interactions.zoom.*;
import com.thescar.hygunsplugin.runtime.components.AutoGuidanceDataComponent;
import com.thescar.hygunsplugin.runtime.components.ZoomStateComponent;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponPersistenceService;
import com.thescar.hygunsplugin.runtime.systems.ItemRuntimeStoreTickBridgeSystem;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;
import com.thescar.hygunsplugin.ui.hud.systems.ActiveSlotHudUpdateSystem;
import com.thescar.hygunsplugin.ui.hud.systems.InventoryHudUpdateSystem;
import com.thescar.hygunsplugin.ui.pages.AmmoSelectionPageSupplier;
import com.thescar.hygunsplugin.ui.pages.WeaponRepairPageSupplier;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.assetstore.event.AssetMonitorEvent;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.InventoryChangeEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.registry.CodecMapRegistry;
import com.hypixel.hytale.server.core.util.Config;

import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HygunsPluginMain extends JavaPlugin {
	public static final String PACK_PREFIX = "Hyguns";
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final Object ASSET_REFRESH_LOCK = new Object();
	private static final long ASSET_REFRESH_DEBOUNCE_MS = 250L;
	private static final InteractionRegistration<?>[] HYGUNS_INTERACTIONS = new InteractionRegistration<?>[]{
		new InteractionRegistration<>(ShootGunInteraction.KEY, ShootGunInteraction.class, ShootGunInteraction.CODEC),
		new InteractionRegistration<>(ReloadInteraction.KEY, ReloadInteraction.class, ReloadInteraction.CODEC),
		new InteractionRegistration<>(ReloadCheckInteraction.KEY, ReloadCheckInteraction.class, ReloadCheckInteraction.CODEC),
		new InteractionRegistration<>(GunValidationInteraction.KEY, GunValidationInteraction.class, GunValidationInteraction.CODEC),
		new InteractionRegistration<>(CanShootInteraction.KEY, CanShootInteraction.class, CanShootInteraction.CODEC),
		new InteractionRegistration<>(HeatInteraction.KEY, HeatInteraction.class, HeatInteraction.CODEC),
		new InteractionRegistration<>(CheckHeatInteraction.KEY, CheckHeatInteraction.class, CheckHeatInteraction.CODEC),
		new InteractionRegistration<>(SetHeatInteraction.KEY, SetHeatInteraction.class, SetHeatInteraction.CODEC),
		new InteractionRegistration<>(AddHeatInteraction.KEY, AddHeatInteraction.class, AddHeatInteraction.CODEC),
		new InteractionRegistration<>(UseAmmoInteraction.KEY, UseAmmoInteraction.class, UseAmmoInteraction.CODEC),
		new InteractionRegistration<>(SyncAmmoInteraction.KEY, SyncAmmoInteraction.class, SyncAmmoInteraction.CODEC),
		new InteractionRegistration<>(CheckAmmoTypeInteraction.KEY, CheckAmmoTypeInteraction.class, CheckAmmoTypeInteraction.CODEC),
		new InteractionRegistration<>(SetAmmoTypeInteraction.KEY, SetAmmoTypeInteraction.class, SetAmmoTypeInteraction.CODEC),
		new InteractionRegistration<>(SetAmmoInteraction.KEY, SetAmmoInteraction.class, SetAmmoInteraction.CODEC),
		new InteractionRegistration<>(AddAmmoInteraction.KEY, AddAmmoInteraction.class, AddAmmoInteraction.CODEC),
		new InteractionRegistration<>(CheckAmmoInteraction.KEY, CheckAmmoInteraction.class, CheckAmmoInteraction.CODEC),
		new InteractionRegistration<>(CheckZoomInteraction.KEY, CheckZoomInteraction.class, CheckZoomInteraction.CODEC),
		new InteractionRegistration<>(SetZoomInteraction.KEY, SetZoomInteraction.class, SetZoomInteraction.CODEC),
		new InteractionRegistration<>(ResetZoomInteraction.KEY, ResetZoomInteraction.class, ResetZoomInteraction.CODEC),
		new InteractionRegistration<>(CheckReloadingInteraction.KEY, CheckReloadingInteraction.class, CheckReloadingInteraction.CODEC),
		new InteractionRegistration<>(CancelReloadInteraction.KEY, CancelReloadInteraction.class, CancelReloadInteraction.CODEC),
		new InteractionRegistration<>(UpdateAmmoHudInteraction.KEY, UpdateAmmoHudInteraction.class, UpdateAmmoHudInteraction.CODEC),
		new InteractionRegistration<>(HideAmmoHudInteraction.KEY, HideAmmoHudInteraction.class, HideAmmoHudInteraction.CODEC),
		new InteractionRegistration<>(HoldingInteraction.KEY, HoldingInteraction.class, HoldingInteraction.CODEC),
		new InteractionRegistration<>(HoldingInteraction.SHORT_KEY, HoldingInteraction.class, HoldingInteraction.CODEC),
		new InteractionRegistration<>(CheckDurabilityInteraction.KEY, CheckDurabilityInteraction.class, CheckDurabilityInteraction.CODEC),
		new InteractionRegistration<>(CheckDurabilityInteraction.SHORT_KEY, CheckDurabilityInteraction.class, CheckDurabilityInteraction.CODEC)
	};
	private static final InteractionRegistration<?>[] SCOPE_INTERACTIONS = new InteractionRegistration<?>[]{
		new InteractionRegistration<>(ScopeZoomInteraction.KEY, ScopeZoomInteraction.class, ScopeZoomInteraction.CODEC),
		new InteractionRegistration<>(ScopeStepZoomInteraction.KEY, ScopeStepZoomInteraction.class, ScopeStepZoomInteraction.CODEC),
		new InteractionRegistration<>(ScopeZoomOutInteraction.KEY, ScopeZoomOutInteraction.class, ScopeZoomOutInteraction.CODEC),
		new InteractionRegistration<>(ScopeZoomInInteraction.KEY, ScopeZoomInInteraction.class, ScopeZoomInInteraction.CODEC)
	};
	private static final Set<Class<? extends EcsEvent>> ECS_EVENTS_SUBSCRIPTIONS = Set.of(
		InventoryChangeEvent.class,
		SwitchActiveSlotEvent.class
	);
	private static HygunsPluginMain instance;
	private final Config<HygunsConfig> config = this.withConfig("Hyguns", HygunsConfig.CODEC);

	private ScheduledFuture<?> pendingAssetRefresh;

	public HygunsPluginMain(JavaPluginInit init) {
		super(init);
		instance = this;
		LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
	}

	public static HygunsPluginMain instance() {
		return instance;
	}

	public static String key(String key) {
		return PACK_PREFIX + "_" + key;
	}

	@Override
	protected void setup() {
		DebugSettings.initialize(config);
		this.registerComponents();
		// Build item id to HyGuns settings map.
		GunRegistry.loadFromJar(getFile());
		AmmoRegistry.loadFromJar(getFile());
		// Build legacy item id migration map from resources.
		this.registerAssetReloadHooks();
		// Hyguns interactions
		this.registerCommands();
		this.registerInteractions(getCodecRegistry(Interaction.CODEC), HYGUNS_INTERACTIONS);
		this.registerInteractions(getCodecRegistry(Interaction.CODEC), SCOPE_INTERACTIONS);
		this.registerCustomPages();
		this.registerEntityEvents(ECS_EVENTS_SUBSCRIPTIONS);
		this.registerGlobalEvents();
		this.registerSystems();
	}

	@Override
	protected void start() {
		// Register after plugin-manager enable stage to avoid race with optional HUD
		// mods.
		AmmoPileAssetGenerator.refresh();
		AmmoBoxAssetGenerator.refresh();
		ItemIdVersioning.loadFromJar(getFile(), resolveHyGunsPackVersion());
		ParticleColorRuleRegistry.load(getFile());
		ParticleInteractionPaletteRegistry.load(getFile());
		ParticleColorVariantService.register();
	}

	@Override
	protected void shutdown() {
		synchronized (ASSET_REFRESH_LOCK) {
			if (this.pendingAssetRefresh != null) {
				this.pendingAssetRefresh.cancel(false);
				this.pendingAssetRefresh = null;
			}
		}

		try {
			HudCoordinator.shutdown();
		} catch (Exception | LinkageError t) {
			LOGGER.atWarning().log("HudCoordinator shutdown failed: %s", t.getMessage());
		}

		try {
			ReloadManager.shutdown();
		} catch (Exception | LinkageError t) {
			LOGGER.atWarning().log("ReloadManager shutdown failed: %s", t.getMessage());
		}

		try {
			RuntimeWeaponPersistenceService.get().shutdown();
		} catch (Exception | LinkageError t) {
			LOGGER.atWarning().log("Runtime weapon persistence shutdown failed: %s", t.getMessage());
		}

		super.shutdown();
	}

	private void registerComponents() {
		AutoGuidanceDataComponent.registerComponent(getEntityStoreRegistry());
		ZoomStateComponent.registerComponent(getEntityStoreRegistry());
	}

	private void registerCommands() {
		getCommandRegistry().registerCommand(new HygunsCommand());
	}

	private void registerInteractions(CodecMapRegistry.Assets<Interaction, ?> assets, InteractionRegistration<?>[] interactions) {
		for (InteractionRegistration<?> interaction : interactions) {
			registerInteraction(assets, interaction);
		}
	}

	private <T extends Interaction> void registerInteraction(CodecMapRegistry.Assets<Interaction, ?> assets,
	                                                         InteractionRegistration<T> interaction) {
		assets.register(interaction.key(), interaction.type(), interaction.codec());
	}

	private void registerCustomPages() {
		OpenCustomUIInteraction.registerCustomPageSupplier(
			this, WeaponRepairPageSupplier.class, "WeaponRepairKit",
			new WeaponRepairPageSupplier()
		);
		OpenCustomUIInteraction.registerCustomPageSupplier(
			this, AmmoSelectionPageSupplier.class, "AmmoSelection",
			new AmmoSelectionPageSupplier()
		);
	}

	private void registerEntityEvents(Set<Class<? extends EcsEvent>> events) {
		for (var eventClass : events) {
			try {
				getEntityStoreRegistry().registerEntityEventType(eventClass);
			} catch (IllegalArgumentException ignored) {
				// Already registered by the server.
			}
		}
	}

	private void registerGlobalEvents() {
		getEventRegistry().registerGlobal(PlayerReadyEvent.class, PlayerEvents::onPlayerReady);
		getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, PlayerEvents::onPlayerLeave);
		getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, PlayerEvents::onPlayerAddedToWorld);
		getEventRegistry().registerGlobal(DrainPlayerFromWorldEvent.class, PlayerEvents::onPlayerDrainedFromWorld);
	}

	private void registerSystems() {
		getEntityStoreRegistry().registerSystem(new ScopeTickSystem());
		getEntityStoreRegistry().registerSystem(new AutoGuidanceSystem());
		getEntityStoreRegistry().registerSystem(new ActiveSlotHudUpdateSystem());
		getEntityStoreRegistry().registerSystem(new InventoryHudUpdateSystem());
		getEntityStoreRegistry().registerSystem(new ItemRuntimeStoreTickBridgeSystem());
	}

	private void registerAssetReloadHooks() {
		DebugLogger.debug("Assets", "Registering asset reload hooks");
		registerAssetReloadHook(LoadedAssetsEvent.class, event -> requestRegistryRefresh());
		registerAssetReloadHook(RemovedAssetsEvent.class, event -> requestRegistryRefresh());
		registerAssetReloadHook(AssetMonitorEvent.class);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void registerAssetReloadHook(Class<?> eventClass) {
		getEventRegistry().registerGlobal((Class) eventClass, event -> requestAssetRefresh());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private <E extends com.hypixel.hytale.event.IBaseEvent<?>> void registerAssetReloadHook(Class<E> eventClass,
	                                                                                        java.util.function.Consumer<E> handler) {
		getEventRegistry().registerGlobal((Class) eventClass, event -> handler.accept((E) event));
	}

	private void requestAssetRefresh() {
		DebugLogger.debug("Assets", "Requested full asset refresh");
		scheduleRefresh(true);
	}

	private void requestRegistryRefresh() {
		DebugLogger.debug("Assets", "Requested registry-only refresh");
		scheduleRefresh(false);
	}

	private void scheduleRefresh(boolean includeGeneratedAssets) {
		DebugLogger.debug("Assets", () -> "Scheduling refresh includeGeneratedAssets=" + includeGeneratedAssets);
		synchronized (ASSET_REFRESH_LOCK) {
			ScheduledFuture<?> pending = this.pendingAssetRefresh;
			if (pending != null) {
				pending.cancel(false);
			}

			this.pendingAssetRefresh = HytaleServer.SCHEDULED_EXECUTOR.schedule(
				() -> {
					synchronized (ASSET_REFRESH_LOCK) {
						this.pendingAssetRefresh = null;
					}

					GunRegistry.refreshChangedResources();
					AmmoRegistry.refreshChangedResources();
					ItemIdVersioning.loadFromJar(getFile(), resolveHyGunsPackVersion());
					ShootProjectile.clearAssetCaches();
					ParticleColorRuleRegistry.refresh();
					ParticleInteractionPaletteRegistry.refresh();
					ParticleColorVariantService.clearCaches();
					int particleClients = ParticleColorVariantService.resendToKnownHandlers();
					DebugLogger.debug("Assets", "Refreshed gun/ammo registries");
					if (particleClients > 0) {
						DebugLogger.debug("Assets", () -> "Resent particle color variants to " + particleClients + " ready client(s)");
					}

					if (includeGeneratedAssets) {
						AmmoPileAssetGenerator.refresh();
						AmmoBoxAssetGenerator.refresh();
						DebugLogger.debug("Assets", "Refreshed generated ammo pile/box assets");
					}
				}, ASSET_REFRESH_DEBOUNCE_MS, TimeUnit.MILLISECONDS
			);
		}
	}

	private String resolveHyGunsPackVersion() {
		AssetModule assetModule = AssetModule.get();
		if (assetModule == null) {
			LOGGER.atWarning().log("AssetModule is unavailable. Could not resolve HyGuns pack version. Using 0.");
			return "0";
		}

		AssetPack foundPack = null;
		for (AssetPack pack : assetModule.getAssetPacks()) {
			if (pack == null || pack.getManifest() == null) {
				continue;
			}
			var manifest = pack.getManifest();
			String group = manifest.getGroup();
			String name = manifest.getName();
			boolean groupMatch = group != null && group.equalsIgnoreCase("thescar");
			boolean nameMatch = name != null && name.equalsIgnoreCase("hyguns");
			if (!(groupMatch && nameMatch)) {
				continue;
			}
			if (manifest.getVersion() == null) {
				continue;
			}
			if (foundPack == null) {
				foundPack = pack;
				continue;
			}

			var prevVersion = foundPack.getManifest().getVersion();
			var nextVersion = manifest.getVersion();
			if (prevVersion == null || nextVersion.compareTo(prevVersion) > 0) {
				foundPack = pack;
			}
		}

		if (foundPack != null && foundPack.getManifest() != null && foundPack.getManifest().getVersion() != null) {
			return foundPack.getManifest().getVersion().toString();
		}

		LOGGER.atWarning().log("Could not resolve HyGuns asset pack version via AssetModule. Using 0.");
		return "0";
	}

	private record InteractionRegistration<T extends Interaction>(String key, Class<T> type, BuilderCodec<T> codec) {
	}
}
