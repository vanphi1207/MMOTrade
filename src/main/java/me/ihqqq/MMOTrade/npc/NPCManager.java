package me.ihqqq.MMOTrade.npc;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import me.ihqqq.MMOTrade.MMOTrade;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NPCManager implements Listener {
   private final MMOTrade plugin;
   private final Map<String, String> npcShops; // NPC ID -> Shop name
   private File npcFile;
   private FileConfiguration npcConfig;
   private boolean fancyNpcsEnabled = false;

   public NPCManager(MMOTrade plugin) {
      this.plugin = plugin;
      this.npcShops = new HashMap<>();

      checkFancyNpcs();
      if (fancyNpcsEnabled) {
         setupNPCFile();
         loadNPCs();
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
      }
   }

   private void checkFancyNpcs() {
      fancyNpcsEnabled = Bukkit.getPluginManager().getPlugin("FancyNpcs") != null;
   }

   private void setupNPCFile() {
      npcFile = new File(plugin.getDataFolder(), "npcs.yml");
      if (!npcFile.exists()) {
         plugin.saveResource("npcs.yml", false);
      }
      npcConfig = YamlConfiguration.loadConfiguration(npcFile);
   }

   private void loadNPCs() {
      ConfigurationSection npcs = npcConfig.getConfigurationSection("npcs");
      if (npcs == null) return;

      for (String npcId : npcs.getKeys(false)) {
         String shopName = npcs.getString(npcId + ".shop");
         if (shopName != null) {
            npcShops.put(npcId, shopName);
         }
      }
   }

   public boolean isEnabled() {
      return fancyNpcsEnabled;
   }

   public int getNPCCount() {
      return npcShops.size();
   }

   /**
    * Create a new NPC for a shop - Instant creation
    */
   public boolean createNPC(String npcName, String shopName, Location location, Player creator, String skin) {
      if (!fancyNpcsEnabled) {
         creator.sendMessage(plugin.getMessage("npc-not-enabled"));
         return false;
      }

      try {
         NpcData data = new NpcData(npcName, creator.getUniqueId(), location);

         // Set skin efficiently
         data.setSkin(skin != null && !skin.isEmpty() ? skin : creator.getName());

         // Set display name with caching
         String displayName = npcConfig.getString("default-display-name", "&6[Shop] &e{name}")
                 .replace("{name}", npcName)
                 .replace("{shop}", shopName);
         data.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

         // Create and register NPC
         Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
         FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);

         // Spawn for all players
         npc.create();
         npc.spawnForAll();

         // Save to config and cache
         saveNPCToConfig(npcName, shopName, location);
         npcShops.put(npcName, shopName);

         creator.sendMessage(plugin.getMessage("npc-created")
                 .replace("{npc}", npcName)
                 .replace("{shop}", shopName));

         return true;

      } catch (Exception e) {
         creator.sendMessage(plugin.getMessage("npc-create-failed"));
         plugin.getLogger().severe("Failed to create NPC '" + npcName + "': " + e.getMessage());
         return false;
      }
   }

   /**
    * Remove an NPC - Optimized
    */
   public boolean removeNPC(String npcName, Player remover) {
      if (!fancyNpcsEnabled) {
         remover.sendMessage(plugin.getMessage("npc-not-enabled"));
         return false;
      }

      try {
         Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpc(npcName);
         if (npc == null) {
            remover.sendMessage(plugin.getMessage("npc-not-found").replace("{npc}", npcName));
            return false;
         }

         // Remove from FancyNpcs
         FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
         npc.removeForAll();

         // Remove from config and cache
         removeNPCFromConfig(npcName);
         npcShops.remove(npcName);

         remover.sendMessage(plugin.getMessage("npc-removed").replace("{npc}", npcName));
         return true;

      } catch (Exception e) {
         remover.sendMessage(plugin.getMessage("npc-remove-failed"));
         plugin.getLogger().severe("Failed to remove NPC '" + npcName + "': " + e.getMessage());
         return false;
      }
   }

   /**
    * Update NPC shop assignment
    */
   public boolean updateNPCShop(String npcName, String shopName, Player updater) {
      if (!fancyNpcsEnabled) return false;

      Npc npc = FancyNpcsPlugin.get().getNpcManager().getNpc(npcName);
      if (npc == null) {
         updater.sendMessage(plugin.getMessage("npc-not-found").replace("{npc}", npcName));
         return false;
      }

      npcShops.put(npcName, shopName);
      saveNPCShopToConfig(npcName, shopName);

      updater.sendMessage(plugin.getMessage("npc-updated")
              .replace("{npc}", npcName)
              .replace("{shop}", shopName));
      return true;
   }

   /**
    * Handle NPC interaction - Optimized version without debug logs
    */
   @EventHandler
   public void onNPCInteract(NpcInteractEvent event) {
      Npc npc = event.getNpc();
      Player player = event.getPlayer();
      String npcName = npc.getData().getName();

      // Check if NPC is linked to a shop
      String shopName = npcShops.get(npcName);
      if (shopName == null) return;

      // Check permission
      if (!player.hasPermission("mmotrade.use")) {
         player.sendMessage(plugin.getMessage("no-permission"));
         return;
      }

      // Send custom message if configured
      String message = npcConfig.getString("npcs." + npcName + ".message");
      if (message != null && !message.isEmpty()) {
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
      }

      // Check if shop exists
      File shopFile = new File(plugin.getDataFolder(), "shops/" + shopName + ".yml");
      if (!shopFile.exists()) {
         player.sendMessage(plugin.getMessage("shop-not-found").replace("{shop}", shopName));
         return;
      }

      // Open shop on main thread
      Bukkit.getScheduler().runTask(plugin, () -> plugin.openShop(player, shopName));
   }

   /**
    * Save NPC to config - Optimized
    */
   private void saveNPCToConfig(String npcName, String shopName, Location loc) {
      String path = "npcs." + npcName + ".";
      npcConfig.set(path + "shop", shopName);
      npcConfig.set(path + "location.world", loc.getWorld().getName());
      npcConfig.set(path + "location.x", loc.getX());
      npcConfig.set(path + "location.y", loc.getY());
      npcConfig.set(path + "location.z", loc.getZ());
      npcConfig.set(path + "location.yaw", loc.getYaw());
      npcConfig.set(path + "location.pitch", loc.getPitch());
      saveConfig();
   }

   private void saveNPCShopToConfig(String npcName, String shopName) {
      npcConfig.set("npcs." + npcName + ".shop", shopName);
      saveConfig();
   }

   private void removeNPCFromConfig(String npcName) {
      npcConfig.set("npcs." + npcName, null);
      saveConfig();
   }

   private void saveConfig() {
      try {
         npcConfig.save(npcFile);
      } catch (IOException e) {
         plugin.getLogger().severe("Failed to save npcs.yml: " + e.getMessage());
      }
   }

   /**
    * Get all NPCs (returns defensive copy for thread safety)
    */
   public Map<String, String> getNPCShops() {
      return new HashMap<>(npcShops);
   }

   /**
    * Reload NPC config
    */
   public void reload() {
      npcShops.clear();
      npcConfig = YamlConfiguration.loadConfiguration(npcFile);
      loadNPCs();
   }
}