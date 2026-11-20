package me.minecraft.resistor.whitelist.playerHandler;

import me.minecraft.resistor.whitelist.Whitelist;
import me.minecraft.resistor.whitelist.database.sqlite.SQLiteCache;
import me.minecraft.resistor.whitelist.database.sqlite.SQLiteDataBase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.SQLException;
import java.util.Locale;


public class PlayerHandler implements Listener {
    private Whitelist plugin;
    private SQLiteCache sqLiteCache;
    private SQLiteDataBase sqliteDB;

    public PlayerHandler(Whitelist plugin, SQLiteCache sqLiteCache, SQLiteDataBase sqLiteDataBase) {
        this.plugin = plugin;
        this.sqLiteCache = sqLiteCache;
        sqliteDB = sqLiteDataBase;
    }

    @EventHandler
    public void playerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        String nick = player.getName();
        if (sqLiteCache.isAllowed(nick) != 1) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, plugin.config.getMessage("message.kickMessage"));
        }
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        try {
            sqliteDB.updateLastSeen(player.getName().toLowerCase(Locale.ROOT));
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка записи последней активности игрока " + player.getName() + ". error: " + e);
        }
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        try {
            sqliteDB.updateLastSeen(player.getName().toLowerCase(Locale.ROOT));
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка записи последней активности игрока " + player.getName() + ". error: " + e);
        }
    }
}
