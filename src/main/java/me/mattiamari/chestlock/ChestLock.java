package me.mattiamari.chestlock;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class ChestLock extends JavaPlugin {
    protected NamespacedKey ownerKey = new NamespacedKey(this, "chest_owner");

    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ChestInteractionListener(this), this);
    }

    protected static Boolean isSign(Block block) {
        Material mat = block.getType();

        return mat == Material.ACACIA_WALL_SIGN
            || mat == Material.BIRCH_WALL_SIGN
            || mat == Material.DARK_OAK_WALL_SIGN
            || mat == Material.JUNGLE_WALL_SIGN
            || mat == Material.OAK_WALL_SIGN
            || mat == Material.SPRUCE_WALL_SIGN;
    }

    protected static boolean isDoubleChest(Chest chest) {
        return chest.getInventory() instanceof DoubleChestInventory;
    }

    protected static Optional<Sign> getSignOfChest(Chest chest) {
        BlockFace chestFace = Directional.class.cast(chest.getBlockData()).getFacing();
        Block signBlock = chest.getBlock().getRelative(chestFace);

        if (!isSign(signBlock)) {
            return Optional.empty();
        }

        return Optional.of(Sign.class.cast(signBlock.getState()));
    }

    protected static Optional<Chest> getChestOfSign(Sign sign) {
        BlockFace signFace = Directional.class.cast(sign.getBlockData()).getFacing();
        Block chestBlock = sign.getBlock().getRelative(signFace.getOppositeFace());
        
        if (chestBlock.getType() != Material.CHEST) {
            return Optional.empty();
        }

        return Optional.of(Chest.class.cast(chestBlock.getState()));
    }
}
