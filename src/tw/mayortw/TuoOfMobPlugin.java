package tw.mayortw;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TuoOfMobPlugin extends JavaPlugin implements Listener {

    private final String CONF_PATH = "tuo";

    private List<MobRoot> rootMobs = new CopyOnWriteArrayList<>();
    private Map<Player, MobRoot> playerSelections = new ConcurrentHashMap<>();

    private boolean loaded;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskLater(this, () -> loadEntities(), 120);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for(MobRoot root : rootMobs) {
                root.updatePosition();
            }
        }, 30, 1);

        getLogger().info("TuoOfMob Enabled");
    }

    @Override
    public void onDisable() {

        for(Player player : playerSelections.keySet()) {
            deselect(player);
        }

        cleanEntityList();

        getLogger().info("Saving data");

        FileConfiguration config = getConfig();

        for(MobRoot root : rootMobs) {
            Map<Entity, Location> map = root.getEntityMap();
            for(Map.Entry<Entity, Location> e : map.entrySet()) {
                String path = CONF_PATH + "." + root.getRootEntity().getUniqueId() + "." + e.getKey().getUniqueId().toString();
                config.set(path + ".x", e.getValue().getX());
                config.set(path + ".y", e.getValue().getY());
                config.set(path + ".z", e.getValue().getZ());
                config.set(path + ".yaw", e.getValue().getYaw());
                config.set(path + ".pitch", e.getValue().getPitch());
            }
        }

        saveConfig();

        getLogger().info("TuoOfMob Disabled");
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent eve) {
        loadEntities();
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent eve) {

        Player player = eve.getPlayer();
        Entity entity = eve.getRightClicked();

        if(eve.isCancelled()) return;
        if(!player.hasPermission("tuo.use")) return;

        if(player.getInventory().getItemInMainHand().getType() == Material.BONE) {

            select(player, entity);
            eve.setCancelled(true);

        } else {
            MobRoot root = playerSelections.get(player);
            if(root != null) {
                if(player.getInventory().getItemInMainHand().getType() == Material.FEATHER) {
                    root.addEntity(entity);
                    eve.setCancelled(true);
                } else if(player.getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD) {
                    root.removeEntity(entity);
                    eve.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent eve) {

        if(eve.isCancelled()) return;
        if(eve.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Material material = eve.getMaterial();
        if(material == Material.BONE) {
            deselect(eve.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent eve) {
        cleanEntityList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean rst = false;

        if(cmd.getName().equalsIgnoreCase("tuo")) {

            if(!(sender instanceof Player)) {
                sender.sendMessage("You are not player");
                return true;
            }

            MobRoot root = playerSelections.get((Player) sender);
            
            if(args.length > 0) {
                switch(args[0].toLowerCase()) {
                    case "sel":
                        if(args.length > 1) {

                            double range;
                            try {
                                range = Double.parseDouble(args[1]);
                            } catch(NumberFormatException e) {
                                rst = false;
                                break;
                            }

                            if(root == null) {
                                sender.sendMessage("No selected root");
                            } else {
                                for(Entity entity : ((Entity) sender).getNearbyEntities(range, range, range)) {
                                    if(!root.getRootEntity().equals(entity)) {
                                        root.addEntity(entity);
                                    }
                                }
                            }
                            rst = true;
                        } else {
                            rst = false;
                        }
                        break;
                    case "clear":
                        if(root == null) {
                            sender.sendMessage("No selected root");
                        } else {
                            root.removeAllEntity();
                            cleanEntityList();
                        }
                        rst = true;
                        break;
                    default:
                        rst = false;
                }
            } else {
                rst = false;
            }
        }

        return rst;
    }

    private void loadEntities() {

        if(loaded)
            return;
        loaded = true;

        FileConfiguration config = getConfig();
        ConfigurationSection section = config.getConfigurationSection(CONF_PATH);

        if(section != null) {
            for(String rootId : section.getKeys(false)) {

                Entity rootEntity = getServer().getEntity(UUID.fromString(rootId));
                if(rootEntity != null) {

                    MobRoot root = findMobRoot(rootEntity);
                    if(root == null) {
                        root = new MobRoot(rootEntity);
                        rootMobs.add(root);
                        getLogger().fine("Added " + root.toString() + " to list");
                    }

                    for(String childId : config.getConfigurationSection(CONF_PATH + "." + rootId).getKeys(false)) {
                        String path = CONF_PATH + "." + rootId + "." + childId;

                        double x = config.getDouble(path + ".x");
                        double y = config.getDouble(path + ".y");
                        double z = config.getDouble(path + ".z");
                        float yaw = (float) config.getDouble(path + ".yaw");
                        float pitch = (float) config.getDouble(path + ".pitch");

                        Entity child = getServer().getEntity(UUID.fromString(childId));
                        if(child != null) {
                            root.addEntity(child, new Location(rootEntity.getWorld(), x, y, z, yaw, pitch));
                        } else loaded = false;
                    }
                } else loaded = false;
            }
        }

        if(loaded) getLogger().info("Loaded");
    }

    private void cleanEntityList() {
        for(MobRoot root : rootMobs) {
            if(root.entityCount() <= 0 ||
                    root.getRootEntity() == null || root.getRootEntity().isDead()) {
                root.removeAllEntity();
                rootMobs.remove(root);
                getConfig().set(CONF_PATH + "." + root.getRootEntity().getUniqueId().toString(), null);
                getLogger().fine("Removed " + root.toString() + " from list");
            }
        }
        for(Player player : playerSelections.keySet()) {
            if(!player.isOnline()) {
                deselect(player);
            }
        }
    }

    private MobRoot findMobRoot(Entity entity) {
        for(MobRoot root : rootMobs) {
            if(root.getRootEntity().equals(entity))
                return root;
        }
        return null;
    }

    private void select(Player player, Entity entity) {

        deselect(player);

        MobRoot root = findMobRoot(entity);
        if(root == null) {
            root = new MobRoot(entity);
            rootMobs.add(root);
            getLogger().fine("Added " + root.toString() + " to list");
        }

        root.setMark(true);

        MobRoot selected = playerSelections.get(player);
        if(selected == null || !selected.getRootEntity().equals(entity))
            playerSelections.put(player, root);

        getLogger().fine("Selected " + root.toString());
    }

    private void deselect(Player player) {
        MobRoot root = playerSelections.get(player);
        if(root != null) {

            root.setMark(false);
            playerSelections.remove(player);
            cleanEntityList();

            getLogger().fine("Deselected " + root.toString());
        }
    }
}
