package io.github.apjifengc.uhcrecipebookex.listener;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.configuration.MainConfig;
import com.gmail.val59000mc.customitems.*;
import com.gmail.val59000mc.exceptions.UhcTeamException;
import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.game.GameState;
import com.gmail.val59000mc.game.handlers.ShulkerInventoryHandler;
import com.gmail.val59000mc.languages.Lang;
import com.gmail.val59000mc.listeners.ItemsListener;
import com.gmail.val59000mc.players.PlayerManager;
import com.gmail.val59000mc.players.PlayerState;
import com.gmail.val59000mc.players.UhcPlayer;
import com.gmail.val59000mc.players.UhcTeam;
import com.gmail.val59000mc.threads.CheckArmorThread;
import com.gmail.val59000mc.utils.TimeUtils;
import com.gmail.val59000mc.utils.UniversalMaterial;
import io.github.apjifengc.uhcrecipebookex.Config;
import io.github.apjifengc.uhcrecipebookex.UhcRecipeBookEx;
import io.github.apjifengc.uhcrecipebookex.inventory.*;
import io.github.apjifengc.uhcrecipebookex.inventory.item.*;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static org.bukkit.Bukkit.broadcastMessage;

public class PlayerListener implements Listener {
    private final UhcRecipeBookEx plugin = UhcRecipeBookEx.getInstance();
    private final CraftRecipeInventory recipe = UhcRecipeBookEx.getRecipeInventory();
    private final Map<UhcPlayer, Map<ItemStack, Integer>> craftedItems = new HashMap<>();
    private final PlayerManager playerManager;
    GameManager gm = GameManager.getGameManager();
    public static final ItemStack BARRIER = new ItemStack(Material.BARRIER);
    public static final ItemStack AIR = new ItemStack(Material.AIR);

    Material[] itemStacks;
    public static Map<Long, Craft> craftMap = new HashMap<>();
    public static Map<Long, Craft> craftNeed = new HashMap<>();

    private final Map<Player,Long> modularUsingLastUpdate = new HashMap<>();

    static {
        ItemMeta meta = BARRIER.getItemMeta();
        meta.setDisplayName("\u00A7a");
        BARRIER.setItemMeta(meta);
    }

    public PlayerListener(PlayerManager playerManager) {
        assert playerManager != null;
        this.playerManager = playerManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /*@EventHandler
    public void onPlayerPickUpItem(EntityPickupItemEvent event){
        if (event.getEntity() instanceof Player){
            return;
        }

        Player player = (Player) event.getEntity();
        ItemStack[] itemStacks = new ItemStack[player.getInventory().getSize()];

        for(int i = 0; i < player.getInventory().getSize(); i++){
            itemStacks[i] = player.getInventory().getItem(i);
        }

        craftNeed = CraftsManager.getCraftNeed();

        if (craftNeed.containsKey(recipeNeedHashCode(itemStacks))){
            Craft craft = craftMap.get(shapelessHashCode(itemStacks));

            broadcastMessage(craft.getName());

        }
    }*/

    @EventHandler
    public void onLeftClickItem(PlayerInteractEvent event) {
        if (
                event.getAction() != Action.LEFT_CLICK_AIR &&
                        event.getAction() != Action.LEFT_CLICK_BLOCK
        ) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (UhcItems.isModularBowPunchItem(hand)
                ||UhcItems.isModularBowLightningItem(hand)
                ||UhcItems.isModularBowPoisonItem(hand)){
            handleModularSwitch(10, hand, player);
        }
    }

    public void handleModularSwitch(int cooldown, ItemStack hand, Player player){
        // Check cooldown
        if (cooldown != -1 && (cooldown*TimeUtils.SECOND_TICKS) + modularUsingLastUpdate.getOrDefault(player,-1L) > System.currentTimeMillis()){
            return;
        }

        modularUsingLastUpdate.put(player,System.currentTimeMillis());

        player.playSound(player.getLocation(),Sound.ENTITY_ARROW_HIT_PLAYER,1,1);
        ItemMeta itemMeta = hand.getItemMeta();

        if(UhcItems.isModularBowPunchItem(hand)){
            player.sendMessage(Lang.ITEMS_MODULAR_BOW_TO_POISON_1);
            player.sendMessage(Lang.ITEMS_MODULAR_BOW_TO_POISON_2);
            itemMeta.removeEnchant(Enchantment.ARROW_KNOCKBACK);
            itemMeta.setLore(List.of(Lang.ITEMS_MODULAR_BOW_POISON
                    ,Lang.ITEMS_MODULAR_BOW_LORE_1
                    ,Lang.ITEMS_MODULAR_BOW_LORE_2
                    ,Lang.ITEMS_MODULAR_BOW_LORE_3
                    ,Lang.ITEMS_MODULAR_BOW_LORE_4));
            hand.setItemMeta(itemMeta);
            return;
        }

        if(UhcItems.isModularBowLightningItem(hand)){
            player.sendMessage(Lang.ITEMS_MODULAR_BOW_TO_PUNCH_1);
            player.sendMessage(Lang.ITEMS_MODULAR_BOW_TO_PUNCH_2);
            itemMeta.addEnchant(Enchantment.ARROW_KNOCKBACK,1,true);
            itemMeta.setLore(List.of(Lang.ITEMS_MODULAR_BOW_PUNCH
                    ,Lang.ITEMS_MODULAR_BOW_LORE_1
                    ,Lang.ITEMS_MODULAR_BOW_LORE_2
                    ,Lang.ITEMS_MODULAR_BOW_LORE_3
                    ,Lang.ITEMS_MODULAR_BOW_LORE_4));
            hand.setItemMeta(itemMeta);
            return;
        }

        if(UhcItems.isModularBowPoisonItem(hand)){
            player.sendMessage(Lang.ITEMS_MODULAR_BOW_TO_LIGHTNING_1);
            player.sendMessage(Lang.ITEMS_MODULAR_BOW_TO_LIGHTNING_2);
            itemMeta.removeEnchant(Enchantment.ARROW_KNOCKBACK);
            itemMeta.setLore(List.of(Lang.ITEMS_MODULAR_BOW_LIGHTNING
                    ,Lang.ITEMS_MODULAR_BOW_LORE_1
                    ,Lang.ITEMS_MODULAR_BOW_LORE_2
                    ,Lang.ITEMS_MODULAR_BOW_LORE_3
                    ,Lang.ITEMS_MODULAR_BOW_LORE_4));
            hand.setItemMeta(itemMeta);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent event) {
        if (
                event.getAction() != Action.RIGHT_CLICK_AIR &&
                        event.getAction() != Action.RIGHT_CLICK_BLOCK
        ) {
            return;
        }

        Player player = event.getPlayer();
        UhcPlayer uhcPlayer = GameManager.getGameManager().getPlayerManager().getUhcPlayer(player);
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (Tag.SHULKER_BOXES.isTagged(hand.getType()) && player.isSneaking()) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, 1, 1);
            player.openInventory(ShulkerInventoryHandler.createShulkerBoxInventory(player, player.getInventory().getItemInMainHand()));
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(UhcCore.getPlugin(), new CheckArmorThread(player), 1);

        if (GameItem.isGameItem(hand)) {
            event.setCancelled(true);
            GameItem gameItem = GameItem.getGameItem(hand);
            handleGameItemInteract(gameItem, player, uhcPlayer, hand);
            return;
        }

        GameState state = GameManager.getGameManager().getGameState();
        if ((state == GameState.PLAYING || state == GameState.DEATHMATCH)
                && UhcItems.isRegenHeadItem(hand)
                && uhcPlayer.getState().equals(PlayerState.PLAYING)
                && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)
        ) {
            event.setCancelled(true);
            uhcPlayer.getTeam().regenTeam(false, 1, uhcPlayer);
        }

        if ((state == GameState.PLAYING || state == GameState.DEATHMATCH)
                && UhcItems.isGoldenHeadItem(hand)
                && uhcPlayer.getState().equals(PlayerState.PLAYING)
                && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)
        ) {
            event.setCancelled(true);
            uhcPlayer.getTeam().regenTeamGold(false, 1, uhcPlayer);
        }

        if ((state == GameState.PLAYING || state == GameState.DEATHMATCH)
                && UhcItems.isCornItem(hand)
                && uhcPlayer.getState().equals(PlayerState.PLAYING)
                && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)
        ) {
            event.setCancelled(true);
            uhcPlayer.regenPlayerCorn(1,uhcPlayer);
        }

        if ((state == GameState.PLAYING || state == GameState.DEATHMATCH)
                && UhcItems.isMasterCompassItem(hand)
                && uhcPlayer.getState().equals(PlayerState.PLAYING)
                && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)
        ) {
            event.setCancelled(true);
            //if (hand.getAmount() > 0) {
            //	hand.setAmount(hand.getAmount() - 1);
            //}
            uhcPlayer.pointMasterCompassToPlayer(1,uhcPlayer);
        }
    }

    private void handleGameItemInteract(GameItem gameItem, Player player, UhcPlayer uhcPlayer, ItemStack item) {
        switch (gameItem) {
            case TEAM_SELECTION:
                UhcItems.openTeamMainInventory(player, uhcPlayer);
                break;
            case TEAM_SETTINGS:
                UhcItems.openTeamSettingsInventory(player);
                break;
            case KIT_SELECTION:
                KitsManager.openKitSelectionInventory(player);
                break;
            case CUSTOM_CRAFT_BOOK:
                // Custom craft book
                player.openInventory(UhcRecipeBookEx.getRecipeInventory().createMainInventory(0));
                break;
            case TEAM_COLOR_SELECTION:
                UhcItems.openTeamColorInventory(player);
                break;
            case TEAM_RENAME:
                openTeamRenameGUI(player, uhcPlayer.getTeam());
                break;
            case SCENARIO_VIEWER:
                Inventory inv;
                if (GameManager.getGameManager().getConfig().get(MainConfig.ENABLE_SCENARIO_VOTING)) {
                    inv = GameManager.getGameManager().getScenarioManager().getScenarioVoteInventory(uhcPlayer);
                } else {
                    inv = GameManager.getGameManager().getScenarioManager().getScenarioMainInventory(player.hasPermission("uhc-core.scenarios.edit"));
                }
                player.openInventory(inv);
                break;
            case BUNGEE_ITEM:
                GameManager.getGameManager().getPlayerManager().sendPlayerToBungeeServer(player);
                break;
            case COMPASS_ITEM:
                uhcPlayer.pointCompassToNextPlayer(GameManager.getGameManager().getConfig().get(MainConfig.PLAYING_COMPASS_MODE), GameManager.getGameManager().getConfig().get(MainConfig.PLAYING_COMPASS_COOLDOWN),uhcPlayer);
                break;
            case TEAM_READY:
            case TEAM_NOT_READY:
                uhcPlayer.getTeam().changeReadyState();
                UhcItems.openTeamSettingsInventory(player);
                break;
            case TEAM_INVITE_PLAYER:
                UhcItems.openTeamInviteInventory(player);
                break;
            case TEAM_INVITE_PLAYER_SEARCH:
                openTeamInviteGUI(player);
                break;
            case TEAM_VIEW_INVITES:
                UhcItems.openTeamInvitesInventory(player, uhcPlayer);
                break;
            case TEAM_INVITE_ACCEPT:
                handleTeamInviteReply(uhcPlayer, item, true);
                player.closeInventory();
                break;
            case TEAM_INVITE_DENY:
                handleTeamInviteReply(uhcPlayer, item, false);
                player.closeInventory();
                break;
            case TEAM_LEAVE:
                try {
                    uhcPlayer.getTeam().leave(uhcPlayer);

                    // Update player tab
                    GameManager.getGameManager().getScoreboardManager().updatePlayerOnTab(uhcPlayer);
                } catch (UhcTeamException ex) {
                    uhcPlayer.sendMessage(ex.getMessage());
                }
                break;
            case TEAM_LIST:
                UhcItems.openTeamsListInventory(player);
                break;
        }
    }

    private void handleTeamInviteReply(UhcPlayer uhcPlayer, ItemStack item, boolean accepted) {
        if (!item.hasItemMeta()) {
            uhcPlayer.sendMessage("Something went wrong!");
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (!meta.hasLore()) {
            uhcPlayer.sendMessage("Something went wrong!");
            return;
        }

        if (meta.getLore().size() != 2) {
            uhcPlayer.sendMessage("Something went wrong!");
            return;
        }

        String line = meta.getLore().get(1).replace(ChatColor.DARK_GRAY.toString(), "");
        UhcTeam team = GameManager.getGameManager().getTeamManager().getTeamByName(line);

        if (team == null) {
            uhcPlayer.sendMessage(Lang.TEAM_MESSAGE_NO_LONGER_EXISTS);
            return;
        }

        GameManager.getGameManager().getTeamManager().replyToTeamInvite(uhcPlayer, team, accepted);
    }

    private void openTeamRenameGUI(Player player, UhcTeam team) {
        new AnvilGUI.Builder()
                .plugin(UhcCore.getPlugin())
                .title(Lang.TEAM_INVENTORY_RENAME)
                .text(team.getTeamName())
                .item(new ItemStack(Material.NAME_TAG))
                .onComplete(((p, s) -> {
                    if (GameManager.getGameManager().getTeamManager().isValidTeamName(s)) {
                        team.setTeamName(s);
                        p.sendMessage(Lang.TEAM_MESSAGE_NAME_CHANGED);
                        return AnvilGUI.Response.close();
                    } else {
                        p.sendMessage(Lang.TEAM_MESSAGE_NAME_CHANGED_ERROR);
                        return AnvilGUI.Response.close();
                    }
                }))
                .open(player);
    }

    private void openTeamInviteGUI(Player player) {
        new AnvilGUI.Builder()
                .plugin(UhcCore.getPlugin())
                .title(Lang.TEAM_INVENTORY_INVITE_PLAYER)
                .text("Enter name ...")
                .item(new ItemStack(Material.NAME_TAG))
                .onComplete(((p, s) -> {
                    p.performCommand("team invite " + s);
                    return AnvilGUI.Response.close();
                }))
                .open(player);
    }

    @EventHandler
    void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CraftRecipeInventoryHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                CraftRecipeInventory recipe = UhcRecipeBookEx.getRecipeInventory();
                CraftRecipeInventoryHolder holder = (CraftRecipeInventoryHolder) event.getView().getTopInventory().getHolder();
                if (recipe.getSlotId().containsKey(event.getSlot())) {
                    int craftId = holder.getPage() * recipe.getSlots().size()
                            + recipe.getSlotId().get(event.getSlot());
                    Craft craft = recipe.getCrafts().get(craftId);
                    event.getWhoClicked().openInventory(recipe.createRecipeViewerInventory(craft, event.getClickedInventory()));
                } else if (recipe.getInventoryItem(Config.INVENTORY_PATTERN, event.getSlot()) instanceof PreviousPageItem) {
                    if (holder.getPage() != CraftRecipeInventory.getFirstPage()) {
                        event.getWhoClicked().openInventory(recipe.createMainInventory(holder.getPage() - 1));
                    }
                } else if (recipe.getInventoryItem(Config.INVENTORY_PATTERN, event.getSlot()) instanceof NextPageItem) {
                    if (holder.getPage() != CraftRecipeInventory.getLastPage()) {
                        event.getWhoClicked().openInventory(recipe.createMainInventory(holder.getPage() + 1));
                    }
                } else if (recipe.getInventoryItem(Config.INVENTORY_PATTERN, event.getSlot()) instanceof CloseInventoryItem) {
                    event.getWhoClicked().closeInventory();
                }
            }
        }
        if (event.getView().getTopInventory().getHolder() instanceof CraftRecipeViewerInventoryHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                CraftRecipeInventory recipe = UhcRecipeBookEx.getRecipeInventory();
                CraftRecipeViewerInventoryHolder holder = (CraftRecipeViewerInventoryHolder) event.getView().getTopInventory().getHolder();
                if (recipe.getInventoryItem(Config.RECIPE_VIEWER_PATTERN, event.getSlot()) instanceof GoBackItem) {
                    event.getWhoClicked().closeInventory();
                    event.getWhoClicked().openInventory(holder.getLastInventory());
                }
            }
        }
        if (event.getView().getTopInventory().getHolder() instanceof CraftingInventoryHolder) {
            Inventory inventory = event.getClickedInventory();
            Player player = (Player) event.getWhoClicked();
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateInventory(player, event.getView().getTopInventory());
                }
            }.runTaskLater(plugin, 1);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                InventoryItem item = recipe.getInventoryItem(Config.CRAFTING_PATTERN, event.getSlot());
                if (item instanceof RecipeSlotItem) {
                    int slot = ((RecipeSlotItem) item).getSlot();
                    if (slot == 0) {
                        event.setCancelled(true);
                        Optional<CraftRecipe> craftOptional = getCurrentCraft(inventory, player);
                        if (craftOptional.isEmpty()) {
                            //broadcastMessage("(java.400)");
                            return;
                        }
                        CraftRecipe craft = craftOptional.get();
                        ItemStack itemStack = craft.getCraft();
                        if(itemStack == null || itemStack.getType().equals(Material.AIR)){
                            //broadcastMessage("(java.406)");
                            return;
                        }
                        if (gm.getConfig().get(MainConfig.ENABLE_CRAFTS_PERMISSIONS) && itemStack.getItemMeta() != null && itemStack.getItemMeta().getLore() != null) {
                            String permission = "uhc-core.craft." + itemStack.getItemMeta().getLore().get(0).toLowerCase().replaceAll(" ", "-").replaceAll("\\u00fa", "u").replaceAll("\\u00a78", "").replaceAll("&8", "");
                            if (!player.hasPermission(permission)) {
                                player.sendMessage(Lang.ITEMS_CRAFT_NO_PERMISSION.replace("%craft%", ChatColor.translateAlternateColorCodes('&', itemStack.getItemMeta().getDisplayName())));
                                event.setCancelled(true);
                                return;
                            }
                        }
                        //broadcastMessage("passed");
                        if (event.isShiftClick()) {
                            ItemStack addedItems = itemStack.clone();
                            //broadcastMessage(addedItems.toString());
                            if(addedItems.getItemMeta()!=null&&addedItems.getItemMeta().getLore()!=null&&addedItems.getItemMeta().getLore().contains(Lang.ITEMS_FUSION_ARMOR)){
                                addedItems = UhcItems.createFusionArmor();
                            }
                            //broadcastMessage("pass 1  " + addedItems.toString());
                            if(addedItems.getItemMeta()!=null&&addedItems.getItemMeta().getLore()!=null&&addedItems.getItemMeta().getLore().contains(Lang.ITEMS_DEUS_EX_MACHINA)){
                                player.setHealth(player.getHealth()/2);
                            }
                            //broadcastMessage("pass 2  " + addedItems.toString());
                            if(craft.getRealCraft()!=null && craft.getRealCraft().isUnbreakable()){
                                ItemMeta meta = addedItems.getItemMeta();
                                meta.setUnbreakable(true);
                                addedItems.setItemMeta(meta);
                            }
                            //broadcastMessage("pass 3  " + addedItems.toString());
                            int addedItemCount = ((int) Math.floor(
                                    (double) Math.min(getAddableItemCount(event.getView().getBottomInventory(), addedItems),
                                            itemStack.getAmount() * getMaximumCrafts(inventory)) / itemStack.getAmount())
                            );
                            //broadcastMessage("pass 4  "+addedItems.toString()+addedItemCount);
                            if (craft.getRealCraft()!=null && craft.hasLimit()) {
                                addedItemCount = Math.min(addedItemCount, craft.getLimit() - getCraftedTimes(player, craft.getRealCraft()));
                            }
                            if (itemStack.getType().getMaxStackSize()==1){
                                for(int i = 0 ; i < addedItemCount ; i++){
                                    event.getWhoClicked().getInventory().addItem(addedItems);
                                }
                            }else{
                                addedItems.setAmount(addedItemCount * itemStack.getAmount());
                                event.getWhoClicked().getInventory().addItem(addedItems);
                            }
                            reduce(inventory, addedItemCount);
                            addCraftedTimes(player, craft.getRealCraft(), addedItemCount);
                            showLimitMessage(player, craft);
                        } else {
                            if (craft.hasLimit() && getCraftedTimes(player, craft.getRealCraft()) == craft.getLimit()) {
                                //broadcastMessage("(java.452)");
                                return;
                            }
                            ItemStack cursor = event.getCursor();
                            if (cursor == null || cursor.getType() == Material.AIR || itemStack.isSimilar(cursor)) {
                                int amount;
                                if (cursor == null) {
                                    amount = itemStack.getAmount();
                                } else {
                                    amount = cursor.getAmount() + itemStack.getAmount();
                                }
                                if (amount <= itemStack.getType().getMaxStackSize()) {
                                    ItemStack newStack = itemStack.clone();
                                    if(newStack.getItemMeta()!=null&&newStack.getItemMeta().getLore()!=null&&newStack.getItemMeta().getLore().contains(Lang.ITEMS_FUSION_ARMOR)){
                                        newStack = UhcItems.createFusionArmor();
                                    }
                                    if(newStack.getItemMeta()!=null&&newStack.getItemMeta().getLore()!=null&&newStack.getItemMeta().getLore().contains(Lang.ITEMS_DEUS_EX_MACHINA)){
                                        player.setHealth(player.getHealth()/2);
                                    }
                                    if(craft.getRealCraft()!=null && craft.getRealCraft().isUnbreakable()){
                                        ItemMeta meta = newStack.getItemMeta();
                                        meta.setUnbreakable(true);
                                        newStack.setItemMeta(meta);
                                    }
                                    newStack.setAmount(amount);
                                    event.getView().setCursor(newStack);
                                    reduce(inventory, 1);
                                    addCraftedTimes(player, craft.getRealCraft(), 1);
                                    showLimitMessage(player, craft);
                                }
                            }
                        }
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }

        /*if(event.getInventory().getType().equals(InventoryType.BREWING) && config.get(MainConfig.BAN_LEVEL_TWO_POTIONS)){
            final BrewerInventory inv = (BrewerInventory) event.getInventory();
            final HumanEntity human = event.getWhoClicked();
            Bukkit.getScheduler().runTaskLater(plugin, new CheckBrewingStandAfterClick(inv.getHolder(), human),1);
        }*/
    }

    @EventHandler
    void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CraftRecipeViewerInventoryHolder ||
                event.getInventory().getHolder() instanceof CraftRecipeInventoryHolder) {
            event.setCancelled(true);
        }
        if (event.getInventory().getHolder() instanceof CraftingInventoryHolder) {
            Inventory inventory = event.getView().getTopInventory();
            for (int slot : event.getRawSlots()) {
                InventoryItem item = recipe.getInventoryItem(Config.CRAFTING_PATTERN, slot);
                if (item instanceof RecipeSlotItem && ((RecipeSlotItem) item).getSlot() == 0) {
                    event.setCancelled(true);
                }
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateInventory((Player) event.getWhoClicked(), inventory);
                }
            }.runTaskLater(plugin, 1);
        }
    }

    @EventHandler
    void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.WORKBENCH) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    event.getPlayer().openInventory(recipe.createCraftingInventory());
                }
            }.runTaskLater(plugin, 1);
            event.setCancelled(true);
        }
    }

    @EventHandler
    void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof CraftingInventoryHolder) {
            for (int i = 0; i < Config.CRAFTING_PATTERN.size(); i++) {
                for (int j = 0; j < 9; j++) {
                    InventoryItem item = recipe.getInventoryItem(Config.CRAFTING_PATTERN, i, j);
                    if (item instanceof RecipeSlotItem && ((RecipeSlotItem) item).getSlot() != 0) {
                        if (event.getInventory().getItem(i * 9 + j) != null) {
                            Map<Integer, ItemStack> map = event.getPlayer().getInventory().addItem(event.getInventory().getItem(i * 9 + j));
                            for (Map.Entry<Integer, ItemStack> entry : map.entrySet()) {
                                event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), entry.getValue());
                            }
                        }
                    }
                }
            }
        }
    }

    int getCraftedTimes(Player player, Craft craft) {

        UhcPlayer uhcPlayer = playerManager.getUhcPlayer(player);

        return craftedItems.getOrDefault(uhcPlayer, new HashMap<>()).getOrDefault(craft.getCraft(), 0);
    }

    void addCraftedTimes(Player player, Craft craft, int amount) {
        UhcPlayer uhcPlayer = playerManager.getUhcPlayer(player);
        if (craft.getCraft() == null) {
            return;
        }
        if (!craftedItems.containsKey(uhcPlayer)) {
            craftedItems.put(uhcPlayer, new HashMap<>());
        }
        Map<ItemStack, Integer> map = craftedItems.get(uhcPlayer);
        if (!map.containsKey(craft.getCraft())) {
            map.put(craft.getCraft(), amount);
        } else {
            map.put(craft.getCraft(), map.get(craft.getCraft()) + amount);
        }
    }

    void reduce(Inventory inventory, int amount) {
        for (int i = 0; i < Config.CRAFTING_PATTERN.size(); i++) {
            for (int j = 0; j < 9; j++) {
                InventoryItem item = recipe.getInventoryItem(Config.CRAFTING_PATTERN, i, j);
                if (item instanceof RecipeSlotItem && ((RecipeSlotItem) item).getSlot() != 0) {
                    ItemStack itemStack = inventory.getItem(i * 9 + j);
                    if (itemStack != null) {
                        if (itemStack.getType().toString().contains("BUCKET")) {
                            itemStack = new ItemStack(Material.BUCKET);
                        } else {
                            itemStack.setAmount(itemStack.getAmount() - amount);
                        }
                    }
                    inventory.setItem(i * 9 + j, itemStack);
                }
            }
        }
    }

    int getAddableItemCount(Inventory inventory, ItemStack itemStack) {
        int count = 0;
        for (int i = 0; i <= 35; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) {
                count += itemStack.getType().getMaxStackSize();
            } else if (itemStack.isSimilar(stack)) {
                count += itemStack.getType().getMaxStackSize() - stack.getAmount();
            }
        }
        return count;
    }

    void updateInventory(Player player, Inventory inventory) {
        Optional<CraftRecipe> craft = getCurrentCraft(inventory, player);
        ItemStack newStack;
        newStack = craft.map(value -> value.getCraft().clone()).orElse(BARRIER);
        ItemMeta meta = newStack.getItemMeta();
        if (meta != null && newStack != BARRIER) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add("");
            lore.add(Config.CLICK_TO_CRAFT.replace("&", "\u00A7"));
            if (craft.get().hasLimit()) {
                lore.add(Config.LIMIT_TIMES.replace("&", "\u00A7")
                        .replace("{times}", String.valueOf(getCraftedTimes(player, craft.get().getRealCraft())))
                        .replace("{limit}", String.valueOf(craft.get().getLimit())));
            }
            meta.setLore(lore);
            newStack.setItemMeta(meta);
        }
        for (int i = 0; i < Config.CRAFTING_PATTERN.size(); i++) {
            for (int j = 0; j < 9; j++) {
                InventoryItem item = recipe.getInventoryItem(Config.CRAFTING_PATTERN, i, j);
                if (item instanceof RecipeSlotItem && ((RecipeSlotItem) item).getSlot() == 0) {
                    inventory.setItem(i * 9 + j, newStack);
                }
            }
        }
    }

    void showLimitMessage(Player player, CraftRecipe craft) {
        if (craft.hasLimit()) {
            if (player != null) {
                String text = Config.SHOW_LIMIT_MESSAGE.replace("&", "\u00A7")
                        .replace("{times}", String.valueOf(getCraftedTimes(player, craft.getRealCraft())))
                        .replace("{limit}", String.valueOf(craft.getLimit()));
                String name = craft.getRealCraft().getName().replace("0","")
                        .replace("1","")
                        .replace("2","")
                        .replace("3","")
                        .replace("4","")
                        .replace("5","")
                        .replace("6","")
                        .replace("7","")
                        .replace("8","")
                        .replace("9","")
                        .replace("&", "");
                text = text.replace("{item}", name);
                player.sendMessage(text);
            }
        }
    }

    boolean matches(ItemStack[] stacks, Craft craft) {
        for (int i = 0; i < 9; i++) {
            if (stacks[i] == null) {
                stacks[i] = new ItemStack(Material.AIR);
            }

            ItemStack oriItem = stacks[i];
            ItemStack oriTarget = craft.getRecipe().get(i);
            if (!craft.getRecipe().get(i).hasItemMeta()) {
                if (!(oriItem.getType() == oriTarget.getType())) {
                    return false;
                }
            } else {
                ItemStack item = oriItem.clone();
                ItemStack target = oriTarget.clone();
                if (item.hasItemMeta() && target.hasItemMeta()) {
                    var itemMeta = item.getItemMeta();
                    var targetMeta = target.getItemMeta();
                    // ignore damage
                    if (itemMeta instanceof Damageable && targetMeta instanceof Damageable) {
                        ((Damageable) itemMeta).setDamage(0);
                        ((Damageable) targetMeta).setDamage(0);
                    }
                    //ignore skull meta
                    if (itemMeta instanceof SkullMeta && targetMeta instanceof SkullMeta) {
                        ((SkullMeta) itemMeta).setOwningPlayer(null);
                        ((SkullMeta) targetMeta).setOwningPlayer(null);
                    }
                    // ignore enchantments
                    item.getEnchantments().forEach((k, v) -> item.removeEnchantment(k));
                    target.getEnchantments().forEach((k, v) -> target.removeEnchantment(k));
                    // ignore name
                    itemMeta.setDisplayName(null);
                    targetMeta.setDisplayName(null);
                    // ignore lore
                    itemMeta.setLore(null);
                    targetMeta.setLore(null);
                    // ignore attributes
                    if (itemMeta.hasAttributeModifiers()) {
                        itemMeta.getAttributeModifiers().forEach((k, v) -> itemMeta.removeAttributeModifier(k));
                    }
                    if (targetMeta.hasAttributeModifiers()) {
                        targetMeta.getAttributeModifiers().forEach((k, v) -> itemMeta.removeAttributeModifier(k));
                    }
                    item.setItemMeta(itemMeta);
                    target.setItemMeta(targetMeta);
                }
                if (!target.isSimilar(item)) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean fusionMatches(ItemStack[] stacks, Craft craft) {
        for (int i = 0; i < 9; i++) {
            if (stacks[i] == null) {
                stacks[i] = new ItemStack(Material.AIR);
            }
            ItemStack oriItem = stacks[i];
            ItemStack oriTarget = craft.getRecipe().get(i);
            if (UniversalMaterial.isDiamondArmor(oriItem.getType())!=UniversalMaterial.isDiamondArmor(oriTarget.getType())){
                if (UniversalMaterial.isDiamondArmor(oriTarget.getType())){
                    return false;
                }
            }
        }
        return true;
    }

    int getMaximumCrafts(Inventory inventory) {
        int maximum = Integer.MAX_VALUE;
        for (int i = 0; i < Config.CRAFTING_PATTERN.size(); i++) {
            for (int j = 0; j < 9; j++) {
                InventoryItem item = recipe.getInventoryItem(Config.CRAFTING_PATTERN, i, j);
                if (item instanceof RecipeSlotItem && ((RecipeSlotItem) item).getSlot() != 0) {
                    ItemStack itemStack = inventory.getItem(i * 9 + j);
                    if (itemStack != null) {
                        maximum = Math.min(maximum, itemStack.getAmount());
                    }
                }
            }
        }
        return maximum;
    }

    Optional<CraftRecipe> getCurrentCraft(Inventory inventory, Player player) {
        ItemStack[] itemStacks = new ItemStack[9];
        for (int i = 0; i < Config.CRAFTING_PATTERN.size(); i++) {
            for (int j = 0; j < 9; j++) {
                InventoryItem item = recipe.getInventoryItem(Config.CRAFTING_PATTERN, i, j);
                if (item instanceof RecipeSlotItem) {
                    int slot = ((RecipeSlotItem) item).getSlot();
                    if (slot != 0) {
                        itemStacks[slot - 1] = inventory.getItem(i * 9 + j);
                        if(itemStacks[slot - 1]==null){
                            itemStacks[slot - 1] = new ItemStack(Material.AIR);
                        }
                    }
                }
            }
        }

        craftMap = CraftsManager.getCraftMap();

        if (craftMap.containsKey(shapelessHashCode(itemStacks))){
            Craft craft = craftMap.get(shapelessHashCode(itemStacks));

            return Optional.of(new CraftRecipe(craft.getLimit(), craft.getCraft(), craft));
        }

        if (craftMap.containsKey(itemStacksHashCode(itemStacks))){
            Craft craft = craftMap.get(itemStacksHashCode(itemStacks));

            ItemMeta itemMeta = craft.getCraft().getItemMeta();

            //Barbarian recipe potion check
            if(itemMeta!=null
                    && itemMeta.getLore()!=null
                    && itemMeta.getLore().contains(Lang.ITEMS_BARBARIAN)){
                for(int k = 0 ; k < 9 ; k++){
                    if (itemStacks[k]!=null && itemStacks[k].getType().equals(Material.POTION)){
                        PotionMeta potionMeta = (PotionMeta) itemStacks[k].getItemMeta();
                        assert potionMeta != null;
                        if(!(potionMeta.getBasePotionData().isExtended()&&potionMeta.getBasePotionData().getType().equals(PotionType.STRENGTH))){
                            return Optional.empty();
                        }
                    }
                }
            }

            return Optional.of(new CraftRecipe(craft.getLimit(), craft.getCraft(), craft));
        }

        /*for (Craft craft : CraftsManager.getCrafts()) {
            if (matches(itemStacks, craft)) {
                return Optional.of(new CraftRecipe(craft.getLimit(), craft.getCraft(), craft));
            }else if(fusionMatches(itemStacks, craft)
                    &&craft.getCraft().getItemMeta()!=null
                    &&craft.getCraft().getItemMeta().getLore()!=null
                    &&craft.getCraft().getItemMeta().getLore().contains(Lang.ITEMS_FUSION_ARMOR)){
                return Optional.of(new CraftRecipe(craft.getLimit(), craft.getCraft(), craft));
            }
        }*/

        Recipe recipe = Bukkit.getCraftingRecipe(itemStacks, player.getWorld());
        if (recipe != null) {
            return Optional.of(new CraftRecipe(-1, recipe.getResult(), null));
        }
        return Optional.empty();
    }

    private long itemStacksHashCode(ItemStack[] itemStacks) {
        long hash = 0;
        for (ItemStack itemStack : itemStacks) {
            //hash += (i + 1) * itemStacks[i].getType().hashCode();
            if (UniversalMaterial.isMeat(itemStack.getType())) {
                hash = 131 * hash + Material.BEEF.hashCode();
            } else if (Tag.ANVIL.isTagged(itemStack.getType())) {
                hash = 131 * hash + Material.ANVIL.hashCode();
            } else if (Tag.ITEMS_MUSIC_DISCS.isTagged(itemStack.getType())) {
                hash = 131 * hash + Material.MUSIC_DISC_13.hashCode();
            } else if (Tag.WOOL.isTagged(itemStack.getType())) {
                hash = 131 * hash + Material.WHITE_WOOL.hashCode();
            } else if (itemStack.getType().equals(Material.CHARCOAL)) {
                hash = 131 * hash + Material.COAL.hashCode();
            } else if (itemStack.getType().equals(Material.COBBLED_DEEPSLATE)) {
                hash = 131 * hash + Material.COBBLESTONE.hashCode();
            } else {
                hash = 131 * hash + itemStack.getType().hashCode();
            }
        }
        return hash;
    }

    private long shapelessHashCode(ItemStack[] itemStacks) {
        long hash = 0;
        for (ItemStack itemStack : itemStacks) {
            if (Tag.SAPLINGS.isTagged(itemStack.getType())) {
                hash += Material.OAK_SAPLING.hashCode();
            } else if (UniversalMaterial.isDiamondArmor(itemStack.getType())) {
                hash += Material.DIAMOND_HELMET.hashCode();
            } else {
                hash += itemStack.getType().hashCode();
            }
        }
        return hash;
    }

    private long recipeNeedHashCode(ItemStack[] itemStacks) {
        long hash = 0;
        for (ItemStack itemStack : itemStacks) {
            if (Tag.SAPLINGS.isTagged(itemStack.getType())) {
                hash += (long) itemStack.getAmount() * Material.OAK_SAPLING.hashCode();
            } else if (UniversalMaterial.isDiamondArmor(itemStack.getType())) {
                hash += (long) itemStack.getAmount() * Material.DIAMOND_HELMET.hashCode();
            } else if (itemStack.getType().equals(Material.AIR)){
                hash += (long) itemStack.getAmount() * itemStack.getType().hashCode();
            }
        }
        return hash;
    }

    /*@EventHandler(priority = EventPriority.HIGHEST)
    public void onHopperEvent(InventoryMoveItemEvent event) {
        Inventory inv = event.getDestination();
        if(inv.getType().equals(InventoryType.BREWING) && config.get(MainConfig.BAN_LEVEL_TWO_POTIONS) && inv.getHolder() instanceof BrewingStand){
            Bukkit.getScheduler().runTaskLater(plugin, new PlayerListener.CheckBrewingStandAfterClick((BrewingStand) inv.getHolder(), null),1);
        }

    }

    private static class CheckBrewingStandAfterClick implements Runnable{
        private final BrewingStand stand;
        private final HumanEntity human;

        private CheckBrewingStandAfterClick(BrewingStand stand, HumanEntity human) {
            this.stand = stand;
            this.human = human;
        }

        @Override
        public void run(){
            ItemStack ingredient = stand.getInventory().getIngredient();
            PotionType type0 = null;
            PotionType type1 = null;
            PotionType type2 = null;
            if (stand.getInventory().getItem(0) != null){
                PotionMeta potion0 = (PotionMeta) stand.getInventory().getItem(0).getItemMeta();
                type0 = potion0.getBasePotionData().getType();
            }
            if (stand.getInventory().getItem(1) != null){
                PotionMeta potion1 = (PotionMeta) stand.getInventory().getItem(1).getItemMeta();
                type1 = potion1.getBasePotionData().getType();
            }
            if (stand.getInventory().getItem(2) != null){
                PotionMeta potion2 = (PotionMeta) stand.getInventory().getItem(2).getItemMeta();
                type2 = potion2.getBasePotionData().getType();
            }

            if(ingredient != null && ingredient.getType().equals(Material.GLOWSTONE_DUST)
                    && (type0==PotionType.STRENGTH||type1==PotionType.STRENGTH||type2==PotionType.STRENGTH)){
                if(human != null){
                    human.sendMessage(Lang.ITEMS_POTION_BANNED);
                }

                stand.getLocation().getWorld().dropItemNaturally(stand.getLocation(), ingredient.clone());
                stand.getInventory().setIngredient(new ItemStack(Material.AIR));
            }
        }
    }*/
}
