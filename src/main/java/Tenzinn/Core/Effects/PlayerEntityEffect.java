package Tenzinn.Core.Effects;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;

import java.util.logging.Level;
import javax.annotation.Nonnull;
import java.util.logging.Logger;

public class PlayerEntityEffect extends JavaPlugin {

    private static final Logger LOGGER = Logger.getLogger("OrbisOffensive");

    public PlayerEntityEffect(@Nonnull JavaPluginInit init) { super(init); }

    public static void applyEffect(Player player, String effectString, ComponentAccessor<EntityStore> accessor) {
        Ref<EntityStore> entityRef = player.getReference();

        assert entityRef != null;
        EffectControllerComponent controller = accessor.getComponent(entityRef, EffectControllerComponent.getComponentType());
        player.sendMessage(Message.raw("Applying effect " + effectString + " to player " + player.getDisplayName()));

        if (controller == null) {
            LOGGER.log(Level.WARNING, "EffectControllerComponent not found in: " + player.getDisplayName());
            return;
        }

        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectString);

        if (effect == null) {
            LOGGER.log(Level.WARNING, "EntityEffect 'OrbisOffensive:' " + effectString + " not found in assets");
            return;
        }

        // Also pass the explicit index; this overload is the most robust.
        int effectIndex = EntityEffect.getAssetMap().getIndex(effectString);
        controller.addEffect(entityRef, effectIndex, effect, accessor);

        LOGGER.log(Level.INFO, "Effect applied to " + player.getDisplayName());
    }
    public static void clearAllEffects(Player player, ComponentAccessor<EntityStore> accessor) {
        Ref<EntityStore> entityRef = player.getReference();

        assert entityRef != null;
        EffectControllerComponent controller = accessor.getComponent(entityRef, EffectControllerComponent.getComponentType());
        if (controller == null) {
            LOGGER.log(Level.WARNING, "EffectControllerComponent not found in: " + player.getDisplayName());
            return;
        }

        controller.clearEffects(entityRef, accessor);
        LOGGER.log(Level.INFO, "Cleared all effects for " + player.getDisplayName());
    }
}
