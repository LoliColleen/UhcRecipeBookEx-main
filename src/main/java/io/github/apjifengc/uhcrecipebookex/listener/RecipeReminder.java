package io.github.apjifengc.uhcrecipebookex.listener;

import com.alkaidmc.alkaid.message.AlkaidMessage;
import com.gmail.val59000mc.configuration.MainConfig;
import com.gmail.val59000mc.customitems.Craft;
import com.gmail.val59000mc.customitems.CraftsManager;
import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.players.PlayerManager;
import io.github.apjifengc.uhcrecipebookex.Config;
import io.github.apjifengc.uhcrecipebookex.UhcRecipeBookEx;
import io.github.apjifengc.uhcrecipebookex.util.Util;
import lombok.Getter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * @author Milkory
 */
public class RecipeReminder implements Listener {

    private static final UhcRecipeBookEx plugin = UhcRecipeBookEx.getInstance();
    private static final PlayerManager playerManager = GameManager.getGameManager().getPlayerManager();
    GameManager gm = GameManager.getGameManager();

    @Getter private final Map<Player, List<Craft>> cache = new HashMap<>();
    private final AlkaidMessage message = new AlkaidMessage();

    public RecipeReminder(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    void handle(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            getItem((Player) event.getEntity(), event.getItem().getItemStack());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    void handle(InventoryClickEvent event) {
        var item = event.getView().getItem(event.getRawSlot());
        if (event.getWhoClicked() instanceof Player) {
            if (item != null) {
                getItem((Player) event.getWhoClicked(), item);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    void handle(PlayerBucketFillEvent event) {
        if (event.getItemStack() != null) {
            getItem(event.getPlayer(), event.getItemStack());
        }
    }

    @EventHandler(ignoreCancelled = true)
    void handle(PlayerDropItemEvent event) {
        recheck(event.getPlayer());
    }

    public void getItem(Player player, ItemStack item) {
        new BukkitRunnable() {
            @Override public void run() {
                initFor(player);
                var reminds = new ArrayList<Craft>();
                var crafts = CraftsManager.getCrafts();
                for (Craft craft : crafts) {
                    if (!Config.IGNORE_CRAFTS.contains(craft.getName())) {
                        if (!cache.get(player).contains(craft) && canCraft(player, craft)) {
                            if (craft.getRecipe().stream().anyMatch(it -> Util.simpleCheckSimilar(it, item))) {
                                if (isEnough(player, craft)) {
                                    reminds.add(craft);
                                }
                            }
                        }
                    }
                }
                if (!reminds.isEmpty()) {

                    for (Craft remind : reminds) {
                        player.sendMessage(Config.RECIPE_REMIND_MESSAGE
                                .replaceAll("&","\u00a7")
                                .replace("{name}", ChatColor.stripColor(Util.getItemName(remind.getDisplayItem()))));

                        var builder = message.text()
                                .append(it -> it.text(Config.RECIPE_REMIND_MESSAGE_BUTTON.replaceAll("&","\u00a7"))
                                .hover(hover -> hover.text().gold(Config.BUTTON_HOVER_MESSAGE.replaceAll("&","\u00a7")))
                                .click(ClickEvent.Action.RUN_COMMAND, "/craft auto " + remind.getName()))
                                .text(Config.RECIPE_REMIND_MESSAGE_AFTER_BUTTON.replaceAll("&","\u00a7"));

                        var text = builder.pureComponents();

                        player.spigot().sendMessage(text);
                    }
                    var result = cache.get(player);
                    result.addAll(reminds);
                    cache.put(player, result);
                }
            }
        }.runTask(plugin);
    }

    public void recheck(Player player) {
        new BukkitRunnable() {
            @Override public void run() {
                initFor(player);
                var removal = new ArrayList<Craft>();
                var crafts = CraftsManager.getCrafts();
                for (Craft craft : crafts) {
                    if (!Config.IGNORE_CRAFTS.contains(craft.getName())) {
                        if (cache.get(player).contains(craft) && !isEnough(player, craft)) {
                            removal.add(craft);
                        }
                    }
                }
                if (!removal.isEmpty()) {
                    var result = cache.get(player);
                    result.removeAll(removal);
                    cache.put(player, result);
                }
            }
        }.runTask(plugin);
    }

    private static int getItemAmount(Player player, ItemStack is) {
        var amount = 0;
        for (ItemStack item : player.getInventory()) {
            if (Util.simpleCheckSimilar(is, item) && (item.getItemMeta()==null || !(item.getItemMeta().getLore()!=null&&item.getItemMeta().getLore().get(0).contains("\u00a78")))) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    private static Map<ItemStack, Integer> getRequirement(Craft craft) {
        var map = new HashMap<ItemStack, Integer>();
        for (ItemStack is : craft.getRecipe()) {
            if (is == null || is.getType() == Material.AIR) continue;
            var key = map.keySet().stream().filter(it -> Util.simpleCheckSimilar(it, is)).findAny();
            key.ifPresentOrElse(it -> map.put(it, map.get(it) + is.getAmount()), () -> map.put(is, is.getAmount()));
        }
        return map;
    }

    public static boolean isEnough(Player player, Craft craft) {
        return getRequirement(craft).entrySet().stream().allMatch(it -> getItemAmount(player, it.getKey()) >= it.getValue());
    }

    private boolean canCraft(Player player, Craft craft) {
        ItemStack itemStack = craft.getCraft();
        if (gm.getConfig().get(MainConfig.ENABLE_CRAFTS_PERMISSIONS) && itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null){
            String permission = "uhc-core.craft." + itemStack.getItemMeta().getLore().get(0).toLowerCase().replaceAll(" ", "-").replaceAll("\\u00fa", "u").replaceAll("\\u00a78", "").replaceAll("&8", "");
            if (!player.hasPermission(permission)) return false;
        }
        return !craft.hasLimit() || UhcRecipeBookEx.getPlayerListener().getCraftedTimes(player, craft) < craft.getLimit();
    }

    private void initFor(Player player) {
        if (!cache.containsKey(player)) {
            cache.put(player, new ArrayList<>());
        }
    }

}
