package com.bnjrKemal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class ASpawner extends JavaPlugin implements CommandExecutor, Listener {

    private final Map<Location, ItemStack> spawnerItems = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("aspawner").setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        loadSpawnerDataFromFile();
    }

    @Override
    public void onDisable(){
        saveDataToFile();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-players-can-use-command"));
            return false;
        }
        Player player = (Player) sender;
        if(!player.isOp()){
            player.sendMessage(getMessage("you-are-not-OP"));
            return false;
        }
        ItemStack playerItem = player.getInventory().getItemInMainHand();
        if (playerItem.getType() == Material.AIR) {
            player.sendMessage(getMessage("no-item-in-hand"));
            return false;
        }
        ItemStack itemStack = new ItemStack(Material.SPAWNER);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.GOLD + "Spawner of " + playerItem.getType());
        itemMeta.setLocalizedName(playerItem.getType().toString());
        itemStack.setItemMeta(itemMeta);
        player.getInventory().addItem(itemStack);
        return true;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e){
        if(e.getBlock().getType().equals(Material.SPAWNER)){
            if(Material.matchMaterial(e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getLocalizedName()) == null) return;
            Material localizedName = Material.matchMaterial(e.getPlayer().getInventory().getItemInMainHand().getItemMeta().getLocalizedName());
            ItemStack itemStack = new ItemStack(localizedName);
            CreatureSpawner spawner = (CreatureSpawner) e.getBlock().getState();
            spawner.setSpawnedType(EntityType.DROPPED_ITEM);
            spawner.setSpawnedItem(itemStack);
            spawner.update();
            spawnerItems.put(e.getBlock().getLocation(), itemStack);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block brokenBlock = event.getBlock();
        if (brokenBlock.getType() == Material.SPAWNER) {
            if(spawnerItems.containsKey(event.getBlock().getLocation())) {
                ItemStack itemStack = spawnerItems.get(event.getBlock().getLocation());
                ItemStack spawnerItem = createSpawnerItem(itemStack);
                player.getInventory().addItem(spawnerItem);
                spawnerItems.put(event.getBlock().getLocation(), null);
            }
        }
    }

    private ItemStack createSpawnerItem(ItemStack itemStack) {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        ItemMeta spawnerItemMeta = spawner.getItemMeta();
        spawnerItemMeta.setDisplayName(ChatColor.GOLD + "Spawner of " + itemStack.getType());
        spawnerItemMeta.setLocalizedName(itemStack.getType().toString());
        spawner.setItemMeta(spawnerItemMeta);
        return spawner;
    }

    private void loadSpawnerDataFromFile() {
        JSONParser parser = new JSONParser();
        try {
            File file = new File(getDataFolder(), "spawner_data.json");
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileReader fileReader = new FileReader(getDataFolder() + "/spawner_data.json")) {
                Object obj = parser.parse(fileReader);
                JSONArray spawnerDataArray = (JSONArray) obj;
                for (Object spawnerDataObj : spawnerDataArray) {
                    JSONObject spawnerData = (JSONObject) spawnerDataObj;
                    Location location = jsonToLocation((JSONObject) spawnerData.get("location"));
                    String itemType = (String) spawnerData.get("itemType");
                    spawnerItems.put(location, new ItemStack(Material.valueOf(itemType)));
                }
            } catch (IOException | ParseException e) {}
        } catch (IOException e) {}
    }

    private Location jsonToLocation(JSONObject jsonLocation) {
        String worldName = (String) jsonLocation.get("world");
        double x = (double) jsonLocation.get("x");
        double y = (double) jsonLocation.get("y");
        double z = (double) jsonLocation.get("z");
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    @SuppressWarnings("unchecked")
    private void saveDataToFile() {
        JSONArray spawnerDataArray = new JSONArray();
        for (Map.Entry<Location, ItemStack> entry : spawnerItems.entrySet()) {
            if (entry.getValue() != null) {
                JSONObject spawnerData = new JSONObject();
                spawnerData.put("location", locationToJSON(entry.getKey()));
                spawnerData.put("itemType", entry.getValue().getType().name());
                spawnerDataArray.add(spawnerData);
            }
        }
        try {
            File pluginFolder = new File("plugins/ASpawner");
            if (!pluginFolder.exists()) {
                pluginFolder.mkdirs();
            }
            File file = new File(pluginFolder, "spawner_data.json");
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(spawnerDataArray.toJSONString());
                fileWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject locationToJSON(Location location) {
        JSONObject jsonLocation = new JSONObject();
        jsonLocation.put("world", location.getWorld().getName());
        jsonLocation.put("x", location.getX());
        jsonLocation.put("y", location.getY());
        jsonLocation.put("z", location.getZ());
        return jsonLocation;
    }

    public String getMessage(String path){
        if(path.equals("no-item-in-hand")){
            return ChatColor.DARK_RED + "[ASpawner]" + ChatColor.RED + " You must hold an item in your hand.";
        }
        if(path.equals("only-players-can-use-command")){
            return ChatColor.DARK_RED + "[ASpawner]" + ChatColor.RED + " This command is only valid in-game.";
        }
        if(path.equals("you-are-not-OP")){
            return ChatColor.DARK_RED + "[ASpawner]" + ChatColor.RED + " You aren't Administrator.";
        }
        return "getMessage() have to adding this path";
    }
}
