package io.github.apjifengc.uhcrecipebookex.inventory;

import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class BackpackInventory{

    public Inventory createInventory(ItemStack itemStack){
        if (itemStack.getItemMeta()!=null) {
            ShulkerBox shulkerBox = (ShulkerBox) ((BlockStateMeta) itemStack.getItemMeta()).getBlockState();
            Inventory gui = Bukkit.createInventory(new BackpackInventoryHolder(itemStack), 27,
                    itemStack.getItemMeta() == null ? "Backpack" : itemStack.getItemMeta().getDisplayName()
            );

            gui.setContents(shulkerBox.getInventory().getContents());
            return gui;
        }
        return null;
    }


}
