package me.minecraft.resistor.whitelist.inactiveChecker;

import me.minecraft.resistor.whitelist.Whitelist;
import me.minecraft.resistor.whitelist.config.Config;
import me.minecraft.resistor.whitelist.database.sqlite.SQLiteCache;
import me.minecraft.resistor.whitelist.database.sqlite.SQLiteDataBase;
import org.bukkit.Bukkit;

import java.sql.SQLException;

public class InactiveChecker implements Runnable {
    private Whitelist plugin;
    private SQLiteDataBase sqldb;
    private SQLiteCache sqlcache;
    private Config config;

    public InactiveChecker(Whitelist plugin, SQLiteDataBase sqLiteDataBase, SQLiteCache sqlcache, Config config) {
        this.plugin = plugin;
        sqldb = sqLiteDataBase;
        this.sqlcache = sqlcache;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            sqldb.checkRest();
            for (String nick : sqldb.checkInactive(config.getDay())) {
                if (Bukkit.getPlayerExact(nick) != null) {
                    continue;
                }

                if (sqlcache.removePlayer(nick) > 0) {
                    plugin.getLogger().info("Игрок; " + nick + " удален из белого списка за неактив");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка проверки неактивности; " + e);
        }
    }

}
