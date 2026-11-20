package me.minecraft.resistor.whitelist.database.sqlite;

import me.minecraft.resistor.whitelist.Whitelist;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SQLiteCache {
    private Whitelist plugin;
    private SQLiteDataBase sqldb;

    private final Set<String> whitelist = ConcurrentHashMap.newKeySet();

    public SQLiteCache(SQLiteDataBase sqLiteDataBase, Whitelist plugin) throws SQLException {
        this.plugin = plugin;
        sqldb = sqLiteDataBase;

        enable();
    }
    // 0 - false
    // !0 - true
    // -1 - error
    public int enable() throws SQLException {
        whitelist.addAll(sqldb.getPlayers());

        return 1;
    }

    public List<String> getPlayers() {
        return new ArrayList<>(whitelist);
    }
    public int isAllowed(String nick) {
        nick = nick.toLowerCase(Locale.ROOT);

        if (whitelist.contains(nick)) {
            return 1;
        } else
            return 0;
    }

    public int addPlayer(String nick) {
        nick = nick.toLowerCase(Locale.ROOT);
        try {
            int num = sqldb.addPlayer(nick);
            whitelist.add(nick);
            return num;
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка добавления игрока " + nick + " в белый список error: " + e);
            return -1;
        }
    }

    public int removePlayer(String nick) {
        nick = nick.toLowerCase(Locale.ROOT);
        try {
            int num = sqldb.removePlayer(nick);
            whitelist.remove(nick);
            return num;
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка удаления игрока " +  nick + " из белого списка error: " + e);
            return -1;
        }
    }
}
