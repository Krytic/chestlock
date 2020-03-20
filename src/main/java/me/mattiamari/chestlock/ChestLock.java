package me.mattiamari.chestlock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestLock extends JavaPlugin {
    private NamespacedKey storeKey = new NamespacedKey(this, "chest_owner");

    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ChestLockListener(), this);
    }

    private Chest getLockedChest(DoubleChest dchest) {
        Chest chestLeft = Chest.class.cast(dchest.getLeftSide());
        Chest chestRight = Chest.class.cast(dchest.getRightSide());

        if (chestRight.getPersistentDataContainer().has(storeKey, PersistentDataType.STRING)) {
            return chestRight;
        }

        return chestLeft;
    }

    private void lockChest(Chest chest, Player player) {
        if (chest.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doublechest = DoubleChest.class.cast(chest.getInventory().getHolder());
            Chest chestLeft = Chest.class.cast(doublechest.getLeftSide());
            Chest chestRight = Chest.class.cast(doublechest.getRightSide());

            chestLeft.getPersistentDataContainer()
                .set(storeKey, PersistentDataType.STRING, player.getUniqueId().toString());
            chestLeft.update();

            chestRight.getPersistentDataContainer()
                .set(storeKey, PersistentDataType.STRING, player.getUniqueId().toString());
            chestRight.update();
            
            return;
        }

        chest.getPersistentDataContainer()
            .set(storeKey, PersistentDataType.STRING, player.getUniqueId().toString());
        chest.update();
    }

    private void unlockChest(Chest chest) {
        if (chest.getInventory() instanceof DoubleChestInventory) {
            DoubleChest doublechest = DoubleChest.class.cast(chest.getInventory().getHolder());
            Chest chestLeft = Chest.class.cast(doublechest.getLeftSide());
            Chest chestRight = Chest.class.cast(doublechest.getRightSide());
                
            chestLeft.getPersistentDataContainer().remove(storeKey);
            chestLeft.update();

            chestRight.getPersistentDataContainer().remove(storeKey);
            chestRight.update();
            
            return;
        }

        chest.getPersistentDataContainer().remove(storeKey);
        chest.update();
    }

    private class ChestLockListener implements Listener {

        @EventHandler
        public void onInventoryOpen(InventoryOpenEvent event) {
            InventoryHolder holder = event.getInventory().getHolder();
            Chest chest = null;

            if (holder instanceof Chest) {
                chest = Chest.class.cast(holder);
            } else if (holder instanceof DoubleChest) {
                DoubleChest doublechest = DoubleChest.class.cast(holder);
                chest = ChestLock.this.getLockedChest(doublechest);
            } else {
                return;
            }

            Player player = null;
            if (event.getPlayer() instanceof Player) {
                player = Player.class.cast(event.getPlayer());
            }

            String owner = chest.getPersistentDataContainer().get(storeKey, PersistentDataType.STRING);

            // if the chest is locked and not accessed by the owner, prevent opening
            if (owner != null && (player == null || !owner.equals(player.getUniqueId().toString()))) {
                event.setCancelled(true);

                if (player != null) {
                    player.sendMessage(ChatColor.YELLOW + "Questa chest è bloccata");
                }

                getLogger().info(String.format("%s tried to open locked chest (by %s) at (%d %d %d).",
                    player.getName(),
                    getServer().getOfflinePlayer(UUID.fromString(owner)).getName(),
                    chest.getX(), chest.getY(), chest.getZ()));
            }
        }

        @EventHandler
        public void onPlayerInteract(PlayerInteractEvent event) {
            if (event.getAction() != Action.LEFT_CLICK_BLOCK
                || event.getItem() == null
                || event.getItem().getType() != Material.STICK
                || event.getClickedBlock().getType() != Material.CHEST
            ) {
                return;
            }

            Player player = event.getPlayer();
            Chest chest = Chest.class.cast(event.getClickedBlock().getState());

            if (chest.getInventory() instanceof DoubleChestInventory) {
                DoubleChest doublechest = DoubleChest.class.cast(chest.getInventory().getHolder());
                chest = ChestLock.this.getLockedChest(doublechest);
            }

            String owner = chest.getPersistentDataContainer().get(storeKey, PersistentDataType.STRING);

            if (owner == null) {
                lockChest(chest, player);
                player.sendMessage(ChatColor.GREEN + "Chest bloccata");
                return;
            }

            if (owner != null && owner.equals(player.getUniqueId().toString())) {
                unlockChest(chest);
                player.sendMessage(ChatColor.YELLOW + "Chest sbloccata");
                return;
            }
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            if (event.getBlock().getType() != Material.CHEST) {
                return;
            }

            Player player = event.getPlayer();
            Chest chest = Chest.class.cast(event.getBlock().getState());
            
            if (chest.getInventory() instanceof DoubleChestInventory) {
                DoubleChest doublechest = DoubleChest.class.cast(chest.getInventory().getHolder());
                chest = ChestLock.this.getLockedChest(doublechest);
            }

            String owner = chest.getPersistentDataContainer().get(storeKey, PersistentDataType.STRING);

            // prevent breaking if locked, except by the owner
            if (owner != null && !owner.equals(player.getUniqueId().toString())) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.YELLOW + "Questa chest è bloccata");

                getLogger().info(String.format("%s tried to break locked chest (by %s) at (%d %d %d).",
                    player.getName(),
                    getServer().getOfflinePlayer(UUID.fromString(owner)).getName(),
                    chest.getX(), chest.getY(), chest.getZ()));
            }
        }

        @EventHandler
        public void onEntityExplode(EntityExplodeEvent event) {
            List<Block> tmpBlockList = new ArrayList<>(event.blockList());
            
            for (Block block : tmpBlockList) {
                if (block.getType() != Material.CHEST) {
                    continue;
                }
    
                Chest chest = Chest.class.cast(block.getState());
                
                if (chest.getInventory() instanceof DoubleChestInventory) {
                    DoubleChest doublechest = DoubleChest.class.cast(chest.getInventory().getHolder());
                    chest = ChestLock.this.getLockedChest(doublechest);
                }
    
                String owner = chest.getPersistentDataContainer().get(storeKey, PersistentDataType.STRING);
    
                // prevent breaking if locked
                if (owner != null) {
                    event.blockList().remove(block);
    
                    getLogger().info(String.format("%s tried to explode locked chest (by %s) at (%d %d %d).",
                        event.getEntity().getName(),
                        getServer().getOfflinePlayer(UUID.fromString(owner)).getName(),
                        chest.getX(), chest.getY(), chest.getZ()));
                }
            }
        }

        @EventHandler
        public void onBlockPlace(BlockPlaceEvent event) {
            Block block = event.getBlock();

            if (block.getType() == Material.LAVA
                || block.getType() == Material.TNT
                || block.getType() == Material.TNT_MINECART
            ) {
                getLogger().info(String.format("%s placed %s at (%d %d %d).",
                    event.getPlayer().getName(),
                    block.getType().name(),
                    block.getX(), block.getY(), block.getZ()));
            }
        }

        @EventHandler
        public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
            Block block = event.getBlock();

            if (event.getBucket() == Material.LAVA_BUCKET) {
                getLogger().info(String.format("%s placed %s at (%d %d %d).",
                    event.getPlayer().getName(),
                    event.getBucket().name(),
                    block.getX(), block.getY(), block.getZ()));
            }
        }
    }
}
