package io.github.apjifengc.uhcrecipebookex;

import com.gmail.val59000mc.configuration.MainConfig;
import com.gmail.val59000mc.customitems.Craft;
import com.gmail.val59000mc.customitems.CraftsManager;
import com.gmail.val59000mc.languages.Lang;
import io.github.apjifengc.uhcrecipebookex.inventory.CraftingInventoryHolder;
import io.github.apjifengc.uhcrecipebookex.listener.RecipeReminder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CraftsCommandExecutor implements CommandExecutor {
    private final Map<UUID, Craft.Builder> craftCreators;

    public CraftsCommandExecutor() {
        craftCreators = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;


        if (args.length == 0) {
            if (!CraftsManager.isAtLeastOneCraft()) {
                // no crafts
                player.sendMessage(Lang.COMMAND_RECIPES_ERROR);
                return true;
            }

            player.openInventory(UhcRecipeBookEx.getRecipeInventory().createMainInventory(0));
            return true;
        }

        if (args[0].equalsIgnoreCase("auto")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: /craft auto <name>");
                return true;
            }

            UhcRecipeBookEx.getRecipeInventory().getCrafts().stream().filter(it -> it.getName().equals(args[1])).findAny()
                    .ifPresent(it -> CraftingInventoryHolder.autoOpen(player, it));
            return true;
        }

        if (!player.hasPermission("uhc-core.commands.craft.create")) {
            player.sendMessage(ChatColor.RED + "Your not allowed to create recipes!");
            return true;
        }

        if (args[0].equalsIgnoreCase("name")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: /craft name <name>");
                return true;
            }

            getCraftCreator(player).setCraftName(args[1]);
            player.sendMessage(ChatColor.GREEN + "Changed craft name to: " + args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("item")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: '/craft item <item-number (1-9)>' (While holding the item)");
                return true;
            }

            int number;
            ItemStack item = player.getItemInHand();

            try {
                number = Integer.parseInt(args[1]);
                if (number < 1 || number > 9) throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Usage: '/craft item <item-number (1-9)>' (While holding the item)");
                return true;
            }

            getCraftCreator(player).setRecipeItem(number - 1, item);
            player.sendMessage(ChatColor.GREEN + "Set recipe item " + number + " to " + item.getType());
            return true;
        }

        if (args[0].equalsIgnoreCase("craft")) {
            getCraftCreator(player).setCraft(player.getItemInHand());
            player.sendMessage(ChatColor.GREEN + "Set craft item to " + player.getItemInHand().getType());
            return true;
        }

        // limit
        if (args[0].equalsIgnoreCase("limit")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: '/craft limit <amount>'");
                return true;
            }

            int number;

            try {
                number = Integer.parseInt(args[1]);
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Usage: '/craft limit <amount>'");
                return true;
            }

            getCraftCreator(player).setCraftLimit(number);
            player.sendMessage(ChatColor.GREEN + "Set craft limit to " + number);
            return true;
        }

        // default name
        if (args[0].equalsIgnoreCase("default-name")) {
            if (args.length != 2) {
                player.sendMessage(ChatColor.RED + "Usage: '/craft default-name <true | false>'");
                return true;
            }

            boolean value;

            try {
                value = Boolean.parseBoolean(args[1]);
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Usage: '/craft default-name <true | false>'");
                return true;
            }

            getCraftCreator(player).useDefaultName(value);
            player.sendMessage(ChatColor.GREEN + "Set craft default-name to " + value);
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            Craft craft;

            try {
                craft = getCraftCreator(player).create();
                craftCreators.remove(player.getUniqueId());
            } catch (IllegalArgumentException ex) {
                player.sendMessage(ChatColor.RED + "Error while creating: " + ex.getMessage());
                return true;
            }

            CraftsManager.getCrafts().add(craft);

            CraftsManager.saveCraft(craft);

            CraftsManager.openCraftInventory(player, craft);
            return true;
        }

        player.sendMessage(ChatColor.DARK_GREEN + "[UhcRecipeBookEx] Craft creation commands:");
        player.sendMessage(ChatColor.GREEN + " - '/craft name <name>' (To change the craft name)");
        player.sendMessage(ChatColor.GREEN + " - '/craft item <item-number (1-9)>' (While holding the item you want in your recipe)");
        player.sendMessage(ChatColor.GREEN + " - '/craft craft' (While holding the item you want as craft)");
        player.sendMessage(ChatColor.GREEN + " - '/craft limit <amount>' (To set the maximum times someone can craft a item)");
        player.sendMessage(ChatColor.GREEN + " - '/craft default-name <true | false>' (Set to true to leave the crafted item name as default)");
        player.sendMessage(ChatColor.GREEN + " - '/craft create' (Creates the craft)");
        return true;
    }

    private Craft.Builder getCraftCreator(Player player) {
        if (!craftCreators.containsKey(player.getUniqueId())) {
            craftCreators.put(player.getUniqueId(), new Craft.Builder());
        }
        return craftCreators.get(player.getUniqueId());
    }

}