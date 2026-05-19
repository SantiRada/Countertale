package Tenzinn.Core.Effects;

import Tenzinn.Core.Localization.Lang;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;

import java.util.logging.Level;
import javax.annotation.Nonnull;
import java.util.logging.Logger;

public class PlayerEntityEffect extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("Countertale");

    public PlayerEntityEffect(@Nonnull JavaPluginInit init) { super(init); }

    public static void applyEffect(Player player, String effectString, ComponentAccessor<EntityStore> accessor) {
        Ref<EntityStore> entityRef = player.getReference();

        assert entityRef != null;
        PlayerRef playerRef = accessor.getComponent(entityRef, PlayerRef.getComponentType());
        String playerName = playerRef != null ? playerRef.getUsername() : "unknown";

        EffectControllerComponent controller = accessor.getComponent(entityRef, EffectControllerComponent.getComponentType());
        if (playerRef != null) {
            playerRef.sendMessage(Lang.msg("effect.applied", "effect", effectString, "player", playerName));
        }

        if (controller == null) {
            LOGGER.log(Level.WARNING, "EffectControllerComponent no encontrado en: " + playerName);
            return;
        }

        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectString);

        if (effect == null) {
            LOGGER.log(Level.WARNING, "EntityEffect 'Countertale:' " + effectString + " no encontrado en los assets");
            return;
        }

        // También pasá el índice explícitamente — es la sobrecarga más robusta
        int effectIndex = EntityEffect.getAssetMap().getIndex(effectString);
        controller.addEffect(entityRef, effectIndex, effect, accessor);

        LOGGER.log(Level.INFO, "Efecto aplicado a " + playerName);
    }
    public static void clearAllEffects(Player player, ComponentAccessor<EntityStore> accessor) {
        Ref<EntityStore> entityRef = player.getReference();

        assert entityRef != null;
        PlayerRef playerRef = accessor.getComponent(entityRef, PlayerRef.getComponentType());
        String playerName = playerRef != null ? playerRef.getUsername() : "unknown";

        EffectControllerComponent controller = accessor.getComponent(entityRef, EffectControllerComponent.getComponentType());
        if (controller == null) {
            LOGGER.log(Level.WARNING, "EffectControllerComponent no encontrado en: " + playerName);
            return;
        }

        controller.clearEffects(entityRef, accessor);
        LOGGER.log(Level.INFO, "SE LIMPIARON LOS EFECTOS a " + playerName);
    }
}
