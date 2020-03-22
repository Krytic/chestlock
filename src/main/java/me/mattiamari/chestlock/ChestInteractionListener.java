package me.mattiamari.chestlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

public class ChestInteractionListener implements Listener {
    private ChestLock plugin;

    public ChestInteractionListener(ChestLock plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        Chest chest = null;

        if (holder instanceof Chest) {
            chest = Chest.class.cast(holder);
        } else if (holder instanceof DoubleChest) {
            DoubleChest doublechest = DoubleChest.class.cast(holder);
            chest = Chest.class.cast(doublechest.getLeftSide());
        } else {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = Player.class.cast(event.getPlayer());
        LockableChest lchest = new LockableChest(chest, plugin);

        // if the chest is locked and not accessed by the owner or allowed players, prevent opening
        if (lchest.isLocked() && !lchest.isAllowed(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.YELLOW + "Questa chest è bloccata");

            plugin.getLogger().info(String.format("%s tried to open locked chest (by %s) at (%d %d %d).",
                player.getName(),
                lchest.getOwner().getName(),
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
        LockableChest lchest = new LockableChest(chest, plugin);

        if (!lchest.isLocked()) {
            lchest.lock(player);
            player.sendMessage(ChatColor.GREEN + "Chest bloccata");
        } else if (lchest.isOwner(player)) {
            lchest.unlock(player);
            player.sendMessage(ChatColor.YELLOW + "Chest sbloccata");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (block.getType() == Material.CHEST) {
            Chest chest = Chest.class.cast(block.getState());
            LockableChest lchest = new LockableChest(chest, plugin);
            
            // prevent breaking if locked, except by the owner
            if (lchest.isLocked() && !lchest.isOwner(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.YELLOW + "Questa chest è bloccata");

                plugin.getLogger().info(String.format("%s tried to break locked chest (by %s) at (%d %d %d).",
                    player.getName(),
                    lchest.getOwner().getName(),
                    chest.getX(), chest.getY(), chest.getZ()));
            }
        }
        
        if (ChestLock.isSign(block)) {
            Sign sign = Sign.class.cast(block.getState());
            Optional<Chest> chest = ChestLock.getChestOfSign(sign);

            if (!chest.isPresent()) {
                return;
            }

            LockableChest lchest = new LockableChest(chest.get(), plugin);

            // prevent breaking if locked, except by the owner
            if (lchest.isLocked() && !lchest.isOwner(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.YELLOW + "Questa chest è bloccata");

                plugin.getLogger().info(String.format("%s tried to break locked chest (by %s) at (%d %d %d).",
                    player.getName(),
                    lchest.getOwner().getName(),
                    sign.getX(), sign.getY(), sign.getZ()));
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> tmpBlockList = new ArrayList<>(event.blockList());
        
        for (Block block : tmpBlockList) {
            if (block.getType() == Material.CHEST) {
                Chest chest = Chest.class.cast(block.getState());
                LockableChest lchest = new LockableChest(chest, plugin);

                if (lchest.isLocked()) {
                    event.blockList().remove(block);

                    plugin.getLogger().info(String.format("%s tried to explode locked chest (by %s) at (%d %d %d).",
                        event.getEntity().getName(),
                        lchest.getOwner().getName(),
                        chest.getX(), chest.getY(), chest.getZ()));
                }
            }

            if (ChestLock.isSign(block)) {
                Sign sign = Sign.class.cast(block.getState());
                Optional<Chest> chest = ChestLock.getChestOfSign(sign);
                
                if (!chest.isPresent()) {
                    continue;
                }

                LockableChest lchest = new LockableChest(chest.get(), plugin);

                if (lchest.isLocked()) {
                    event.blockList().remove(block);
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (!ChestLock.isSign(block)) {
            return;
        }

        Sign sign = Sign.class.cast(block.getState());
        Optional<Chest> chest = ChestLock.getChestOfSign(sign);

        if (!chest.isPresent()) {
            return;
        }

        Player player = event.getPlayer();
        LockableChest lchest = new LockableChest(chest.get(), plugin);

        // prevent placing a sign if the chest is locked, except by the owner
        if (lchest.isLocked() && !lchest.isOwner(player)) {
            event.setCancelled(true);
        }   
    }
}
