package me.ihqqq.MMOTrade;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.ItemMeta;

public class ShopLoader {
   public static List<MerchantRecipe> loadShop(File file, MMOTrade plugin) {
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      List<MerchantRecipe> recipes = new ArrayList();
      List<Map<String, Object>> tradeList = (List)yaml.get("trades");
      if (tradeList == null) {
         plugin.getLogger().warning("No 'trades' section found in " + file.getName());
         return recipes;
      } else {
         for(int i = 0; i < tradeList.size(); ++i) {
            Map trade = (Map)tradeList.get(i);

            try {
               ItemStack result = parseItem(trade.get("type"), trade.get("id"), trade.get("amount"), trade.get("model"), plugin);
               if (result == null) {
                  plugin.getLogger().warning("Invalid result item in trade #" + (i + 1) + " in " + file.getName());
               } else {
                  List<ItemStack> ingredients = new ArrayList();
                  List<Map<String, Object>> inputs = (List)trade.get("input");
                  if (inputs != null) {
                     Iterator var10 = inputs.iterator();

                     while(var10.hasNext()) {
                        Map<String, Object> input = (Map)var10.next();
                        ItemStack ingredient = parseItem(input.get("type"), input.get("id"), input.get("amount"), input.get("model"), plugin);
                        if (ingredient != null) {
                           ingredients.add(ingredient);
                        }
                     }
                  }

                  if (ingredients.isEmpty()) {
                     plugin.getLogger().warning("Trade #" + (i + 1) + " has no valid ingredients in " + file.getName());
                  } else {
                     MerchantRecipe recipe = new MerchantRecipe(result, 999999);
                     if (ingredients.size() >= 1) {
                        recipe.addIngredient((ItemStack)ingredients.get(0));
                     }

                     if (ingredients.size() >= 2) {
                        recipe.addIngredient((ItemStack)ingredients.get(1));
                     }

                     recipes.add(recipe);
                  }
               }
            } catch (Exception var13) {
               plugin.getLogger().warning("Error loading trade #" + (i + 1) + " in " + file.getName() + ": " + var13.getMessage());
               var13.printStackTrace();
            }
         }

         return recipes;
      }
   }

   private static ItemStack parseItem(Object typeObj, Object idObj, Object amountObj, Object modelObj, MMOTrade plugin) {
      if (typeObj != null && idObj != null) {
         String type = typeObj.toString();
         String id = idObj.toString();
         int amount = amountObj instanceof Integer ? (Integer)amountObj : 1;
         Integer customModel = null;
         if (modelObj != null) {
            try {
               customModel = Integer.parseInt(modelObj.toString());
            } catch (NumberFormatException var14) {
               plugin.getLogger().warning("Invalid model value: " + String.valueOf(modelObj));
            }
         }

         ItemStack mmoItem;
         ItemMeta meta;
         if (plugin.isMMOItemsEnabled()) {
            try {
               Type mmoType = Type.get(type);
               if (mmoType != null) {
                  mmoItem = MMOItems.plugin.getItem(mmoType, id);
                  if (mmoItem != null) {
                     mmoItem.setAmount(amount);
                     if (customModel != null) {
                        meta = mmoItem.getItemMeta();
                        if (meta != null) {
                           meta.setCustomModelData(customModel);
                           mmoItem.setItemMeta(meta);
                        }
                     }

                     return mmoItem;
                  }
               }
            } catch (Exception var13) {
            }
         }

         try {
            Material mat = Material.matchMaterial(id.toUpperCase());
            if (mat != null) {
               mmoItem = new ItemStack(mat, amount);
               if (customModel != null) {
                  meta = mmoItem.getItemMeta();
                  if (meta != null) {
                     meta.setCustomModelData(customModel);
                     mmoItem.setItemMeta(meta);
                  }
               }

               return mmoItem;
            }
         } catch (Exception var12) {
            plugin.getLogger().warning("Could not parse material: " + id);
         }

         return null;
      } else {
         return null;
      }
   }
}
