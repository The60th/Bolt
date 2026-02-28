package org.popcraft.bolt.protection;

import java.util.UUID;

public final class EntityProtection extends Protection {
    private String entity;

    public EntityProtection(UUID id, UUID lockId, long created, long accessed, long jammedUntil, String entity) {
        super(id, lockId, created, accessed, jammedUntil);
        this.entity = entity;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    @Override
    public String toString() {
        return "EntityProtection{" +
                "id=" + id +
                ", lockId=" + lockId +
                ", created=" + created +
                ", accessed=" + accessed +
                ", jammedUntil=" + jammedUntil +
                ", entity='" + entity + '\'' +
                '}';
    }
}
