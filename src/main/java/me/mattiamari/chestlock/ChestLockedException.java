package me.mattiamari.chestlock;

import org.bukkit.OfflinePlayer;

public class ChestLockedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private OfflinePlayer lockedBy;

    public ChestLockedException(OfflinePlayer lockedBy) {
        super("Chest is locked");
        this.lockedBy = lockedBy;
    }

    public OfflinePlayer getLockedBy() {
        return lockedBy;
    }
}
