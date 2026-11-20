package me.minecraft.resistor.whitelist.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static final Map<String,String> message = new LinkedHashMap<>();
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private FileConfiguration config;
    private Plugin plugin;

    public Config(Plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();

        setupConfig();
    }

    static {
        message.put("message.pluginName", "Whitelist");
        message.put("message.kickMessage", "<red>Вас нет в белом списке</red>");
        message.put("message.addPlayer.errorFormat", "<green>Whitelist: </green><white>невозможно добавить игрока <nick>. Разрешенный формат ник 3-16 символов, только буквы, цифры и _ </white>");
        message.put("message.addPlayer.successAdded", "<green>Whitelist: </green><white>игрок <nick> успешно добавлен в белый список!</white>");
        message.put("message.addPlayer.alreadyBe", "<green>Whitelist: </green><white>игрок <nick> уже находится в белом списке!</white>");
        message.put("message.addPlayer.couldNotAdd", "<red>Whitelist: </red><white>игрока <nick> не удалось добавить в белый список! Проверьте консоль</white>");

        message.put("message.deletePlayer.successDeleted", "<green>Whitelist: </green><white>игрок <nick> успешно удален из белого списка!</white>");
        message.put("message.deletePlayer.alreadyBe", "<green>Whitelist: </green><white>игрока <nick> нету в белом списке!</white>");
        message.put("message.deletePlayer.couldNotAdd", "<red>Whitelist: </red><white>игрока <nick> не удалось удалить из белого списка! Проверьте консоль</white>");
        message.put("message.deletePlayer.kickMessage", "<green>Whitelist: </green><white>вы были исключены из белого списка!</white>");

        message.put("message.rest.successCancel", "<green>Whitelist: </green><white>рест игрока <nick> успешно отменен! </white>");
        message.put("message.rest.cancelError", "<red>Whitelist: </red><white>не удалось отменить рест игроку <nick>! Проверьте консоль! </white>");
        message.put("message.rest.nicknameDoesntExist", "<green>Whitelist: </green><white>игрока <nick> нет в белом списке.");
        message.put("message.rest.successfullyIssued", "<green>Whitelist: </green><white>игроку <nick> успешно выдан рест на <day> дней!</white>");
        message.put("message.rest.periodTooLong", "<green>Whitelist: </green><white>слишком большой срок рест (выше установленного максимума)</white>");
        message.put("message.rest.periodTooShort", "<green>Whitelist: </green><white>слишком маленький срок реста (меньше 0)</white>");
    }

    private void setupConfig() {

        if (!config.contains("inactive.day")) {
            config.set("inactive.day", 30);
        }
        if (!config.contains("inactive.check")) {
            config.set("inactive.check", 60);
        }
        if (!config.contains("rest.maxDay")) {
            config.set("rest.maxDay", 60);
        }


        message.forEach((path,text) -> {
            if (!config.contains(path))
                config.set(path,text);
        });
        config.options().copyDefaults(true);

        config.setComments("inactive.check", List.of("Промежуток времени после которого будет произведена проверка (в минутах)"));
        config.setComments("inactive.day", List.of("Количество дней которое пользователь может быть неактивным"));
        config.setComments("rest.maxDay", List.of("Максимальный срок на который можно выдать рест"));

        config.setComments("message.pluginName",List.of("Что плагин будет писать перед выводом ошибки (цвет красный неизменяемый)"));
        config.setComments("message.kickMessage", List.of("Сообщение которое будет выведено игроку не находящемуся в белом списке при попытке входа"));


        plugin.saveConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public int getDay() {
        int num = config.getInt("inactive.day");

        if (num > 0)
            return num;
        else {
            plugin.getLogger().warning("Недопустимое значние inactive.day. Установлено дефолтное - 30 дней");
            return 30;
        }
    }

    public int getCheck() {
        int num = config.getInt("inactive.check");

        if (num > 0)
            return num;
        else {
            plugin.getLogger().warning("Недопустимое значние inactive.check. Установлено дефолтное - 60 минут");
            return 60;
        }
    }

    public int getMaxDay() {
        int num = config.getInt("rest.maxDay");

        if (num > 0)
            return num;
        else {
            plugin.getLogger().warning("Недопустимое значние rest.maxDay. Установлено дефолтное - 60 дней");
            return 60;
        }
    }

    public Component getMessage(String path, TagResolver... resolvers) {
        String r = config.getString(path, "<red>Сообщения нет: " +path);
        return MM.deserialize(r,resolvers);
    }
}
