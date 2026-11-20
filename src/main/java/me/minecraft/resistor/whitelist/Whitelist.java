package me.minecraft.resistor.whitelist;

import me.minecraft.resistor.whitelist.command.Command;
import me.minecraft.resistor.whitelist.config.Config;
import me.minecraft.resistor.whitelist.database.sqlite.SQLiteCache;
import me.minecraft.resistor.whitelist.database.sqlite.SQLiteDataBase;
import me.minecraft.resistor.whitelist.inactiveChecker.InactiveChecker;
import me.minecraft.resistor.whitelist.playerHandler.PlayerHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.logging.Level;

public final class Whitelist extends JavaPlugin {
    public SQLiteDataBase sqLiteDataBase;
    public SQLiteCache sqLiteCache;
    public Config config;
    private PlayerHandler playerHandler;
    private BukkitTask bukkitTask;


    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("------------------------------------");
        getLogger().info(" ");
        getLogger().info("Plugin on enabled");
        getLogger().info(" ");
        getLogger().info("------------------------------------");



        config = new Config(this);

        try {
            sqLiteDataBase = new SQLiteDataBase(getDataFolder(), this);
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "error connection database SQLite: " + e);
            shutdownServer();
            return;
        }

        try {
            sqLiteCache = new SQLiteCache(sqLiteDataBase, this);
        } catch (SQLException e) {
            getLogger().log(Level.WARNING, "White list loading error " + e);
            shutdownServer();
            return;
        }

        Command command = new Command(this);

        getCommand("wh").setExecutor(command);

        startChecker();
        playerHandler = new PlayerHandler(this, sqLiteCache, sqLiteDataBase);
        Bukkit.getPluginManager().registerEvents(playerHandler, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("------------------------------------");
        getLogger().info(" ");
        getLogger().info("Plugin on disable");
        getLogger().info(" ");
        getLogger().info("------------------------------------");

        if (sqLiteDataBase != null) sqLiteDataBase.close();
    }



    public void shutdownServer() {
        getLogger().log(Level.WARNING,
                "----------------------------------------------\n \nPLUGIN ON DISABLE\n \n----------------------------------------------"
        );

        getServer().shutdown();
    }

    private void startChecker() {
        if (bukkitTask != null) {
            bukkitTask.cancel();
        }

        long checknum = config.getCheck() * 60L * 20;

        bukkitTask = Bukkit.getScheduler().runTaskTimer(this,
                new InactiveChecker(this, sqLiteDataBase, sqLiteCache, config), 20L * 30, checknum);
    }

    public void reloadPlugin(CommandSender sender) {
        config.reloadConfig();

        SQLiteDataBase newbd;
        try {
            newbd = new SQLiteDataBase(getDataFolder(), this);
        } catch (SQLException e) {
            sender.sendMessage("Ошибка при перезагрузке проверьте логи");
            getLogger().warning("error connection database SQLite: " + e);
            return;
        }

        SQLiteCache sqlCache;
        try {
            sqlCache = new SQLiteCache(newbd, this);
        } catch (SQLException e) {
            sender.sendMessage("Ошибка при перезагрузке проверьте логи");
            getLogger().warning("White list loading error " + e);
            newbd.close();
            return;
        }

        sqLiteDataBase.close();
        sqLiteDataBase = newbd;
        sqLiteCache = sqlCache;

        startChecker();

        HandlerList.unregisterAll(playerHandler);
        playerHandler = new PlayerHandler(this, sqLiteCache, sqLiteDataBase);
        Bukkit.getPluginManager().registerEvents(playerHandler, this);

        sender.sendMessage("Плагин успешно перезагружен");
    }
}

