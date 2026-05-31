package moe.feo.shootexp.integration;

import moe.feo.shootexp.Config;
import moe.feo.shootexp.ShootEXP;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class ExpHandler {

    private ExpHandler() {}

    /**
     * Give experience to the player based on the configured exp-type.
     */
    public static void giveExp(Player player, int amount) {
        switch (Config.expType().toUpperCase()) {
            case "SKILLAPI" -> giveSkillApiExp(player, amount);
            case "MMOCORE" -> giveMMOCoreExp(player, amount);
            default -> giveVanillaExp(player, amount);
        }
    }

    private static void giveVanillaExp(Player player, int amount) {
        player.giveExperiencePoints(amount);
    }

    private static void giveSkillApiExp(Player player, int amount) {
        if (ModList.get().isLoaded("skillapi")) {
            try {
                // SkillAPI integration — uses reflection to avoid hard dependency
                Class<?> skillApi = Class.forName("com.sucy.skill.SkillAPI");
                Object playerData = skillApi.getMethod("getPlayerData", Player.class).invoke(null, player);
                if (playerData != null) {
                    playerData.getClass().getMethod("giveExp", double.class).invoke(playerData, (double) amount);
                }
            } catch (Exception e) {
                ShootEXP.LOGGER.warn("Failed to give SkillAPI exp, falling back to vanilla", e);
                giveVanillaExp(player, amount);
            }
        } else {
            giveVanillaExp(player, amount);
        }
    }

    private static void giveMMOCoreExp(Player player, int amount) {
        if (ModList.get().isLoaded("mmocore")) {
            try {
                // MMOCore integration — uses reflection to avoid hard dependency
                Class<?> mmocore = Class.forName("net.Indyuce.mmocore.MMOCore");
                Object playerData = mmocore.getMethod("getPlayerData", Player.class).invoke(null, player);
                if (playerData != null) {
                    playerData.getClass().getMethod("giveExperience", int.class).invoke(playerData, amount);
                }
            } catch (Exception e) {
                ShootEXP.LOGGER.warn("Failed to give MMOCore exp, falling back to vanilla", e);
                giveVanillaExp(player, amount);
            }
        } else {
            giveVanillaExp(player, amount);
        }
    }
}
