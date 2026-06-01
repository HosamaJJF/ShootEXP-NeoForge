package moe.feo.shootexp.item;

import moe.feo.shootexp.Config;
import moe.feo.shootexp.util.ShootExpUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExpItem {

    private static final String TAG_KEY = "shootexp";

    private ExpItem() {}

    public static ItemStack create(Entity owner, Entity recipient, int amount) {
        ItemStack stack = new ItemStack(Items.BONE_MEAL);
        stack.setCount(1);

        Component ownerDisplay = owner.getDisplayName();
        Component recipientDisplay = recipient.getDisplayName();

        // Store data in CUSTOM_DATA component
        String ownerName = owner.getName().getString();
        String recipientName = recipient.getName().getString();
        String ownerDescId = owner.getType().getDescriptionId();
        String recipientDescId = recipient.getType().getDescriptionId();
        CompoundTag tag = new CompoundTag();
        tag.putString("owner", ownerName);
        tag.putString("recipient", recipientName);
        tag.putString("ownerDescId", ownerDescId);
        tag.putString("recipientDescId", recipientDescId);
        tag.putInt("amount", amount);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // Build placeholder map for item name and lore
        Map<String, Component> placeholders = new HashMap<>();
        placeholders.put("OWNER", ownerDisplay);
        placeholders.put("RECIPIENT", recipientDisplay);
        placeholders.put("AMOUNT", Component.literal(String.valueOf(amount)));

        // Custom name
        stack.set(DataComponents.CUSTOM_NAME,
                ShootExpUtil.formatMessage(ShootExpUtil.lang("shootexp.item.name"), placeholders));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(ShootExpUtil.formatMessage(ShootExpUtil.lang("shootexp.item.lore.1"), placeholders));
        lore.add(ShootExpUtil.formatMessage(ShootExpUtil.lang("shootexp.item.lore.2"), placeholders));
        lore.add(ShootExpUtil.formatComponent(ShootExpUtil.lang("shootexp.item.lore.3")));
        lore.add(ShootExpUtil.formatComponent(ShootExpUtil.lang("shootexp.item.lore.4")));
        stack.set(DataComponents.LORE, new ItemLore(lore));

        // Custom model data (reflection for 1.21.0-1.21.1 vs 1.21.2+)
        if (Config.customModelDataEnable()) {
            setCustomModelData(stack, (float) Config.customModelDataValue());
        }

        return stack;
    }

    private static void setCustomModelData(ItemStack stack, float value) {
        try {
            Constructor<?> ctor = CustomModelData.class.getConstructor(
                    List.class, List.class, List.class, List.class);
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                    (CustomModelData) ctor.newInstance(List.of(value), List.of(), List.of(), List.of()));
        } catch (NoSuchMethodException e) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData((int) value));
        } catch (Exception e) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData((int) value));
        }
    }

    public static boolean isExpItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.BONE_MEAL)) return false;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        CompoundTag tag = customData.copyTag();
        return tag.contains("owner") && tag.contains("recipient") && tag.contains("amount");
    }

    public static String getOwner(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag().getString("owner");
    }

    public static String getOwnerDescId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag().getString("ownerDescId");
    }

    public static String getRecipient(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag().getString("recipient");
    }

    public static String getRecipientDescId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag().getString("recipientDescId");
    }

    public static int getAmount(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getInt("amount");
    }
}
