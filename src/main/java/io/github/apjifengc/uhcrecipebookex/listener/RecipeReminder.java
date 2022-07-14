package io.github.apjifengc.uhcrecipebookex.listener;

import com.alkaidmc.alkaid.message.AlkaidMessage;
import com.alkaidmc.alkaid.message.text.Format;
import com.gmail.val59000mc.customitems.Craft;
import com.gmail.val59000mc.customitems.CraftsManager;
import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.players.PlayerManager;
import io.github.apjifengc.uhcrecipebookex.Config;
import io.github.apjifengc.uhcrecipebookex.UhcRecipeBookEx;
import io.github.apjifengc.uhcrecipebookex.util.Util;
import lombok.Getter;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
                    var builder = message.text().yellow(Config.RECIPE_REMIND_MESSAGE);
                    for (Craft remind : reminds) {
                        builder.append(it -> it.green("[" + Util.getItemName(remind.getDisplayItem()) + "]")
                                        .hover(hover -> hover.text().green(Config.BUTTON_HOVER_MESSAGE))
                                        .click(ClickEvent.Action.RUN_COMMAND, "/craft auto " + remind.getName()))
                                .text(", ");
                    }
                    var text = builder.pureComponents();
                    player.spigot().sendMessage(Arrays.copyOf(text, text.length - 1));
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

    private int getItemAmount(Player player, ItemStack is) {
        var amount = 0;
        for (ItemStack item : player.getInventory()) {
            if (Util.simpleCheckSimilar(is, item)) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    private Map<ItemStack, Integer> getRequirement(Craft craft) {
        var map = new HashMap<ItemStack, Integer>();
        for (ItemStack is : craft.getRecipe()) {
            if (is == null || is.getType() == Material.AIR) continue;
            var key = map.keySet().stream().filter(it -> Util.simpleCheckSimilar(it, is)).findAny();
            key.ifPresentOrElse(it -> map.put(it, map.get(it) + is.getAmount()), () -> map.put(is, is.getAmount()));
        }
        return map;
    }

    private boolean isEnough(Player player, Craft craft) {
        return getRequirement(craft).entrySet().stream().allMatch(it -> getItemAmount(player, it.getKey()) >= it.getValue());
    }

    private boolean canCraft(Player player, Craft craft) {
        return !craft.hasLimit() || UhcRecipeBookEx.getPlayerListener().getCraftedTimes(player, craft) < craft.getLimit();
    }

    private void initFor(Player player) {
        if (!cache.containsKey(player)) {
            cache.put(player, new ArrayList<>());
        }
    }

}
