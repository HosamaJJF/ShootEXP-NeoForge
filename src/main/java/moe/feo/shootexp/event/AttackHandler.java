package moe.feo.shootexp.event;

import moe.feo.shootexp.Config;
import moe.feo.shootexp.ShootEXP;
import moe.feo.shootexp.item.ExpItem;
import moe.feo.shootexp.mechanic.Couple;
import moe.feo.shootexp.mechanic.CoupleManager;
import moe.feo.shootexp.mechanic.PlayerStatus;
import moe.feo.shootexp.mechanic.PlayerStatusManager;
import moe.feo.shootexp.util.ShootExpUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AttackHandler {

    private final Set<UUID> crouchingPlayers = new HashSet<>();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer)) return;
        if (player.level().isClientSide()) return;

        UUID id = player.getUUID();
        boolean isCrouching = player.isCrouching();
        boolean wasCrouching = crouchingPlayers.contains(id);

        // Detect crouch press (false → true)
        if (isCrouching && !wasCrouching) {
            handleCrouchPress(player);
        }

        if (isCrouching) {
            crouchingPlayers.add(id);
        } else {
            crouchingPlayers.remove(id);
        }

        // Check active couple for timeout and shoot
        Couple couple = CoupleManager.get(id);
        if (couple != null) {
            long currentTick = player.level().getGameTime();
            if (couple.hasTimedOut(currentTick)) {
                CoupleManager.remove(id);
            } else if (couple.canShoot()) {
                handleShoot(couple, player);
            }
        }
    }

    private void handleCrouchPress(Player player) {
        Entity target = ShootExpUtil.getNearestEntity(player);
        if (target == null) return;

        Couple couple = CoupleManager.getOrCreate(player, target);
        couple.attack();

        // Play attack sound only (no message - original plugin behavior)
        playSound(player, Config.soundAttack());
    }

    private void handleShoot(Couple couple, Player attacker) {
        PlayerStatus status = PlayerStatusManager.get(attacker.getUUID());
        int amount = status.ejaculation();
        Entity defender = couple.defender();

        if (amount <= 0) {
            // No EXP to shoot
            String msg = ShootExpUtil.lang("shootexp.message.shoot_no_exp")
                    .replace("%ATTACKER%", attacker.getName().getString())
                    .replace("%DEFENDER%", defender.getName().getString())
                    .replace("%TIMES%", String.valueOf(couple.numOfAttack()));
            broadcast(msg, attacker, defender);
            playSound(attacker, Config.soundShootNoExp());
        } else {
            // Create EXP item and drop it
            ItemStack expItem = ExpItem.create(
                    attacker.getName().getString(),
                    defender.getName().getString(),
                    amount);
            attacker.drop(expItem, false);

            String msg = ShootExpUtil.lang("shootexp.message.shoot")
                    .replace("%ATTACKER%", attacker.getName().getString())
                    .replace("%DEFENDER%", defender.getName().getString())
                    .replace("%AMOUNT%", String.valueOf(amount))
                    .replace("%TIMES%", String.valueOf(couple.numOfAttack()));
            broadcast(msg, attacker, defender);
            playSound(attacker, Config.soundShoot());
        }

        CoupleManager.remove(attacker.getUUID());
    }

    private void broadcast(String message, Player attacker, Entity defender) {
        if (Config.privateMessage()) {
            attacker.displayClientMessage(ShootExpUtil.formatComponent(message), false);
            if (defender instanceof Player p) {
                p.displayClientMessage(ShootExpUtil.formatComponent(message), false);
            }
        } else {
            ((ServerLevel) attacker.level()).getServer().getPlayerList().broadcastSystemMessage(
                    ShootExpUtil.formatComponent(message), false);
        }
    }

    private void playSound(Player player, String soundName) {
        var id = ShootExpUtil.parseSoundLocation(soundName);
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(id);
        if (soundEvent != null) {
            player.level().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    soundEvent, player.getSoundSource(), 1.0f, 1.0f);
        } else {
            ShootEXP.LOGGER.warn("Sound not found in registry: {} (parsed from: {})", id, soundName);
        }
    }
}
