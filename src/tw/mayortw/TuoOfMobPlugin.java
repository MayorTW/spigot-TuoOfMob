package tw.mayortw;

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
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.List;

public class TuoOfMobPlugin extends JavaPlugin implements Listener {

    private int selectedIndex = -1;
    private List<MobRoot> rootMobs = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimer(this, () -> {
            for(MobRoot root : rootMobs) {
                root.updatePosition();
            }
        }, 30, 1);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent eve) {

        Player player = eve.getPlayer();
        Entity entity = eve.getRightClicked();

        if(player.getInventory().getItemInMainHand().getType() == Material.BONE ||
                player.getInventory().getItemInOffHand().getType() == Material.BONE) {

            select(entity);

            eve.setCancelled(true);
        } else if(player.getInventory().getItemInMainHand().getType() == Material.FEATHER ||
                player.getInventory().getItemInOffHand().getType() == Material.FEATHER) {

            if(selectedIndex >= 0 && selectedIndex < rootMobs.size()) {
                MobRoot selected = rootMobs.get(selectedIndex);

                selected.addEntity(entity);
            }

            eve.setCancelled(true);
        } else if(player.getInventory().getItemInMainHand().getType() == Material.BLAZE_ROD ||
                player.getInventory().getItemInOffHand().getType() == Material.BLAZE_ROD) {
                MobRoot selected = rootMobs.get(selectedIndex);

                selected.removeEntity(entity);

            eve.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent eve) {

        if(eve.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = eve.getClickedBlock();
        Material material = eve.getMaterial();
        Player player = eve.getPlayer();

        if(material == Material.BONE) {
            deselect();
        }
    }

    private MobRoot findRoot(Entity entity) {
        for(MobRoot mob : rootMobs) {
            if(mob.getRootEntity().equals(entity))
                return mob;
        }
        return null;
    }

    private void select(Entity entity) {
        if(selectedIndex >= 0 && selectedIndex < rootMobs.size() &&
                rootMobs.get(selectedIndex).getRootEntity().equals(entity))
            return;
        deselect();

        MobRoot root = findRoot(entity);
        if(root == null) {
            root = new MobRoot(entity);
            rootMobs.add(root);
            getLogger().info("Added " + root.toString() + " from list");
        }
        selectedIndex = rootMobs.indexOf(root);

        entity.setGlowing(true);
        root.setMarkChildren(true);

        getLogger().info("Selected " + root.toString());
    }

    private void deselect() {
        if(selectedIndex >= 0 && selectedIndex < rootMobs.size()) {

            MobRoot root = rootMobs.get(selectedIndex);

            root.getRootEntity().setGlowing(false);
            root.setMarkChildren(false);

            getLogger().info("Deselected " + root.toString());

            if(root.entityCount() <= 0) {
                MobRoot removed = rootMobs.remove(selectedIndex);
                getLogger().info("Removed " + removed.toString() + " from list");
            }

            selectedIndex = -1;
        }
    }
}
