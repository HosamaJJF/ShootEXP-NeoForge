package moe.feo.shootexp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class Config {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("shootexp.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static String lang = "zh_cn";
    private static boolean privateMessage = false;
    private static int maxStock = 1000;
    private static String requiredAttackTimes = "1.618^SHOOT + 10";
    private static String shootAmount = "STOCK / 2";
    private static List<String> entityTypes = List.of("Player", "PathfinderMob");
    private static String expType = "VANILLA";
    private static double attackDistance = 2.0;
    private static int attackTimeout = 100;
    private static int restoreShootPeriod = 6000;
    private static int restoreShootAmount = 1;
    private static int restoreStockPeriod = 6000;
    private static int restoreStockAmount = 200;
    private static boolean customModelDataEnable = false;
    private static int customModelDataValue = 0;
    private static String soundAttack = "entity.parrot.imitate.slime";
    private static String soundShoot = "block.slime_block.step";
    private static String soundShootNoExp = "entity.llama.eat";
    private static String soundEat = "entity.generic.drink";

    @SuppressWarnings("unchecked")
    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            createDefault();
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Map<String, Object> data = GSON.fromJson(reader,
                    new TypeToken<Map<String, Object>>() {}.getType());
            if (data == null) return;

            lang = getString(data, "lang", lang);
            privateMessage = getBoolean(data, "private-message", privateMessage);
            maxStock = getInt(data, "max-stock", maxStock);
            requiredAttackTimes = getString(data, "required-attack-times", requiredAttackTimes);
            shootAmount = getString(data, "shoot-amount", shootAmount);
            expType = getString(data, "exp-type", expType);
            customModelDataEnable = getBoolean(data, "custom-model-data.enable", customModelDataEnable);
            customModelDataValue = getInt(data, "custom-model-data.value", customModelDataValue);

            if (data.containsKey("entity-type") && data.get("entity-type") instanceof List) {
                entityTypes = (List<String>) data.get("entity-type");
            }

            Map<String, Object> attack = (Map<String, Object>) data.get("attack");
            if (attack != null) {
                if (attack.get("distance") instanceof Number) {
                    attackDistance = ((Number) attack.get("distance")).doubleValue();
                }
                attackTimeout = getInt(attack, "timeout", attackTimeout);
            }

            Map<String, Object> restore = (Map<String, Object>) data.get("restore");
            if (restore != null) {
                Map<String, Object> shoot = (Map<String, Object>) restore.get("shoot");
                if (shoot != null) {
                    restoreShootPeriod = getInt(shoot, "period", restoreShootPeriod);
                    restoreShootAmount = getInt(shoot, "amount", restoreShootAmount);
                }
                Map<String, Object> stock = (Map<String, Object>) restore.get("stock");
                if (stock != null) {
                    restoreStockPeriod = getInt(stock, "period", restoreStockPeriod);
                    restoreStockAmount = getInt(stock, "amount", restoreStockAmount);
                }
            }

            Map<String, Object> sound = (Map<String, Object>) data.get("sound");
            if (sound != null) {
                soundAttack = getString(sound, "attack", soundAttack);
                soundShoot = getString(sound, "shoot", soundShoot);
                soundShootNoExp = getString(sound, "shoot-no-exp", soundShootNoExp);
                soundEat = getString(sound, "eat", soundEat);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
        }
    }

    private static void createDefault() {
        try (InputStream in = Config.class.getClassLoader()
                .getResourceAsStream("assets/shootexp/default_config.json")) {
            if (in != null) {
                Files.copy(in, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create default config", e);
        }
    }

    private static String getString(Map<String, Object> map, String key, String def) {
        Object val = getNested(map, key);
        return val instanceof String ? (String) val : def;
    }

    private static boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object val = getNested(map, key);
        return val instanceof Boolean ? (Boolean) val : def;
    }

    private static int getInt(Map<String, Object> map, String key, int def) {
        Object val = getNested(map, key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    private static Object getNested(Map<String, Object> map, String key) {
        String[] parts = key.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (!(current instanceof Map)) return null;
            current = ((Map<String, Object>) current).get(part);
        }
        return current;
    }

    // --- Accessors ---

    public static String lang() { return lang; }
    public static boolean privateMessage() { return privateMessage; }
    public static int maxStock() { return maxStock; }
    public static String requiredAttackTimesFormula() { return requiredAttackTimes; }
    public static String shootAmountFormula() { return shootAmount; }
    public static List<String> entityTypes() { return entityTypes; }
    public static String expType() { return expType; }
    public static double attackDistance() { return attackDistance; }
    public static int attackTimeout() { return attackTimeout; }
    public static int restoreShootPeriod() { return restoreShootPeriod; }
    public static int restoreShootAmount() { return restoreShootAmount; }
    public static int restoreStockPeriod() { return restoreStockPeriod; }
    public static int restoreStockAmount() { return restoreStockAmount; }
    public static boolean customModelDataEnable() { return customModelDataEnable; }
    public static int customModelDataValue() { return customModelDataValue; }
    public static String soundAttack() { return soundAttack; }
    public static String soundShoot() { return soundShoot; }
    public static String soundShootNoExp() { return soundShootNoExp; }
    public static String soundEat() { return soundEat; }
}
