package moe.feo.shootexp.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import moe.feo.shootexp.Config;
import moe.feo.shootexp.ShootEXP;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ShootExpUtil {

    private static Map<String, String> langMap;
    private static final Gson GSON = new Gson();

    private ShootExpUtil() {}

    public static Map<String, String> loadLang() {
        if (langMap != null) return langMap;
        String lang = Config.lang();
        String path = "/assets/shootexp/lang/" + lang + ".json";
        try {
            InputStream stream = ShootExpUtil.class.getResourceAsStream(path);
            if (stream == null) {
                ShootEXP.LOGGER.warn("Language file not found: {}, falling back to en_us", lang);
                stream = ShootExpUtil.class.getResourceAsStream("/assets/shootexp/lang/en_us.json");
            }
            if (stream == null) {
                ShootEXP.LOGGER.error("Fallback language file en_us.json not found!");
                langMap = new HashMap<>();
            } else {
                try (Reader reader = new InputStreamReader(stream)) {
                    langMap = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                }
            }
        } catch (Exception e) {
            ShootEXP.LOGGER.error("Failed to load language file: {}", lang, e);
            langMap = new HashMap<>();
        }
        return langMap;
    }

    public static String lang(String key) {
        Map<String, String> map = loadLang();
        return map.getOrDefault(key, key);
    }

    public static void clearLangCache() {
        langMap = null;
    }

    /**
     * Convert & color codes to Minecraft Component.
     */
    public static Component formatComponent(String message) {
        if (message == null) return Component.empty();
        MutableComponent result = Component.empty();
        StringBuilder current = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '&' && i + 1 < message.length()) {
                char code = message.charAt(i + 1);
                // Flush current text
                if (current.length() > 0) {
                    result.append(Component.literal(current.toString()).withStyle(currentStyle));
                    current = new StringBuilder();
                }
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (formatting.isColor()) {
                        currentStyle = currentStyle.withColor(formatting);
                    } else {
                        currentStyle = currentStyle.applyFormat(formatting);
                    }
                }
                i++; // skip the color code
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.append(Component.literal(current.toString()).withStyle(currentStyle));
        }
        return result;
    }

    public static Component formatMessage(String template, Map<String, Component> placeholders) {
        if (template == null) return Component.empty();
        if (placeholders == null) placeholders = Collections.emptyMap();
        MutableComponent result = Component.empty();
        StringBuilder current = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '%') {
                int end = template.indexOf('%', i + 1);
                if (end > i) {
                    if (current.length() > 0) {
                        result.append(Component.literal(current.toString()).withStyle(currentStyle));
                        current = new StringBuilder();
                    }
                    String key = template.substring(i + 1, end);
                    Component replacement = placeholders.get(key);
                    if (replacement != null) {
                        Style merged = currentStyle.applyTo(replacement.getStyle());
                        result.append(replacement.copy().withStyle(merged));
                    }
                    i = end + 1;
                    continue;
                }
            }
            if (c == '&' && i + 1 < template.length()) {
                char code = template.charAt(i + 1);
                if (current.length() > 0) {
                    result.append(Component.literal(current.toString()).withStyle(currentStyle));
                    current = new StringBuilder();
                }
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (formatting.isColor()) {
                        currentStyle = currentStyle.withColor(formatting);
                    } else {
                        currentStyle = currentStyle.applyFormat(formatting);
                    }
                }
                i += 2;
                continue;
            }
            current.append(c);
            i++;
        }
        if (current.length() > 0) {
            result.append(Component.literal(current.toString()).withStyle(currentStyle));
        }
        return result;
    }

    /**
     * Find the nearest entity matching configured entity type names.
     */
    public static Entity getNearestEntity(Player player) {
        Level level = player.level();
        double dist = Config.attackDistance();
        AABB box = player.getBoundingBox().inflate(dist, dist, dist);
        List<Entity> nearby = level.getEntities(player, box, e -> e != player && isTargetable(e));

        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity e : nearby) {
            double d = player.distanceToSqr(e);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = e;
            }
        }
        return nearest;
    }

    // Subpackages to search for entity classes, ordered by specificity
    private static final String[] ENTITY_PACKAGES = {
        "",
        "player",
        "animal",
        "animal.sheep",
        "animal.cow",
        "animal.pig",
        "animal.chicken",
        "animal.horse",
        "animal.wolf",
        "animal.cat",
        "animal.fox",
        "animal.rabbit",
        "animal.fish",
        "monster",
        "boss",
        "npc",
        "creature",
        "ambient",
        "flying",
        "water"
    };

    private static boolean isTargetable(Entity entity) {
        Class<?> entityClass = entity.getClass();
        for (String typeName : Config.entityTypes()) {
            // Try direct class name
            try {
                Class<?> clazz = Class.forName("net.minecraft.world.entity." + typeName);
                if (clazz.isAssignableFrom(entityClass)) return true;
            } catch (ClassNotFoundException ignored) {}

            // Try all known subpackages
            for (String pkg : ENTITY_PACKAGES) {
                try {
                    String fullName = pkg.isEmpty()
                        ? "net.minecraft.world.entity." + typeName
                        : "net.minecraft.world.entity." + pkg + "." + typeName;
                    Class<?> clazz = Class.forName(fullName);
                    if (clazz.isAssignableFrom(entityClass)) return true;
                } catch (ClassNotFoundException ignored) {}
            }
        }
        return false;
    }

    public static Component entityDisplayName(Entity entity) {
        return entity.getDisplayName();
    }

    /**
     * Parse an Identifier from the config sound string.
     */
    public static Identifier parseSoundLocation(String soundName) {
        return Identifier.parse(soundName);
    }
}
