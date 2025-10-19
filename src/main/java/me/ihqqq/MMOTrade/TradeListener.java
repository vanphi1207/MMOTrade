package me.ihqqq.MMOTrade;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class TradeListener implements Listener {
   private final MMOTrade plugin;
   private Sound cachedSound;
   private float volume;
   private float pitch;

   public TradeListener(MMOTrade plugin) {
      this.plugin = plugin;
      loadSoundSettings();
   }

   private void loadSoundSettings() {
      try {
         String soundName = plugin.getConfig().getString("trade-sound.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
         volume = (float) plugin.getConfig().getDouble("trade-sound.volume", 1.0);
         pitch = (float) plugin.getConfig().getDouble("trade-sound.pitch", 1.0);

         try {
            cachedSound = Sound.valueOf(soundName.toUpperCase());
         } catch (IllegalArgumentException e) {
            cachedSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            plugin.getLogger().warning("Invalid sound: " + soundName + ", using default");
         }
      } catch (Exception e) {
         cachedSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
         volume = 1.0f;
         pitch = 1.0f;
      }
   }

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onTradeClick(InventoryClickEvent event) {
      // Quick checks first
      if (event.getSlotType() != SlotType.RESULT) return;
      if (!(event.getWhoClicked() instanceof Player)) return;

      InventoryView view = event.getView();
      if (view.getType() != InventoryType.MERCHANT) return;
      if (!(view.getTopInventory() instanceof MerchantInventory)) return;

      Player player = (Player) event.getWhoClicked();
      MerchantInventory inv = (MerchantInventory) view.getTopInventory();
      MerchantRecipe recipe = inv.getSelectedRecipe();

      if (recipe == null) return;

      ItemStack result = recipe.getResult();
      if (result == null || result.getType().isAir()) return;

      // Send success message
      String itemName = getItemName(result);
      String message = plugin.getMessage("trade-success")
              .replace("{item}", itemName)
              .replace("{amount}", String.valueOf(result.getAmount()));
      player.sendMessage(message);

      // Play sound
      playTradeSound(player);
   }

   private void playTradeSound(Player player) {
      try {
         player.playSound(player.getLocation(), cachedSound, volume, pitch);
      } catch (Exception e) {
         // Silently fail - sound is not critical
      }
   }

   private String getItemName(ItemStack item) {
      if (item == null) return "Unknown Item";

      if (item.hasItemMeta()) {
         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
         }
      }

      return formatMaterialName(item.getType().toString());
   }

   private String formatMaterialName(String material) {
      String[] words = material.toLowerCase().split("_");
      StringBuilder formatted = new StringBuilder();

      for (String word : words) {
         if (word.length() > 0) {
            formatted.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
         }
      }

      return formatted.toString().trim();
   }
}