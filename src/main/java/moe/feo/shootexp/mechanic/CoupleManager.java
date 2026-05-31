package moe.feo.shootexp.mechanic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class CoupleManager {

    private static final Map<UUID, Couple> couples = new HashMap<>();

    /**
     * Get or create a couple for the given attacker. If the defender changed, the couple is replaced.
     */
    public static Couple getOrCreate(Player attacker, Entity defender) {
        Couple existing = couples.get(attacker.getUUID());
        if (existing != null && existing.defender().getUUID().equals(defender.getUUID())) {
            return existing;
        }
        Couple couple = new Couple(attacker, defender);
        couples.put(attacker.getUUID(), couple);
        return couple;
    }

    public static void remove(UUID attackerId) {
        couples.remove(attackerId);
    }

    public static Couple get(UUID attackerId) {
        return couples.get(attackerId);
    }

    public static Collection<Couple> all() {
        return couples.values();
    }
}
