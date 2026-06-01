package moe.feo.shootexp.event;

import moe.feo.shootexp.Config;
import moe.feo.shootexp.ShootEXP;
import moe.feo.shootexp.item.ExpItem;
import moe.feo.shootexp.mechanic.Couple;
import moe.feo.shootexp.mechanic.CoupleManager;
import moe.feo.shootexp.mechanic.PlayerStatus;
import moe.feo.shootexp.mechanic.PlayerStatusManager;
import moe.feo.shootexp.util.ShootExpUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

        Map<String, net.minecraft.network.chat.Component> placeholders = new HashMap<>();
        placeholders.put("ATTACKER", attacker.getDisplayName());
        placeholders.put("DEFENDER", defender.getDisplayName());
        placeholders.put("TIMES", net.minecraft.network.chat.Component.literal(String.valueOf(couple.numOfAttack())));

        if (amount <= 0) {
            // No EXP to shoot
            broadcast(ShootExpUtil.lang("shootexp.message.shoot_no_exp"), placeholders, attacker, defender);
            playSound(attacker, Config.soundShootNoExp());
        } else {
            // Create EXP item and drop it
            ItemStack expItem = ExpItem.create(attacker, defender, amount);
            attacker.drop(expItem, false);

            placeholders.put("AMOUNT", net.minecraft.network.chat.Component.literal(String.valueOf(amount)));
            broadcast(ShootExpUtil.lang("shootexp.message.shoot"), placeholders, attacker, defender);
            playSound(attacker, Config.soundShoot());
        }

        CoupleManager.remove(attacker.getUUID());
    }

    private void broadcast(String template, Map<String, net.minecraft.network.chat.Component> placeholders, Player attacker, Entity defender) {
        net.minecraft.network.chat.Component msg = ShootExpUtil.formatMessage(template, placeholders);
        if (Config.privateMessage()) {
            attacker.displayClientMessage(msg, false);
            if (defender instanceof Player p) {
                p.displayClientMessage(msg, false);
            }
        } else {
            ((ServerLevel) attacker.level()).getServer().getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    private void playSound(Player player, String soundName) {
        var id = ShootExpUtil.parseSoundLocation(soundName);
        var optHolder = BuiltInRegistries.SOUND_EVENT.get(id);
        if (optHolder.isPresent()) {
            Holder<SoundEvent> holder = optHolder.get();
            player.level().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    holder, player.getSoundSource(), 1.0f, 1.0f);
        } else {
            ShootEXP.LOGGER.warn("Sound not found in registry: {} (parsed from: {})", id, soundName);
        }
    }
}
