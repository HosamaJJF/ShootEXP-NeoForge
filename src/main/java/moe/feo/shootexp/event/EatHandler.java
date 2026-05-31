package moe.feo.shootexp.event;

import moe.feo.shootexp.Config;
import moe.feo.shootexp.ShootEXP;
import moe.feo.shootexp.integration.ExpHandler;
import moe.feo.shootexp.item.ExpItem;
import moe.feo.shootexp.util.ShootExpUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class EatHandler {

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (handleEat(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // Brewery/BreweryX compatibility check
        if (ModList.get().isLoaded("brewery") || ModList.get().isLoaded("breweryx")) {
            var pos = event.getPos();
            var level = event.getLevel();
            var block = level.getBlockState(pos).getBlock();
            var key = block.builtInRegistryHolder().key();
            if (key != null) {
                String keyStr = key.location().toString();
                // Brewery barrel and cauldron block IDs
                if (keyStr.contains("brewery") && (keyStr.contains("barrel") || keyStr.contains("cauldron"))) {
                    return; // Don't consume EXP item when interacting with Brewery
                }
            }
        }
        if (handleEat(event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    private boolean handleEat(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer)) return false;
        if (!ExpItem.isExpItem(stack)) return false;

        String owner = ExpItem.getOwner(stack);
        String recipient = ExpItem.getRecipient(stack);
        int amount = ExpItem.getAmount(stack);

        // Give experience
        ExpHandler.giveExp(player, amount);

        // Consume one item
        stack.shrink(1);

        // Play eat sound
        var soundId = ShootExpUtil.parseSoundLocation(Config.soundEat());
        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (soundEvent != null) {
            player.level().playSound(
                    null, player.getX(), player.getY(), player.getZ(),
                    soundEvent, player.getSoundSource(), 1.0f, 1.0f);
        } else {
            ShootEXP.LOGGER.warn("Sound not found in registry: {}", soundId);
        }

        // Broadcast message
        ServerLevel serverLevel = (ServerLevel) player.level();
        String msg = ShootExpUtil.lang("shootexp.message.eat")
                .replace("%PLAYER%", player.getName().getString())
                .replace("%OWNER%", owner)
                .replace("%RECIPIENT%", recipient)
                .replace("%AMOUNT%", String.valueOf(amount));
        if (Config.privateMessage()) {
            player.displayClientMessage(ShootExpUtil.formatComponent(msg), false);
            // Notify the owner
            var ownerPlayer = serverLevel.getServer().getPlayerList().getPlayerByName(owner);
            if (ownerPlayer != null) {
                ownerPlayer.displayClientMessage(ShootExpUtil.formatComponent(msg), false);
            }
            // Notify the recipient
            if (recipient != null) {
                var recipientPlayer = serverLevel.getServer().getPlayerList().getPlayerByName(recipient);
                if (recipientPlayer != null) {
                    recipientPlayer.displayClientMessage(ShootExpUtil.formatComponent(msg), false);
                }
            }
        } else {
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                    ShootExpUtil.formatComponent(msg), false);
        }

        return true;
    }
}
