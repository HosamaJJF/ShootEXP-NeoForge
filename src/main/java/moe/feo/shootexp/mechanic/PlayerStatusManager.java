package moe.feo.shootexp.mechanic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatusManager {

    private static final Map<UUID, PlayerStatus> statuses = new HashMap<>();

    public static PlayerStatus get(UUID playerId) {
        return statuses.computeIfAbsent(playerId, PlayerStatus::new);
    }

    public static void remove(UUID playerId) {
        statuses.remove(playerId);
    }

    public static void tickAll(long currentTick) {
        for (PlayerStatus status : statuses.values()) {
            status.tick(currentTick);
        }
    }
}
