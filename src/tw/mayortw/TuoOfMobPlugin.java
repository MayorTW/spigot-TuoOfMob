package tw.mayortw;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

public class TuoOfMobPlugin extends JavaPlugin implements Listener {

    private List<MobRoot> rootMobs = new CopyOnWriteArrayList<>();
    private Map<Player, MobRoot> playerSelections = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for(MobRoot root : rootMobs) {
                root.updatePosition();
            }
        }, 30, 1);

        getLogger().info("TuoOfMob Enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("TuoOfMob Disabled");
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent eve) {

        Player player = eve.getPlayer();
        Entity entity = eve.getRightClicked();

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

                            int range;
                            try {
                                range = Integer.parseInt(args[1]);
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

    private void cleanEntityList() {
        for(MobRoot root : rootMobs) {
            if(root.entityCount() <= 0 || root.getRootEntity().isDead()) {
                root.removeAllEntity();
                rootMobs.remove(root);
                getLogger().info("Removed " + root.toString() + " from list");
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

        MobRoot selected = playerSelections.get(player);
        if(selected != null && selected.getRootEntity().equals(entity))
            return;
        deselect(player);

        MobRoot root = findMobRoot(entity);
        if(root == null) {
            root = new MobRoot(entity);
            rootMobs.add(root);
            getLogger().info("Added " + root.toString() + " to list");
        }

        root.setMark(true);
        playerSelections.put(player, root);

        getLogger().info("Selected " + root.toString());
    }

    private void deselect(Player player) {
        MobRoot root = playerSelections.get(player);
        if(root != null) {

            root.setMark(false);
            playerSelections.remove(player);

            getLogger().info("Deselected " + root.toString());
        }
    }
}
