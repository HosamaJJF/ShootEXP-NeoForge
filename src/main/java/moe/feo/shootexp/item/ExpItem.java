package moe.feo.shootexp.item;

import moe.feo.shootexp.Config;
import moe.feo.shootexp.util.ShootExpUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public final class ExpItem {

    private static final String TAG_KEY = "shootexp";

    private ExpItem() {}

    public static ItemStack create(String owner, String recipient, int amount) {
        ItemStack stack = new ItemStack(Items.BONE_MEAL);
        stack.setCount(1);

        // Store data in CUSTOM_DATA component
        CompoundTag tag = new CompoundTag();
        tag.putString("owner", owner);
        tag.putString("recipient", recipient);
        tag.putInt("amount", amount);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // Custom name
        String nameFormat = ShootExpUtil.lang("shootexp.item.name")
                .replace("%OWNER%", owner)
                .replace("%RECIPIENT%", recipient);
        stack.set(DataComponents.CUSTOM_NAME, ShootExpUtil.formatComponent(nameFormat));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(ShootExpUtil.formatComponent(
                ShootExpUtil.lang("shootexp.item.lore.1").replace("%AMOUNT%", String.valueOf(amount))));
        lore.add(ShootExpUtil.formatComponent(
                ShootExpUtil.lang("shootexp.item.lore.2").replace("%OWNER%", owner).replace("%RECIPIENT%", recipient)));
        lore.add(ShootExpUtil.formatComponent(
                ShootExpUtil.lang("shootexp.item.lore.3")));
        lore.add(ShootExpUtil.formatComponent(
                ShootExpUtil.lang("shootexp.item.lore.4")));
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
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(value));
        } catch (Exception e) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(value));
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

    public static String getRecipient(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        return customData.copyTag().getString("recipient");
    }

    public static int getAmount(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return 0;
        return customData.copyTag().getInt("amount");
    }
}
