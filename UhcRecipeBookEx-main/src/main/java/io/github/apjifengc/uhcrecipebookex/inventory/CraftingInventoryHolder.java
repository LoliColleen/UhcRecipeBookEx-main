package io.github.apjifengc.uhcrecipebookex.inventory;

import com.gmail.val59000mc.customitems.Craft;
import io.github.apjifengc.uhcrecipebookex.Config;
import io.github.apjifengc.uhcrecipebookex.UhcRecipeBookEx;
import io.github.apjifengc.uhcrecipebookex.inventory.item.InventoryItem;
import io.github.apjifengc.uhcrecipebookex.inventory.item.RecipeSlotItem;
import io.github.apjifengc.uhcrecipebookex.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class CraftingInventoryHolder implements InventoryHolder {

    private static final CraftRecipeInventory recipeInventory = UhcRecipeBookEx.getRecipeInventory();

    @Override
    public Inventory getInventory() {
        return null;
    }

    public static void autoOpen(Player player, Craft craft) {
        var inv = recipeInventory.createCraftingInventory();
        var recipe = craft.getRecipe();
        for (int i = 0; i < Config.CRAFTING_PATTERN.size(); i++) {
            for (int j = 0; j < 9; j++) {
                InventoryItem invItem = recipeInventory.getInventoryItem(Config.CRAFTING_PATTERN, i, j);
                if (invItem instanceof RecipeSlotItem) {
                    int slot = ((RecipeSlotItem) invItem).getSlot();
                    if (slot != 0) {
                        var item = recipe.get(slot - 1);
                        inv.setItem(i * 9 + j, robItem(player, item));
                    }
                }
            }
        }
        player.openInventory(inv);
        UhcRecipeBookEx.getPlayerListener().updateInventory(player, inv);
    }

    private static ItemStack robItem(Player player, ItemStack target) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            var item = inv.getItem(i);
            if (item != null) {
                if (Util.simpleCheckSimilar(target, item)) {
                    if (item.getAmount() == 1) {
                        inv.setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - 1);
                        item = item.clone();
                        item.setAmount(1);
                    }
                    return item;
                }
            }
        }
        return null;
    }

}
