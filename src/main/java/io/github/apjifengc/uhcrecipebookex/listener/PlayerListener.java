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
import io.github.apjifengc.uhcrecipebookex.util.Util;
//import me.emptyirony.enchantmentbook.enchantmentbook.EnchantmentBook;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
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
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static org.bukkit.Bukkit.broadcastMessage;
import static org.bukkit.Bukkit.getLogger;

public class PlayerListener implements Listener {
    private final UhcRecipeBookEx plugin = UhcRecipeBookEx.getInstance();
    private final CraftRecipeInventory recipe = UhcRecipeBookEx.getRecipeInventory();
    private final Map<UhcPlayer, Map<ItemStack, Integer>> craftedItems = new HashMap<>();
    private final PlayerManager playerManager;
    GameManager gm = GameManager.getGameManager();
    public static final ItemStack BARRIER = new ItemStack(Material.BARRIER);

    public static Map<Long, Craft> craftMap = new HashMap<>();

    private ItemStack addedItemStack;

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
            UhcItems.handleModularSwitch(4, hand, player);
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
        ItemStack item = event.getItem();

        /*if (Tag.SHULKER_BOXES.isTagged(hand.getType())) {
            event.setCancelled(true);
            player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, 1, 1);
            player.openInventory(ShulkerInventoryHandler.createShulkerBoxInventory(player, player.getInventory().getItemInMainHand()));
            return;
        }*/

        Bukkit.getScheduler().scheduleSyncDelayedTask(UhcCore.getPlugin(), new CheckArmorThread(player), 1);

        if (GameItem.isGameItem(hand)) {
            event.setCancelled(true);
            GameItem gameItem = GameItem.getGameItem(hand);
            handleGameItemInteract(gameItem, player, uhcPlayer, hand);
            return;
        }

        GameManager gameManager = GameManager.getGameManager();
        GameState state = gameManager.getGameState();
        if ((state == GameState.PLAYING || state == GameState.DEATHMATCH)
                && uhcPlayer.getState().equals(PlayerState.PLAYING)
                && (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK)
        ) {
            if(UhcItems.isGoldenHeadItem(item)){
                uhcPlayer.getTeam().regenTeamGold(false, 1, uhcPlayer, item);
                event.setCancelled(true);
                return;
            }
            if(UhcItems.isRegenHeadItem(item)){
                uhcPlayer.getTeam().regenTeam(false, 1, uhcPlayer, item);
                event.setCancelled(true);
                return;
            }
            if(UhcItems.isBackpackItem(item)&&Tag.SHULKER_BOXES.isTagged(item.getType())){
                event.setCancelled(true);
                player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, 1, 1);
                player.openInventory(UhcRecipeBookEx.getBackpackInventory().createInventory(item));
                //player.openInventory(ShulkerInventoryHandler.createShulkerBoxInventory(player, hand));
                return;
            }
			/*if(UhcItems.isBackpackItem(event.getPlayer().getInventory().getItemInOffHand())&&Tag.SHULKER_BOXES.isTagged(player.getInventory().getItemInOffHand().getType())){
				event.setCancelled(true);
				return;
			}*/
            if(UhcItems.isCornItem(item)){
                uhcPlayer.regenPlayerCorn(1,uhcPlayer, item);
                event.setCancelled(true);
                return;
            }
            if(UhcItems.isMasterCompassItem(hand)){
                uhcPlayer.pointMasterCompassToPlayer(2,uhcPlayer, item);
                event.setCancelled(true);
                return;
            }
            if(UhcItems.isTheMarkItem(item)){
                if(!uhcPlayer.isTheMarkCooldown(5,uhcPlayer)){
                    UhcItems.launchTheMark(player);
                }
                event.setCancelled(true);
            }
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

    private boolean isCantEnchant(ItemStack itemStack){
        return itemStack.getItemMeta() != null
                && itemStack.getItemMeta().getLore() != null
                && itemStack.getItemMeta().getLore().contains(Lang.ITEMS_CANT_ENCHANT);
    }

    private boolean isCantMove(ItemStack itemStack){
        return itemStack.getItemMeta() != null
                && itemStack.getItemMeta().getLore() != null
                && itemStack.getItemMeta().getLore().contains(Lang.ITEMS_CANT_MOVE);
    }

    private boolean isEnchantCheckInventory(InventoryType inventoryType){
        return inventoryType.equals(InventoryType.ANVIL)
                || inventoryType.equals(InventoryType.GRINDSTONE)
                || inventoryType.equals(InventoryType.SMITHING)
                || inventoryType.equals(InventoryType.ENCHANTING);
    }

    @EventHandler
    void onInventoryClick(InventoryClickEvent event) {

        if (event.getClickedInventory()!=null) {

            if (event.isRightClick()) {
                ItemStack item = event.getCurrentItem();
                Player player = (Player) event.getWhoClicked();
                if (UhcItems.isBackpackItem(item)) {
                    event.setCancelled(true);
                    player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, 1, 1);
                    player.openInventory(UhcRecipeBookEx.getBackpackInventory().createInventory(item));
                    return;
                }
            }

            if (isEnchantCheckInventory(event.getView().getTopInventory().getType())){
                if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) && event.getClickedInventory().getType().equals(InventoryType.PLAYER)
                || (event.getAction().equals(InventoryAction.PLACE_ALL) || event.getAction().equals(InventoryAction.PLACE_ONE) || event.getAction().equals(InventoryAction.PLACE_SOME))
                && event.getClickedInventory().getType().equals(InventoryType.PLAYER)) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (event.getView().getTopInventory().getHolder() instanceof BackpackInventoryHolder) {
                if (UhcItems.isBackpackItem(event.getCurrentItem())) {
                    event.setCancelled(true);
                    return;
                }

                if (event.getClickedInventory().getHolder() instanceof BackpackInventoryHolder) {
                    ((BackpackInventoryHolder) event.getClickedInventory().getHolder()).handleClick(event.getInventory(), (Player) event.getWhoClicked());
                    return;
                }
            }

            // Check cant move
            if (event.getCurrentItem() != null && isCantMove(event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }

            if (event.getClickedInventory() instanceof AnvilInventory) {
                AnvilInventory anvil = (AnvilInventory) event.getClickedInventory();
                ItemStack slot0 = anvil.getItem(0);
                ItemStack slot1 = anvil.getItem(1);

                if (event.getSlot() == 2
                        && slot1 != null
                        && slot1.getItemMeta() != null
                        && slot1.getItemMeta().getLore() != null
                        && slot1.getItemMeta().getLore().contains(Lang.ITEMS_ENHANCEMENT_BOOK)) {
                    event.setCancelled(true);

                    if (event.isLeftClick() && slot0 != null
                            && !(slot0.getType().equals(Material.WOODEN_PICKAXE)
                            || slot0.getType().equals(Material.STONE_PICKAXE)
                            || slot0.getType().equals(Material.IRON_PICKAXE)
                            || slot0.getType().equals(Material.GOLDEN_PICKAXE)
                            || slot0.getType().equals(Material.DIAMOND_PICKAXE)
                            || slot0.getType().equals(Material.NETHERITE_PICKAXE))) {
                        if (event.getClick().isShiftClick()) {
                            ItemStack resultEnhanced = UhcItems.handleEnhancementBook(slot0.clone());
                            anvil.setContents(new ItemStack[anvil.getSize()]);
                            event.getWhoClicked().getInventory().addItem(resultEnhanced);
                            Player player = (Player) event.getWhoClicked();
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                        } else {
                            ItemStack resultEnhanced = UhcItems.handleEnhancementBook(slot0.clone());
                            anvil.setContents(new ItemStack[anvil.getSize()]);
                            event.getWhoClicked().setItemOnCursor(resultEnhanced);
                            Player player = (Player) event.getWhoClicked();
                            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                        }
                    }
                }
                return;
            }
        }


        // Custom crafting table

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
            return;
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
            return;
        }
        if (event.getView().getTopInventory().getHolder() instanceof CraftingInventoryHolder) {
            Inventory inventory = event.getClickedInventory();
            Player player = (Player) event.getWhoClicked();
            //broadcastMessage(event.getView().getTitle());
            //broadcastMessage(Config.GUI_AUTO_CRAFTING_NAME);
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateInventory(player, event.getView().getTopInventory());
                    /*if(event.getView().getTitle().contains(Config.GUI_AUTO_CRAFTING_NAME.replace("&8", ""))){
                        player.closeInventory();
                    }*/
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
                        if(itemStack == null || itemStack.getType().equals(Material.AIR) || itemStack.getType().equals(Material.BARRIER)){
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
                        if (craft.hasLimit() && getCraftedTimes(player, craft.getRealCraft()) >= craft.getLimit()) {
                            player.sendMessage(Config.REACH_LIMIT_MESSAGE.replaceAll("&", "\u00A7"));
                            return;
                        }
                        if (event.isShiftClick()) {
                            addedItemStack = itemStack.clone();
                            if(addedItemStack.getItemMeta()!=null&&addedItemStack.getItemMeta().getLore()!=null&&itemStack.getType().getMaxStackSize()==1){
                                ItemMeta meta = addedItemStack.getItemMeta();
                                List<String> lores = meta.getLore();
                                lores.add("");
                                lores.add(Lang.ITEMS_MADE_BY.replaceAll("%player_name%",playerManager.getUhcPlayer(player).getName()
                                        ).replaceAll("&","\u00A7"));
                                meta.setLore(lores);
                                addedItemStack.setItemMeta(meta);
                            }
                            //broadcastMessage("pass 3  " + addedItems.toString());
                            int addedItemCount = ((int) Math.floor(
                                    (double) Math.min(getAddableItemCount(event.getView().getBottomInventory(), addedItemStack),
                                            addedItemStack.getAmount() * getMaximumCrafts(inventory)) / addedItemStack.getAmount())
                            );
                            //broadcastMessage("pass 4  "+addedItems.toString()+addedItemCount);
                            if (craft.getRealCraft()!=null && craft.hasLimit()) {
                                addedItemCount = Math.min(addedItemCount, craft.getLimit() - getCraftedTimes(player, craft.getRealCraft()));
                            }
                            //----------
                            if(addedItemCount>0) {
                                addedItemCount = handleSpecialCrafting(addedItemCount, player);
                                if(addedItemCount==0)return;
                            }
                            //----------
                            if (addedItemCount<0){;
                                event.setCancelled(true);
                                return;
                            }
                            if (!addedItemStack.getType().equals(Material.AIR)) {
                                if (addedItemStack.getType().getMaxStackSize() == 1) {
                                    for (int i = 0; i < addedItemCount; i++) {
                                        event.getWhoClicked().getInventory().addItem(addedItemStack);
                                    }
                                } else {
                                    addedItemStack.setAmount(addedItemCount * addedItemStack.getAmount());
                                    event.getWhoClicked().getInventory().addItem(addedItemStack);
                                }
                            }
                            reduce(inventory, addedItemCount);

                            addCraftedTimes(player, craft.getRealCraft(), addedItemCount);
                            showLimitMessage(player, craft);

                        } else {
                            /*if (craft.hasLimit() && getCraftedTimes(player, craft.getRealCraft()) >= craft.getLimit()) {
                                //broadcastMessage("(java.452)");
                                return;
                            }*/
                            ItemStack cursor = event.getCursor();
                            if (cursor == null || cursor.getType() == Material.AIR || itemStack.isSimilar(cursor)) {
                                int amount;
                                if (cursor == null) {
                                    amount = itemStack.getAmount();
                                } else {
                                    amount = cursor.getAmount() + itemStack.getAmount();
                                }
                                if (amount <= itemStack.getType().getMaxStackSize()) {
                                    addedItemStack = itemStack.clone();
                                    int addedItemCount = handleSpecialCrafting(1, player);
                                    if(addedItemCount==0)return;
                                    if (addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && itemStack.getType().getMaxStackSize() == 1) {
                                        ItemMeta meta = addedItemStack.getItemMeta();
                                        List<String> lores = meta.getLore();
                                        lores.add("");
                                        lores.add(Lang.ITEMS_MADE_BY.replaceAll("%player_name%", playerManager.getUhcPlayer(player).getName()
                                        ).replaceAll("&", "\u00A7"));
                                        meta.setLore(lores);
                                        addedItemStack.setItemMeta(meta);
                                    }
                                    if (event.getClick().equals(ClickType.NUMBER_KEY)) {
                                        if (event.getView().getBottomInventory().getItem(event.getHotbarButton()) == null
                                                || event.getView().getBottomInventory().getItem(event.getHotbarButton()).getType().equals(Material.AIR)) {
                                            event.getView().getBottomInventory().setItem(event.getHotbarButton(),addedItemStack);
                                        } else {
                                            event.setCancelled(true);
                                            return;
                                        }
                                    }else{
                                        addedItemStack.setAmount(amount);
                                        event.getView().setCursor(addedItemStack);
                                    }
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
    
    private int handleSpecialCrafting(int addedItemCount, Player player){
        if (addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_DEUS_EX_MACHINA)) {
            player.setHealth(player.getHealth() / 2);
        }
        //broadcastMessage(addedItems.toString());
        if (addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_DICE)) {
            addedItemStack = GameManager.getGameManager().createDiceResult();
            addedItemCount = 1;
        }
        if (addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_ESSENCE)) {
            UhcPlayer uhcPlayer = playerManager.getUhcPlayer(player);
            uhcPlayer.getTeam().xpTeam(uhcPlayer);
            addedItemCount = 1;
            addedItemStack = new ItemStack(Material.ENCHANTING_TABLE);
        }
        if (addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_FUSION_ARMOR)) {
            addedItemStack = UhcItems.createFusionArmor();
        }
        //broadcastMessage("pass 1  " + addedItems.toString());
        if (addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_FENRIR)) {
            UhcItems.spawnFenrir(player.getLocation(), player);
            addedItemCount = 1;
            addedItemStack = new ItemStack(Material.AIR);
        }
        if (addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_DAREDEVIL)) {
            UhcItems.spawnDaredevil(player.getLocation());
            addedItemCount = 1;
            addedItemStack = new ItemStack(Material.AIR);
        }
        if(addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_FATE)){
            UhcItems.placeFatesCall(player.getLocation());
            addedItemCount = 1;
            addedItemStack = new ItemStack(Material.AIR);
            if(GameManager.getGameManager().getGameState().equals(GameState.DEATHMATCH)) {
                gm.handleDeathMatchBlockClear(player.getLocation());
            }
        }
        if(addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_PANDORAS_BOX)){
            UhcItems.placePandora(player.getLocation());
            addedItemCount = 1;
            addedItemStack = new ItemStack(Material.AIR);
            if(GameManager.getGameManager().getGameState().equals(GameState.DEATHMATCH)) {
                gm.handleDeathMatchBlockClear(player.getLocation());
            }
        }
        if(addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_CHEST_OF_FATE)){
            UhcItems.placeChestOfFate(player.getLocation(), player);
            addedItemCount = 1;
            addedItemStack = new ItemStack(Material.AIR);
            if(GameManager.getGameManager().getGameState().equals(GameState.DEATHMATCH)) {
                gm.handleDeathMatchBlockClear(player.getLocation());
            }
        }
        if(addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_LUCKY_SHEARS)){
            if(player.getHealth()>=10) {
                player.sendMessage(Lang.ITEMS_CANT_CRAFT_NOW);
                return 0;
            }
        }
        //broadcastMessage("pass 2  " + addedItems.toString());
        if (addedItemStack.getItemMeta() != null && addedItemStack.getItemMeta().getLore() != null && addedItemStack.getItemMeta().getLore().contains(Lang.ITEMS_UNBREAKABLE)) {
            ItemMeta meta = addedItemStack.getItemMeta();
            meta.setUnbreakable(true);
            addedItemStack.setItemMeta(meta);
        }
        return addedItemCount;
    }

    @EventHandler
    void onInventoryDrag(InventoryDragEvent event) {
        if (isEnchantCheckInventory(event.getView().getTopInventory().getType())) {
            if (isEnchantCheckInventory(event.getInventory().getType())){
                if (isCantEnchant(event.getOldCursor())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (event.getInventory().getHolder() instanceof CraftRecipeViewerInventoryHolder ||
                event.getInventory().getHolder() instanceof CraftRecipeInventoryHolder) {
            event.setCancelled(true);
            return;
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
            return;
        }
        if (event.getInventory().getHolder() instanceof BackpackInventoryHolder) {
            ((BackpackInventoryHolder) event.getInventory().getHolder()).handleClick(event.getInventory(), (Player) event.getWhoClicked());
        }
    }

    @EventHandler
    void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.WORKBENCH) {
            /*(new BukkitRunnable() {
                @Override
                public void run() {
                    event.getPlayer().openInventory(recipe.createCraftingInventory(false));
                }
            }.runTaskLater(plugin, 1);*/
            event.getPlayer().openInventory(recipe.createCraftingInventory());
            event.setCancelled(true);
        }

        if (event.getInventory().getType().equals(InventoryType.ENCHANTING)){
            EnchantingInventory inv = (EnchantingInventory) event.getInventory();
            ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI,64);
            ItemMeta meta = lapis.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(Lang.ITEMS_CANT_MOVE);
            meta.setLore(lore);
            lapis.setItemMeta(meta);
            inv.setSecondary(lapis);
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
            return;
        }

        if (event.getInventory().getHolder() instanceof BackpackInventoryHolder) {
            ((BackpackInventoryHolder) event.getInventory().getHolder()).saveContents(event.getInventory());
            ((Player) event.getPlayer()).playSound(event.getPlayer().getLocation(), Sound.ENTITY_SHULKER_CLOSE, 1, 1);
            return;
        }

        if (event.getInventory().getType().equals(InventoryType.ENCHANTING)){
            EnchantingInventory inv = (EnchantingInventory) event.getInventory();
            inv.setSecondary(new ItemStack(Material.AIR));
        }
    }

    int getCraftedTimes(Player player, Craft craft) {

        UhcPlayer uhcPlayer = playerManager.getUhcPlayer(player);

        return craftedItems.getOrDefault(uhcPlayer, new HashMap<>()).getOrDefault(craft.getCraft(), 0);
    }

    void addCraftedTimes(Player player, Craft craft, int amount) {
        UhcPlayer uhcPlayer = playerManager.getUhcPlayer(player);
        if (craft == null || craft.getCraft() == null) {
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
        UhcRecipeBookEx.getRecipeReminder().recheck(player);
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

    public void updateInventory(Player player, Inventory inventory) {
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
        if (newStack.getType().equals(Material.AIR)){
            newStack = BARRIER;
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
        if (craft == null || craft.getCraft() == null) {
            return;
        }
        if (craft.hasLimit()) {
            if (player != null) {
                String text = Config.SHOW_LIMIT_MESSAGE.replaceAll("&", "\u00A7")
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

            ItemStack item = stacks[i];
            ItemStack target = craft.getRecipe().get(i);
            if (!craft.getRecipe().get(i).hasItemMeta()) {
                if (!(item.getType() == target.getType())) {
                    return false;
                }
            } else {
                if (!Util.simpleCheckSimilar(item, target)) {
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
                hash = 131 * hash + Material.PORKCHOP.hashCode();
            } else if (Tag.ANVIL.isTagged(itemStack.getType())) {
                hash = 131 * hash + Material.ANVIL.hashCode();
            } else if (Tag.ITEMS_MUSIC_DISCS.isTagged(itemStack.getType())) {
                hash = 131 * hash + Material.MUSIC_DISC_13.hashCode();
            } else if (Tag.WOOL.isTagged(itemStack.getType())) {
                hash = 131 * hash + Material.WHITE_WOOL.hashCode();
            } else if (Tag.LEAVES.isTagged(itemStack.getType())) {
                hash = 131 * hash + Material.OAK_LEAVES.hashCode();
            } else if (Tag.PLANKS.isTagged(itemStack.getType())) {
                hash = 131 * hash + Material.OAK_PLANKS.hashCode();
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
            } else if (Tag.WOOL.isTagged(itemStack.getType())) {
                hash += Material.WHITE_WOOL.hashCode();
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
