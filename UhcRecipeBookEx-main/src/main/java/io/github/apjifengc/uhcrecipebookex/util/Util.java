package io.github.apjifengc.uhcrecipebookex.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * @author Milkory
 */
public class Util {

    public static ItemStack simplify(ItemStack is) {
        var clone = is.clone();
        var im = clone.getItemMeta();
        if (im == null) return clone;
        // ignore damage
        if (im instanceof Damageable) {
            ((Damageable) im).setDamage(0);
        }
        //ignore skull meta
        if (im instanceof SkullMeta) {
            ((SkullMeta) im).setOwningPlayer(null);
        }
        // ignore name
        im.setDisplayName(null);
        // ignore lore
        im.setLore(null);
        // ignore attributes
        if (im.hasAttributeModifiers()) {
            im.getAttributeModifiers().forEach((k, v) -> im.removeAttributeModifier(k));
        }
        clone.setItemMeta(im);
        // ignore enchantments
        clone.getEnchantments().forEach((k, v) -> clone.removeEnchantment(k));
        return clone;
    }

    public static boolean simpleCheckSimilar(ItemStack a, ItemStack b) {
        if (a == null) return b == null;
        if (b == null) return false;
        return simplify(a).isSimilar(simplify(b));
    }

    public static String getItemName(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta.hasDisplayName()) return meta.getDisplayName();
        else if (meta.hasLocalizedName()) return meta.getLocalizedName();
        else return "未知";
    }

}
