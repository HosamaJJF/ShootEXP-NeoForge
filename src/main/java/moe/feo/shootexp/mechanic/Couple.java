package moe.feo.shootexp.mechanic;

import moe.feo.shootexp.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class Couple {

    private final Player attacker;
    private final Entity defender;
    private int numOfAttack;
    private long lastAttackTick;

    public Couple(Player attacker, Entity defender) {
        this.attacker = attacker;
        this.defender = defender;
        this.numOfAttack = 0;
        this.lastAttackTick = attacker.level().getGameTime();
    }

    public Player attacker() {
        return attacker;
    }

    public Entity defender() {
        return defender;
    }

    public int numOfAttack() {
        return numOfAttack;
    }

    public void attack() {
        numOfAttack++;
        lastAttackTick = attacker.level().getGameTime();
    }

    public boolean hasTimedOut(long currentTick) {
        return currentTick - lastAttackTick > Config.attackTimeout();
    }

    public boolean canShoot() {
        PlayerStatus status = PlayerStatusManager.get(attacker.getUUID());
        return numOfAttack >= status.getRequiredAttackTimes();
    }
}
