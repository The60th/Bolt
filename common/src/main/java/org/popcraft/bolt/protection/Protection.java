package org.popcraft.bolt.protection;

import java.util.UUID;

public sealed abstract class Protection permits BlockProtection, EntityProtection {
    protected final UUID id;
    protected UUID lockId;
    protected long created;
    protected long accessed;
    protected long jammedUntil;
    protected int tier;

    protected Protection(UUID id, UUID lockId, long created, long accessed, long jammedUntil) {
        this(id, lockId, created, accessed, jammedUntil, 1);
    }

    protected Protection(UUID id, UUID lockId, long created, long accessed, long jammedUntil, int tier) {
        this.id = id;
        this.lockId = lockId;
        this.created = created;
        this.accessed = accessed;
        this.jammedUntil = jammedUntil;
        this.tier = tier;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLockId() {
        return lockId;
    }

    public void setLockId(UUID lockId) {
        this.lockId = lockId;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getAccessed() {
        return accessed;
    }

    public void setAccessed(long accessed) {
        this.accessed = accessed;
    }

    public long getJammedUntil() {
        return jammedUntil;
    }

    public void setJammedUntil(long jammedUntil) {
        this.jammedUntil = jammedUntil;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public boolean isJammed() {
        return jammedUntil > System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Protection{" +
                "id=" + id +
                ", lockId=" + lockId +
                ", created=" + created +
                ", accessed=" + accessed +
                ", jammedUntil=" + jammedUntil +
                ", tier=" + tier +
                '}';
    }
}
