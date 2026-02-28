package org.popcraft.bolt.protection;

import java.util.UUID;

public final class BlockProtection extends Protection {
    private String world;
    private int x;
    private int y;
    private int z;
    private String block;

    public BlockProtection(UUID id, UUID lockId, long created, long accessed, long jammedUntil, String world, int x, int y, int z, String block) {
        super(id, lockId, created, accessed, jammedUntil);
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.block = block;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public String getBlock() {
        return block;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    @Override
    public String toString() {
        return "BlockProtection{" +
                "id=" + id +
                ", lockId=" + lockId +
                ", created=" + created +
                ", accessed=" + accessed +
                ", jammedUntil=" + jammedUntil +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", block='" + block + '\'' +
                '}';
    }
}
