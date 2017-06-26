package tw.mayortw;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;

public class MobRoot {

    private Map<Entity, Location> children = new ConcurrentHashMap<>();
    private Entity entity;
    private boolean markEntity;
    
    public MobRoot(Entity entity) {
        this.entity = entity;
    }

    public Entity getRootEntity() {
        return entity;
    }

    public void addEntity(Entity child) {
        if(entity.isDead()) return;
        if(child.equals(entity)) return;
        if(children.containsKey(child)) return;

        children.put(child, getLocalLoc(entity.getLocation(), child.getLocation()));
        if(markEntity) {
            child.setCustomName("Selected");
            child.setCustomNameVisible(true);
        }

        Bukkit.getLogger().fine("Added " + child.toString());
    }

    public void removeEntity(Entity child) {
        child.setCustomName(null);
        child.setCustomNameVisible(false);
        Location c = children.remove(child);
        if(c != null) Bukkit.getLogger().fine("Removed " + child.toString());
    }

    public void removeAllEntity() {
        for(Map.Entry<Entity, Location> e : children.entrySet()) {
            removeEntity(e.getKey());
        }
    }

    public void setMark(boolean mark) {
        markEntity = mark;
        entity.setGlowing(mark);
        for(Map.Entry<Entity, Location> e : children.entrySet()) {
            Entity child = e.getKey();
            if(markEntity) {
                child.setCustomName("Selected");
                child.setCustomNameVisible(true);
            } else {
                child.setCustomName(null);
                child.setCustomNameVisible(false);
            }
        }
    }

    public int entityCount() {
        return children.size();
    }

    public void updatePosition() {
        if(entity == null || entity.isDead()) return;

        Location entLoc = entity.getLocation();

        for(Map.Entry<Entity, Location> e : children.entrySet()) {

            Entity child = e.getKey();

            if(child.isDead()) {
                removeEntity(child);
            } else {
                Location targetLoc = getGlobalLoc(entLoc, e.getValue());
                if(!child.getLocation().equals(targetLoc)) {
                    teleportEntity(child, targetLoc);
                }
            }
        }
    }

    private void teleportEntity(Entity entity, Location location) {
        List<Entity> passengers = entity.getPassengers();
        entity.eject();
        entity.teleport(location);
        for(Entity passenger : passengers) {
            entity.addPassenger(passenger);
        }
    }

    private Location getLocalLoc(Location loc1, Location loc2) {
        double x = (loc2.getX() - loc1.getX()) * Math.cos(Math.toRadians(loc1.getYaw())) +
            (loc2.getZ() - loc1.getZ()) * Math.sin(Math.toRadians(loc1.getYaw()));
        double z = -(loc2.getX() - loc1.getX()) * Math.sin(Math.toRadians(loc1.getYaw())) +
            (loc2.getZ() - loc1.getZ()) * Math.cos(Math.toRadians(loc1.getYaw()));
        double y = loc2.getY() - loc1.getY();

        return new Location(loc1.getWorld(), x, y, z, loc2.getYaw() - loc1.getYaw(), loc2.getPitch());
    }

    private Location getGlobalLoc(Location loc1, Location loc2) {
        double x = loc2.getX() * Math.cos(Math.toRadians(loc1.getYaw())) -
            loc2.getZ() * Math.sin(Math.toRadians(loc1.getYaw())) + loc1.getX();
        double z = loc2.getX() * Math.sin(Math.toRadians(loc1.getYaw())) +
            loc2.getZ() * Math.cos(Math.toRadians(loc1.getYaw())) + loc1.getZ();
        double y = loc2.getY() + loc1.getY();

        return new Location(loc1.getWorld(), x, y, z, loc2.getYaw() + loc1.getYaw(), loc2.getPitch());
    }
}
