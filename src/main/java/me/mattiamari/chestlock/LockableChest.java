package me.mattiamari.chestlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class LockableChest {
    private ChestLock plugin;
    private NamespacedKey ownerKey;
    private Chest chest;
    private Optional<Chest> slaveChest = Optional.empty();
    private List<Sign> chestSigns = new ArrayList<>();
    private Optional<OfflinePlayer> owner = Optional.empty();
    private List<Player> allowedPlayers;

    public LockableChest(Chest chest, ChestLock plugin) {
        this.plugin = plugin;
        this.ownerKey = plugin.ownerKey;

        splitDoubleChest(chest);

        // Get signs attached to the chest
        Optional<Sign> sign = ChestLock.getSignOfChest(this.chest);
        if (sign.isPresent()) {
            chestSigns.add(sign.get());
        }

        if (slaveChest.isPresent()) {
            sign = ChestLock.getSignOfChest(slaveChest.get());
            if (sign.isPresent()) {
                chestSigns.add(sign.get());
            }
        }

        allowedPlayers = chestSigns.stream()
            .flatMap(s -> getPlayersFromSign(s).stream())
            .collect(Collectors.toList());

        if (this.chest.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
            UUID ownerUUID = UUID.fromString(this.chest.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING));
            owner = Optional.of(plugin.getServer().getOfflinePlayer(ownerUUID));
        }
    }

    public boolean isLocked() {
        return owner.isPresent();
    }

    public OfflinePlayer getOwner() {
        return owner.get();
    }

    public void lock(Player player) {
        if (isLocked()) {
            throw new ChestLockedException(owner.get());
        }
        
        String uuid = player.getUniqueId().toString();

        chest.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, uuid);
        chest.update();

        if (slaveChest.isPresent()) {
            slaveChest.get().getPersistentDataContainer()
                .set(ownerKey, PersistentDataType.STRING, uuid);
            slaveChest.get().update();
        }
    }

    public void unlock(Player player) {
        if (!isLocked()) {
            return;
        }
        if (!isOwner(player)) {
            throw new ChestLockedException(owner.get());
        }
        
        chest.getPersistentDataContainer().remove(ownerKey);
        chest.update();

        if (slaveChest.isPresent()) {
            slaveChest.get().getPersistentDataContainer().remove(ownerKey);
            slaveChest.get().update();
        }
    }

    public void toggleLock(Player player) {
        if (isLocked()) {
            unlock(player);
            return;
        }

        lock(player);
    }

    public boolean isOwner(Player player) {
        return isLocked()
            && owner.get().getUniqueId().equals(player.getUniqueId());
    }

    public boolean isAllowed(Player player) {
        return !isLocked()
            || isOwner(player)
            || allowedPlayers.stream().anyMatch(e -> e.getUniqueId().equals(player.getUniqueId()));
    }

    private void splitDoubleChest(Chest chest) {
        if (!ChestLock.isDoubleChest(chest)) {
            this.chest = chest;
            return;
        }

        DoubleChest dchest = DoubleChest.class.cast(chest.getInventory().getHolder());
        Chest chestLeft = Chest.class.cast(dchest.getLeftSide());
        Chest chestRight = Chest.class.cast(dchest.getRightSide());

        if (chestLeft.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
            this.chest = chestLeft;
            this.slaveChest = Optional.of(chestRight);
        } else if (chestRight.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING)) {
            this.chest = chestRight;
            this.slaveChest = Optional.of(chestLeft);
        } else {
            this.chest = chestLeft;
            this.slaveChest = Optional.of(chestRight);
        }
    }

    private List<Player> getPlayersFromSign(Sign sign) {
        return Arrays.asList(sign.getLines()).stream()
            .filter(e -> e != "")
            .map(name -> plugin.getServer().getPlayerExact(name))
            .filter(e -> e != null)
            .collect(Collectors.toList());
    }
}
