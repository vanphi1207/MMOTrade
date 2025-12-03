package me.ihqqq.MMOTrade.npc;

import me.ihqqq.MMOTrade.MMOTrade;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NPCCommand implements CommandExecutor, TabCompleter {
    private final MMOTrade plugin;
    private final NPCManager npcManager;

    // Valid mob types for NPCs
    private static final List<String> VALID_MOB_TYPES = Arrays.asList(
            "PLAYER", "ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "ENDERMAN",
            "VILLAGER", "WITCH", "PIGLIN", "ZOMBIFIED_PIGLIN", "IRON_GOLEM",
            "SNOW_GOLEM", "WITHER_SKELETON", "STRAY", "HUSK", "DROWNED",
            "PHANTOM", "BLAZE", "MAGMA_CUBE", "SLIME", "GHAST", "HOGLIN",
            "ZOGLIN", "STRIDER", "BEE", "CAT", "WOLF", "POLAR_BEAR",
            "PANDA", "FOX", "OCELOT", "PARROT", "CHICKEN", "COW", "PIG",
            "SHEEP", "RABBIT", "HORSE", "DONKEY", "MULE", "LLAMA"
    );

    public NPCCommand(MMOTrade plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("players-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!npcManager.isEnabled()) {
            player.sendMessage(plugin.getMessage("npc-not-enabled"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(player, args);
            case "remove":
                return handleRemove(player, args);
            case "edit":
                return handleEdit(player, args);
            case "rename":
                return handleRename(player, args);
            case "settype":
            case "type":
                return handleSetType(player, args);
            case "list":
                return handleList(player);
            case "reload":
                return handleReload(player);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        // /mtnpc create <npc_name> <shop_name> [skin]
        if (!player.hasPermission("mmotrade.npc.create")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("npc-create-usage"));
            return true;
        }

        String npcName = args[1];
        String shopName = args[2];
        String skin = args.length >= 4 ? args[3] : null;

        // Check if shop exists
        File shopFile = new File(plugin.getDataFolder(), "shops/" + shopName + ".yml");
        if (!shopFile.exists()) {
            player.sendMessage(plugin.getMessage("shop-not-found").replace("{shop}", shopName));
            return true;
        }

        // Create NPC at player's location
        npcManager.createNPC(npcName, shopName, player.getLocation(), player, skin);
        return true;
    }

    private boolean handleRemove(Player player, String[] args) {
        // /mtnpc remove <npc_name>
        if (!player.hasPermission("mmotrade.npc.remove")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getMessage("npc-remove-usage"));
            return true;
        }

        String npcName = args[1];
        npcManager.removeNPC(npcName, player);
        return true;
    }

    private boolean handleEdit(Player player, String[] args) {
        // /mtnpc edit <npc_name> <shop_name>
        if (!player.hasPermission("mmotrade.npc.edit")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("npc-edit-usage"));
            return true;
        }

        String npcName = args[1];
        String shopName = args[2];

        // Check if shop exists
        File shopFile = new File(plugin.getDataFolder(), "shops/" + shopName + ".yml");
        if (!shopFile.exists()) {
            player.sendMessage(plugin.getMessage("shop-not-found").replace("{shop}", shopName));
            return true;
        }

        npcManager.updateNPCShop(npcName, shopName, player);
        return true;
    }

    private boolean handleRename(Player player, String[] args) {
        // /mtnpc rename <npc_name> <new_display_name>
        if (!player.hasPermission("mmotrade.npc.rename")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("npc-rename-usage"));
            return true;
        }

        String npcName = args[1];

        // Join all remaining args as the new display name (supports spaces)
        StringBuilder newDisplayName = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) newDisplayName.append(" ");
            newDisplayName.append(args[i]);
        }

        npcManager.renameNPC(npcName, newDisplayName.toString(), player);
        return true;
    }

    private boolean handleSetType(Player player, String[] args) {
        // /mtnpc settype <npc_name> <mob_type>
        if (!player.hasPermission("mmotrade.npc.settype")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getMessage("npc-settype-usage"));
            return true;
        }

        String npcName = args[1];
        String mobType = args[2].toUpperCase();

        // Validate mob type
        if (!VALID_MOB_TYPES.contains(mobType)) {
            player.sendMessage(plugin.getMessage("npc-invalid-type")
                    .replace("{type}", mobType));
            player.sendMessage(plugin.getMessage("npc-valid-types"));
            return true;
        }

        // Try to get EntityType
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(mobType);
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getMessage("npc-invalid-type")
                    .replace("{type}", mobType));
            return true;
        }

        npcManager.setNPCType(npcName, entityType, player);
        return true;
    }

    private boolean handleList(Player player) {
        if (!player.hasPermission("mmotrade.npc.list")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        Map<String, String> npcs = npcManager.getNPCShops();

        if (npcs.isEmpty()) {
            player.sendMessage(plugin.getMessage("npc-list-empty"));
            return true;
        }

        player.sendMessage(plugin.getMessage("npc-list-header"));
        for (Map.Entry<String, String> entry : npcs.entrySet()) {
            player.sendMessage(plugin.getMessage("npc-list-format")
                    .replace("{npc}", entry.getKey())
                    .replace("{shop}", entry.getValue()));
        }
        return true;
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("mmotrade.npc.reload")) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return true;
        }

        npcManager.reload();
        player.sendMessage(plugin.getMessage("npc-reloaded"));
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getMessage("npc-help-header"));
        player.sendMessage(plugin.getMessage("npc-help-create"));
        player.sendMessage(plugin.getMessage("npc-help-remove"));
        player.sendMessage(plugin.getMessage("npc-help-edit"));
        player.sendMessage(plugin.getMessage("npc-help-rename"));
        player.sendMessage(plugin.getMessage("npc-help-settype"));
        player.sendMessage(plugin.getMessage("npc-help-list"));
        player.sendMessage(plugin.getMessage("npc-help-reload"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return null;

        if (args.length == 1) {
            return Arrays.asList("create", "remove", "edit", "rename", "settype", "list", "reload").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("remove") || subCmd.equals("edit") || subCmd.equals("rename") || subCmd.equals("settype") || subCmd.equals("type")) {
                // Suggest NPC names
                return new ArrayList<>(npcManager.getNPCShops().keySet()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("create") || subCmd.equals("edit")) {
                // Suggest shop names
                File shopFolder = new File(plugin.getDataFolder(), "shops");
                if (shopFolder.exists()) {
                    File[] files = shopFolder.listFiles();
                    if (files != null) {
                        return Arrays.stream(files)
                                .filter(f -> f.getName().endsWith(".yml"))
                                .map(f -> f.getName().replace(".yml", ""))
                                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                                .collect(Collectors.toList());
                    }
                }
            } else if (subCmd.equals("settype") || subCmd.equals("type")) {
                // Suggest mob types
                return VALID_MOB_TYPES.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return null;
    }
}