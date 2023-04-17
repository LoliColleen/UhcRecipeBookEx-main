package io.github.apjifengc.uhcrecipebookex.inventory;

import lombok.Getter;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class BackpackInventoryHolder implements InventoryHolder {
    @Getter
    private final ItemStack itemStack;

    public BackpackInventoryHolder(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void handleClick(Inventory inv, Player player){
        if (itemStack==null){
            player.closeInventory();
            return;
        }
        if (!(itemStack.getItemMeta() instanceof BlockStateMeta)){
            player.closeInventory();
            return;
        }
        saveContents(inv);
    }

    public void saveContents(Inventory inv){
        if (!(itemStack.getItemMeta() instanceof BlockStateMeta)){
            return;
        }

        if(itemStack.getItemMeta()!=null) {
            ShulkerBox shulkerBox = (ShulkerBox) ((BlockStateMeta) itemStack.getItemMeta()).getBlockState();
            shulkerBox.getInventory().setContents(inv.getContents());
        }

        BlockStateMeta im = (BlockStateMeta) itemStack.getItemMeta();
        ShulkerBox shulker = (ShulkerBox) im.getBlockState();

        //set all contents minus most recent item
        shulker.getInventory().setContents(inv.getContents());
        im.setBlockState(shulker);
        itemStack.setItemMeta(im);
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
