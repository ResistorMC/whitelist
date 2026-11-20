package me.minecraft.resistor.whitelist.database.sqlite;

import me.minecraft.resistor.whitelist.Whitelist;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLiteDataBase {
    private String url;
    private Connection conn;

    private SQLiteWhitelistTable sqLiteTable;

    private Whitelist plugin;

    public SQLiteDataBase(File folder, Whitelist plugin) throws SQLException {
        this.plugin = plugin;

        if (!folder.exists()) {
            folder.mkdirs();
        }

        File dbFile = new File(folder, "database.db");
        url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        conn = DriverManager.getConnection(url);

        sqLiteTable = new SQLiteWhitelistTable(conn, plugin);

        createTable();
    }

    private void createTable() throws SQLException {
        try {
            sqLiteTable.createTable();

            plugin.getLogger().info( "Таблица белого списка успешно создана/проверена");
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка создания таблицы белого списка " + e);

            close();
            throw e;
        }
    }

    public List<String> getPlayers() throws SQLException {
        return sqLiteTable.getWhitelist();
    }

    public Map<String,Long> getPlayerInfo(String nick) throws SQLException {
        return sqLiteTable.getInfoPlayer(nick);
    }

    public Set<String> checkInactive(int day) throws SQLException {
        return sqLiteTable.checkInactive(day);
    }

    public int setRest(String nick, int day, int maxday) throws SQLException {
        return sqLiteTable.setRest(nick,day,maxday);
    }

    public int cancelRest(String nick) throws SQLException {
        return sqLiteTable.cancelRest(nick);
    }

    public Set<String> checkRest() throws SQLException {
        return sqLiteTable.checkRest();
    }

    public int addPlayer(String nick) throws SQLException {
        return sqLiteTable.addPlayer(nick);
    }

    public int removePlayer(String nick) throws SQLException {
        return sqLiteTable.removePlayer(nick);
    }

    public int updateLastSeen(String nick) throws SQLException {
        return sqLiteTable.updateLastSeen(nick);
    }

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка закрытия коннекта sqlite: " + e);
        }
    }
}
