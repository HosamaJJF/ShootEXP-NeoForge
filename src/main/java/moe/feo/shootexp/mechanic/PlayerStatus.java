package moe.feo.shootexp.mechanic;

import moe.feo.shootexp.Config;
import moe.feo.shootexp.item.ExpItem;
import moe.feo.shootexp.ShootEXP;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.UUID;

public class PlayerStatus {

    private final UUID playerId;
    private int timesOfShoot;
    private int stock;
    private long lastRestoreShootTick;
    private long lastRestoreStockTick;

    public PlayerStatus(UUID playerId) {
        this.playerId = playerId;
        this.stock = Config.maxStock();
    }

    public int getTimesOfShoot() {
        return timesOfShoot;
    }

    public int getStock() {
        return stock;
    }

    public void setTimesOfShoot(int times) {
        this.timesOfShoot = Math.max(0, times);
    }

    public void setStock(int s) {
        this.stock = Math.max(0, Math.min(Config.maxStock(), s));
    }

    public void addShootTimes(int delta) {
        setTimesOfShoot(this.timesOfShoot + delta);
    }

    public void addStock(int delta) {
        setStock(this.stock + delta);
    }

    public int getRequiredAttackTimes() {
        return (int) Math.floor(new ExpressionBuilder(Config.requiredAttackTimesFormula())
                .variable("SHOOT")
                .variable("STOCK")
                .variable("MAXSTOCK")
                .build()
                .setVariable("SHOOT", timesOfShoot)
                .setVariable("STOCK", stock)
                .setVariable("MAXSTOCK", Config.maxStock())
                .evaluate());
    }

    public int getShootAmount() {
        int amount = (int) Math.floor(new ExpressionBuilder(Config.shootAmountFormula())
                .variable("SHOOT")
                .variable("STOCK")
                .variable("MAXSTOCK")
                .build()
                .setVariable("SHOOT", timesOfShoot)
                .setVariable("STOCK", stock)
                .setVariable("MAXSTOCK", Config.maxStock())
                .evaluate());
        return Math.min(amount, stock);
    }

    public void restoreAll() {
        this.timesOfShoot = 0;
        this.stock = Config.maxStock();
    }

    public void tick(long currentTick) {
        if (lastRestoreShootTick == 0) lastRestoreShootTick = currentTick;
        if (lastRestoreStockTick == 0) lastRestoreStockTick = currentTick;

        if (currentTick - lastRestoreShootTick >= Config.restoreShootPeriod()) {
            addShootTimes(-Config.restoreShootAmount());
            lastRestoreShootTick = currentTick;
        }
        if (currentTick - lastRestoreStockTick >= Config.restoreStockPeriod()) {
            addStock(Config.restoreStockAmount());
            lastRestoreStockTick = currentTick;
        }
    }

    /**
     * Called when the player successfully "shoots". Returns the amount of EXP produced.
     */
    public int ejaculation() {
        int amount = getShootAmount();
        if (amount <= 0) return 0;
        addStock(-amount);
        addShootTimes(1);
        return amount;
    }
}
