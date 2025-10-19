package me.ihqqq.MMOTrade;

import me.ihqqq.MMOTrade.npc.NPCCommand;
import me.ihqqq.MMOTrade.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MMOTrade extends JavaPlugin implements TabExecutor {

   private FileConfiguration messages;
   private File messagesFile;
   private File shopFolder;
   private boolean mmoItemsEnabled = false;
   private NPCManager npcManager;

   @Override
   public void onEnable() {
      // Check dependencies
      mmoItemsEnabled = Bukkit.getPluginManager().getPlugin("MMOItems") != null;

      // Setup configuration
      saveDefaultConfig();
      setupLanguageFiles();
      loadMessages();
      setupShopsFolder();

      // Register commands and listeners
      getCommand("mmotrade").setExecutor(this);
      Bukkit.getPluginManager().registerEvents(new TradeListener(this), this);

      // Initialize NPC Manager
      npcManager = new NPCManager(this);
      if (npcManager.isEnabled()) {
         getCommand("mtnpc").setExecutor(new NPCCommand(this, npcManager));
      }

      // Log startup summary
      logStartupSummary();
   }

   private void logStartupSummary() {
      getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      getLogger().info("MMOTrade v" + getDescription().getVersion());
      getLogger().info("Dependencies:");
      getLogger().info("  MMOItems: " + (mmoItemsEnabled ? "✓" : "✗"));
      getLogger().info("  FancyNpcs: " + (npcManager.isEnabled() ? "✓ (" + npcManager.getNPCCount() + " NPCs)" : "✗"));
      getLogger().info("Shops: " + countShops());
      getLogger().info("Language: " + getConfig().getString("language", "en"));
      getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
   }

   private int countShops() {
      if (!shopFolder.exists()) return 0;
      File[] files = shopFolder.listFiles((dir, name) -> name.endsWith(".yml"));
      return files != null ? files.length : 0;
   }

   public NPCManager getNpcManager() {
      return npcManager;
   }

   private void setupLanguageFiles() {
      File langFolder = new File(getDataFolder(), "languages");
      if (!langFolder.exists()) langFolder.mkdirs();

      // Create default language files if missing
      saveResourceIfMissing("languages/en.yml");
      saveResourceIfMissing("languages/vi.yml");
   }

   private void saveResourceIfMissing(String path) {
      File file = new File(getDataFolder(), path);
      if (!file.exists()) {
         saveResource(path, false);
      }
   }

   private void loadMessages() {
      String language = getConfig().getString("language", "en");
      messagesFile = new File(getDataFolder(), "languages/" + language + ".yml");

      if (!messagesFile.exists()) {
         getLogger().warning("Language '" + language + "' not found, using English");
         messagesFile = new File(getDataFolder(), "languages/en.yml");
      }

      messages = YamlConfiguration.loadConfiguration(messagesFile);
   }

   private void setupShopsFolder() {
      shopFolder = new File(getDataFolder(), "shops");
      if (!shopFolder.exists()) shopFolder.mkdirs();

      File example = new File(shopFolder, "example.yml");
      if (!example.exists()) {
         saveResource("shops/example.yml", false);
      }
   }

   public String getMessage(String key) {
      String message = messages.getString(key, "&cMessage not found: " + key);
      String prefix = messages.getString("prefix", "&8[&6MMOTrade&8]&r");
      message = message.replace("{prefix}", prefix);
      return ChatColor.translateAlternateColorCodes('&', message);
   }

   public boolean isMMOItemsEnabled() {
      return mmoItemsEnabled;
   }

   /**
    * Open a shop for a player (public method for NPC integration)
    */
   public void openShop(Player player, String shopName) {
      File file = new File(shopFolder, shopName + ".yml");

      if (!file.exists()) {
         player.sendMessage(getMessage("shop-not-found").replace("{shop}", shopName));
         return;
      }

      List<MerchantRecipe> recipes = ShopLoader.loadShop(file, this);
      if (recipes.isEmpty()) {
         player.sendMessage(getMessage("shop-empty"));
         return;
      }

      Merchant merchant = Bukkit.createMerchant(ChatColor.translateAlternateColorCodes('&', shopName));
      merchant.setRecipes(recipes);
      player.openMerchant(merchant, true);
   }

   @Override
   public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
      if (!(sender instanceof Player)) {
         sender.sendMessage(getMessage("players-only"));
         return true;
      }
      Player player = (Player) sender;

      if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
         if (!player.hasPermission("mmotrade.reload")) {
            player.sendMessage(getMessage("no-permission"));
            return true;
         }
         reloadConfig();
         loadMessages();
         if (npcManager != null) {
            npcManager.reload();
         }
         player.sendMessage(getMessage("reloaded"));
         return true;
      }

      if (args.length != 1) {
         player.sendMessage(getMessage("usage"));
         return true;
      }

      String shopName = args[0];
      openShop(player, shopName);
      return true;
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
      if (args.length == 1) {
         List<String> options = new ArrayList<>();
         String current = args[0].toLowerCase();

         if ("reload".startsWith(current)) {
            options.add("reload");
         }

         if (shopFolder.exists()) {
            File[] files = shopFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
               for (File f : files) {
                  String name = f.getName().replace(".yml", "");
                  if (name.toLowerCase().startsWith(current)) {
                     options.add(name);
                  }
               }
            }
         }
         return options;
      }
      return Collections.emptyList();
   }
}