/*
 * This file is part of Tribes, licensed under the ISC License.
 *
 * Copyright (c) 2015 Richard Harrah
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted,
 * provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package me.topplethenun.tribes.storage;

import me.topplethenun.tribes.TribesPlugin;
import me.topplethenun.tribes.data.Cell;
import me.topplethenun.tribes.data.Member;
import me.topplethenun.tribes.data.Tribe;
import me.topplethenun.tribes.math.Vec2;
import org.nunnerycode.facecore.database.MySqlDatabasePool;
import org.nunnerycode.facecore.logging.PluginLogger;
import org.nunnerycode.kern.io.CloseableRegistry;
import org.nunnerycode.kern.shade.google.common.base.Preconditions;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class MySQLDataStorage implements DataStorage {

    private static final String TR_CELLS_CREATE = "CREATE TABLE IF NOT EXISTS tr_cells (world VARCHAR(60) NOT NULL," +
            "x INT NOT NULL, z INT NOT NULL, owner VARCHAR(60), PRIMARY KEY (world, x, z))";
    private static final String TR_MEMBERS_CREATE = "CREATE TABLE IF NOT EXISTS tr_members (id VARCHAR(60) PRIMARY " +
            "KEY, score INT NOT NULL, tribe VARCHAR(60), rank VARCHAR(20))";
    private static final String TR_TRIBES_CREATE = "CREATE TABLE IF NOT EXISTS tr_tribes (id VARCHAR(60) PRIMARY " +
            "KEY, owner VARCHAR(60) NOT NULL, name VARCHAR(20) NOT NULL UNIQUE)";
    private final PluginLogger pluginLogger;
    private boolean initialized;
    private TribesPlugin plugin;
    private MySqlDatabasePool connectionPool;

    public MySQLDataStorage(TribesPlugin plugin) {
        this.plugin = plugin;
        this.pluginLogger = new PluginLogger(new File(plugin.getDataFolder(), "logs/mysql.log"));
        this.initialized = false;
    }

    private void createTable() throws SQLException {
        CloseableRegistry registry = new CloseableRegistry();
        Connection connection = registry.register(connectionPool.getConnection());

        Statement statement = registry.register(connection.createStatement());
        statement.executeUpdate(TR_CELLS_CREATE);
        statement.executeUpdate(TR_MEMBERS_CREATE);
        statement.executeUpdate(TR_TRIBES_CREATE);

        registry.closeQuietly();
    }

    private boolean tryQuery(Connection c, String query) {
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Statement statement = registry.register(c.createStatement());
            if (statement != null) {
                statement.executeQuery(query);
            }
            return true;
        } catch (SQLException e) {
            return false;
        } finally {
            registry.closeQuietly();
        }
    }

    @Override
    public void initialize() {
        if (initialized) {
            return;
        }

        connectionPool = new MySqlDatabasePool(plugin.getSettings().getString("db.host"), plugin.getSettings()
                .getString("db.port"), plugin.getSettings().getString("db.username"), plugin.getSettings().getString
                ("db.password"), plugin.getSettings().getString("db.database"));
        if (!connectionPool.initialize()) {
            return;
        }

        try {
            createTable();
            initialized = true;
            plugin.getPluginLogger().log(Level.INFO, "mysql initialized");
        } catch (SQLException ex) {
            plugin.getPluginLogger().log(Level.INFO, "unable to setup mysql");
        }
    }

    @Override
    public void shutdown() {
        // don't do anything
    }

    @Override
    public Set<Cell> loadCells() {
        Set<Cell> cells = new HashSet<>();
        Preconditions.checkState(initialized, "must be initialized");
        String query = "SELECT * FROM tr_cells";
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Connection c = registry.register(connectionPool.getConnection());
            Statement s = registry.register(c.createStatement());
            ResultSet rs = registry.register(s.executeQuery(query));
            while (rs.next()) {
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int z = rs.getInt("z");
                Vec2 vec2 = Vec2.fromCoordinates(worldName, x, z);
                String ownerString = rs.getString("owner");
                if (ownerString == null) {
                    cells.add(new Cell(vec2));
                } else {
                    UUID owner = UUID.fromString(ownerString);
                    cells.add(new Cell(vec2, owner));
                }
            }
        } catch (Exception e) {
            pluginLogger.log("unable to load cells: " + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
        return cells;
    }

    @Override
    public Set<Cell> loadCells(Iterable<Vec2> vec2s) {
        Set<Cell> cells = new HashSet<>();
        Preconditions.checkState(initialized, "must be initialized");
        String query = "SELECT * FROM tr_cells WHERE world=? AND x=? AND z=?";
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Connection c = registry.register(connectionPool.getConnection());
            PreparedStatement statement = registry.register(c.prepareStatement(query));
            for (Vec2 vec : vec2s) {
                statement.setString(1, vec.getWorld().getName());
                statement.setInt(2, vec.getX());
                statement.setInt(3, vec.getZ());
                ResultSet rs = registry.register(statement.executeQuery());
                while (rs.next()) {
                    String worldName = rs.getString("world");
                    int x = rs.getInt("x");
                    int z = rs.getInt("z");
                    Vec2 vec2 = Vec2.fromCoordinates(worldName, x, z);
                    String ownerString = rs.getString("owner");
                    if (ownerString == null) {
                        cells.add(new Cell(vec2));
                    } else {
                        UUID owner = UUID.fromString(ownerString);
                        cells.add(new Cell(vec2, owner));
                    }
                }
            }
        } catch (Exception e) {
            pluginLogger.log("unable to load cells: " + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
        return cells;
    }

    @Override
    public Set<Cell> loadCells(Vec2... vec2s) {
        return loadCells(Arrays.asList(vec2s));
    }

    @Override
    public void saveCells(Iterable<Cell> cellIterable) {
        Preconditions.checkNotNull(cellIterable, "cellIterable cannot be null");
        String query = "REPLACE INTO tr_cells (world, x, z, owner) VALUES (?,?,?,?)";
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Connection c = registry.register(connectionPool.getConnection());
            PreparedStatement statement = registry.register(c.prepareStatement(query));
            for (Cell cell : cellIterable) {
                statement.setString(1, cell.getLocation().getWorld().getName());
                statement.setInt(2, cell.getLocation().getX());
                statement.setInt(3, cell.getLocation().getZ());
                statement.setString(4, cell.getOwner().toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            pluginLogger.log("unable to save cells: " + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
    }

    @Override
    public Set<Member> loadMembers() {
        Set<Member> members = new HashSet<>();
        String query = "SELECT * FROM tr_members";
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Connection connection = registry.register(connectionPool.getConnection());
            Statement statement = registry.register(connection.createStatement());
            ResultSet resultSet = registry.register(statement.executeQuery(query));
            while (resultSet.next()) {
                Member member = new Member(UUID.fromString(resultSet.getString("id")));
                member.setScore(resultSet.getInt("score"));
                member.setTribe(UUID.fromString(resultSet.getString("tribe")));
                member.setRank(Tribe.Rank.fromString(resultSet.getString("rank")));
                members.add(member);
            }
        } catch (SQLException e) {
            pluginLogger.log("unable to load members:" + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
        return members;
    }

    @Override
    public Set<Member> loadMembers(Iterable<UUID> uuids) {
        Set<Member> members = new HashSet<>();
        Preconditions.checkState(initialized, "must be initialized");
        String query = "SELECT * FROM tr_members WHERE id=?";
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Connection c = registry.register(connectionPool.getConnection());
            PreparedStatement statement = registry.register(c.prepareStatement(query));
            for (UUID uuid : uuids) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = registry.register(statement.executeQuery());
                while (resultSet.next()) {
                    Member member = new Member(UUID.fromString(resultSet.getString("id")));
                    member.setScore(resultSet.getInt("score"));
                    member.setTribe(UUID.fromString(resultSet.getString("tribe")));
                    member.setRank(Tribe.Rank.fromString(resultSet.getString("rank")));
                    members.add(member);
                }
            }
        } catch (Exception e) {
            pluginLogger.log("unable to load members: " + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
        return members;
    }

    @Override
    public Set<Member> loadMembers(UUID... uuids) {
        return loadMembers(Arrays.asList(uuids));
    }

    @Override
    public void saveMembers(Iterable<Member> memberIterable) {
        Preconditions.checkNotNull(memberIterable, "memberIterable cannot be null");
        CloseableRegistry registry = new CloseableRegistry();
        String query = "REPLACE INTO tr_members (id, score, tribe, rank) VALUES (?,?,?,?)";
        try {
            Connection connection = registry.register(connectionPool.getConnection());
            PreparedStatement statement = registry.register(connection.prepareStatement(query));
            for (Member member : memberIterable) {
                statement.setString(1, member.getUniqueId().toString());
                statement.setInt(2, member.getScore());
                if (member.getTribe() == null) {
                    statement.setNull(3, Types.VARCHAR);
                } else {
                    statement.setString(3, member.getTribe().toString());
                }
                statement.setString(4, member.getRank().name());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            pluginLogger.log("unable to save members: " + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
    }

    @Override
    public Set<Tribe> loadTribes() {
        Set<Tribe> tribes = new HashSet<>();
        String query = "SELECT * FROM tr_tribes";
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Connection connection = registry.register(connectionPool.getConnection());
            Statement statement = registry.register(connection.createStatement());
            ResultSet resultSet = registry.register(statement.executeQuery(query));
            while (resultSet.next()) {
                Tribe tribe = new Tribe(UUID.fromString(resultSet.getString("id")));
                tribe.setOwner(UUID.fromString(resultSet.getString("owner")));
                tribe.setName(resultSet.getString("name"));
                tribes.add(tribe);
            }
        } catch (SQLException e) {
            pluginLogger.log("unable to load tribes: " + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
        return tribes;
    }

    @Override
    public Set<Tribe> loadTribes(Iterable<UUID> uuids) {
        Set<Tribe> tribes = new HashSet<>();
        String query = "SELECT * FROM tr_tribes WHERE id=?";
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Connection connection = registry.register(connectionPool.getConnection());
            PreparedStatement statement = registry.register(connection.prepareStatement(query));
            for (UUID uuid : uuids) {
                statement.setString(1, uuid.toString());
                ResultSet resultSet = registry.register(statement.executeQuery());
                while (resultSet.next()) {
                    Tribe tribe = new Tribe(UUID.fromString(resultSet.getString("id")));
                    tribe.setOwner(UUID.fromString(resultSet.getString("owner")));
                    tribe.setName(resultSet.getString("name"));
                    tribes.add(tribe);
                }
            }
        } catch (SQLException e) {
            pluginLogger.log("unable to load tribes: " + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
        return tribes;
    }

    @Override
    public Set<Tribe> loadTribes(UUID... uuids) {
        return loadTribes(Arrays.asList(uuids));
    }

    @Override
    public void saveTribes(Iterable<Tribe> tribeIterable) {
        Preconditions.checkNotNull(tribeIterable);
        String query = "REPLACE INTO tr_tribes (id, owner, name) VALUES (?,?, ?)";
        CloseableRegistry registry = new CloseableRegistry();
        try {
            Connection connection = registry.register(connectionPool.getConnection());
            PreparedStatement statement = registry.register(connection.prepareStatement(query));
            for (Tribe tribe : tribeIterable) {
                statement.setString(1, tribe.getUniqueId().toString());
                if (tribe.getOwner() == null) {
                    statement.setNull(2, Types.VARCHAR);
                } else {
                    statement.setString(2, tribe.getOwner().toString());
                }
                statement.setString(3, tribe.getName());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            pluginLogger.log("unable to save tribes: " + e.getMessage());
        } finally {
            registry.closeQuietly();
        }
    }

    private String getConnectionURI() {
        return "jdbc:mysql://" + plugin.getSettings().getString("db.host") + ":" + plugin.getSettings().getString("db" +
                ".port") + "/" + plugin.getSettings().getString("db.database");
    }

}
