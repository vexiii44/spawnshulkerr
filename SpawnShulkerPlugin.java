package pl.spawnshulker;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnShulkerPlugin extends JavaPlugin implements Listener {
    private static final String REGION_NAME = "spawn";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SpawnShulker enabled. Read-only shulkers are limited to WorldGuard region '" + REGION_NAME + "'.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShulkerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof ShulkerBox shulker)) {
            return;
        }

        // Outside the WorldGuard region named "spawn", do not interfere:
        // Minecraft handles the shulker normally.
        if (!isInsideSpawn(event.getClickedBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);

        ReadOnlyHolder holder = new ReadOnlyHolder();
        Inventory preview = Bukkit.createInventory(holder, 27, "Podgląd shulkera");
        holder.setInventory(preview);

        for (int slot = 0; slot < Math.min(27, shulker.getInventory().getSize()); slot++) {
            var item = shulker.getInventory().getItem(slot);
            preview.setItem(slot, item == null ? null : item.clone());
        }

        Player player = event.getPlayer();
        player.openInventory(preview);
    }

    private boolean isInsideSpawn(org.bukkit.Location location) {
        var query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
        var applicable = query.getApplicableRegions(BukkitAdapter.adapt(location));
        for (ProtectedRegion region : applicable) {
            if (REGION_NAME.equalsIgnoreCase(region.getId())) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof ReadOnlyHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof ReadOnlyHolder) {
            event.setCancelled(true);
        }
    }

    private static final class ReadOnlyHolder implements InventoryHolder {
        private Inventory inventory;

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
