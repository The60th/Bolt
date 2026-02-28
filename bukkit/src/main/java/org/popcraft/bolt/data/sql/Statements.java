package org.popcraft.bolt.data.sql;

public enum Statements {
    CREATE_TABLE_BLOCKS(
            "CREATE TABLE IF NOT EXISTS %sblocks (id varchar(36) PRIMARY KEY, lock_id varchar(36), created integer, accessed integer, jammed_until integer, tier integer DEFAULT 1, world varchar(128), x integer, y integer, z integer, block varchar(128));",
            "CREATE TABLE IF NOT EXISTS %sblocks (id varchar(36) PRIMARY KEY, lock_id varchar(36), created bigint, accessed bigint, jammed_until bigint, tier integer DEFAULT 1, world varchar(128), x integer, y integer, z integer, block varchar(128), INDEX block_lock_id(lock_id), INDEX block_location(world, x, y, z));"
    ),
    CREATE_TABLE_ENTITIES(
            "CREATE TABLE IF NOT EXISTS %sentities (id varchar(36) PRIMARY KEY, lock_id varchar(36), created integer, accessed integer, jammed_until integer, tier integer DEFAULT 1, entity varchar(128));",
            "CREATE TABLE IF NOT EXISTS %sentities (id varchar(36) PRIMARY KEY, lock_id varchar(36), created bigint, accessed bigint, jammed_until bigint, tier integer DEFAULT 1, entity varchar(128), INDEX entity_lock_id(lock_id));"
    ),
    CREATE_INDEX_BLOCK_LOCK_ID(
            "CREATE INDEX IF NOT EXISTS block_lock_id ON %sblocks(lock_id);"
    ),
    CREATE_INDEX_BLOCK_LOCATION(
            "CREATE UNIQUE INDEX IF NOT EXISTS block_location ON %sblocks(world, x, y, z);"
    ),
    CREATE_INDEX_ENTITY_LOCK_ID(
            "CREATE INDEX IF NOT EXISTS entity_lock_id ON %sentities(lock_id);"
    ),
    SELECT_BLOCK_BY_LOCATION(
            "SELECT * FROM %sblocks WHERE world = ? AND x = ? AND y = ? AND z = ?;"
    ),
    SELECT_ALL_BLOCKS(
            "SELECT * FROM %sblocks;"
    ),
    REPLACE_BLOCK(
            "REPLACE INTO %sblocks (id, lock_id, created, accessed, jammed_until, tier, world, x, y, z, block) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    ),
    DELETE_BLOCK(
            "DELETE FROM %sblocks WHERE id = ?;"
    ),
    SELECT_ENTITY_BY_UUID(
            "SELECT * FROM %sentities WHERE id = ?;"
    ),
    SELECT_ALL_ENTITIES(
            "SELECT * FROM %sentities;"
    ),
    REPLACE_ENTITY(
            "REPLACE INTO %sentities (id, lock_id, created, accessed, jammed_until, tier, entity) VALUES (?, ?, ?, ?, ?, ?, ?);"
    ),
    DELETE_ENTITY(
            "DELETE FROM %sentities WHERE id = ?;"
    ),
    ALTER_TABLE_BLOCKS_ADD_TIER(
            "ALTER TABLE %sblocks ADD COLUMN tier integer DEFAULT 1;"
    ),
    ALTER_TABLE_ENTITIES_ADD_TIER(
            "ALTER TABLE %sentities ADD COLUMN tier integer DEFAULT 1;"
    );

    private final String sqlite;
    private final String mysql;

    Statements(final String sqlite, final String mysql) {
        this.sqlite = sqlite;
        this.mysql = mysql;
    }

    Statements(final String sqlite) {
        this.sqlite = sqlite;
        this.mysql = sqlite;
    }

    public String get(final String type) {
        return "sqlite".equals(type) ? sqlite : mysql;
    }
}
