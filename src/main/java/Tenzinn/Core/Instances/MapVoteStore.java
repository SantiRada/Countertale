package Tenzinn.Core.Instances;

import Tenzinn.Core.Listeners.MapListeners;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacena de forma temporal los votos de mapa por jugador.
 *
 * Los votos se establecen en {@link Tenzinn.Core.UI.ModesPage} justo antes de encolar
 * al jugador y se consumen en {@link Tenzinn.Core.Commands.QueueCommand}.
 *
 * Si un jugador accede directamente con /queue sin pasar por la UI (admin, consola, etc.),
 * {@link #getVotes} devuelve todos los mapas disponibles como valor por defecto,
 * maximizando su compatibilidad con otras partidas.
 */
public final class MapVoteStore {

    private static final Map<UUID, List<String>> votes = new ConcurrentHashMap<>();

    private MapVoteStore() {}

    /**
     * Guarda los mapas seleccionados por el jugador.
     * Reemplaza cualquier voto previo sin merge.
     */
    public static void setVotes(PlayerRef playerRef, List<String> maps) {
        if (playerRef == null || maps == null) return;
        votes.put(playerRef.getUuid(), Collections.unmodifiableList(new ArrayList<>(maps)));
    }

    /**
     * Devuelve los votos almacenados del jugador.
     * Si no hay votos (acceso directo sin ModesPage), devuelve todos los mapas registrados.
     */
    public static List<String> getVotes(PlayerRef playerRef) {
        if (playerRef == null) return new ArrayList<>(MapListeners.getMapNames());
        List<String> stored = votes.get(playerRef.getUuid());
        if (stored == null || stored.isEmpty()) {
            return new ArrayList<>(MapListeners.getMapNames());
        }
        return new ArrayList<>(stored);
    }

    /**
     * Elimina los votos del jugador. Llamar tras consumirlos para no dejar datos obsoletos.
     */
    public static void clearVotes(PlayerRef playerRef) {
        if (playerRef != null) votes.remove(playerRef.getUuid());
    }
}
