package me.minecraft.resistor.whitelist.command;

import me.minecraft.resistor.whitelist.Whitelist;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Command implements CommandExecutor, TabExecutor {
    private Whitelist plugin;


    public Command(Whitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    @Nullable
    public List<String> onTabComplete (@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> list = new ArrayList<>();

        if (args.length == 1 ) {
            StringUtil.copyPartialMatches(args[0],
                    List.of("add","del","list","info","reload","rest"), list);
        } else if (args.length == 2 && List.of("del","info","rest").contains(args[0].toLowerCase())) {
            StringUtil.copyPartialMatches(args[1], plugin.sqLiteCache.getPlayers(),list);

        } else if (args.length == 3 && args[0].equalsIgnoreCase("rest")) {
            StringUtil.copyPartialMatches(args[2], List.of("0","7","14","30","cancel"), list);
        }
        Collections.sort(list);
        return list;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("wh")) {
            if (args.length == 0) {
                incorrectCommandNotif(sender);
                return true;
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("list")) {
                    return getWhitelist(sender, 1);
                } else if (args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadPlugin(sender);
                    return true;
                } else {
                    incorrectCommandNotif(sender);
                    return true;
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("add")) {
                    if (!args[1].matches("[A-Za-z0-9_]{3,16}")) {
                        sender.sendMessage(plugin.config.getMessage("message.addPlayer.errorFormat", Placeholder.unparsed("nick",args[1])));
                        return true;
                    }
                    return addPlayer(sender, args[1]);
                } else if (args[0].equalsIgnoreCase("del")) {
                    return deletePlayer(sender, args[1]);
                } else if (args[0].equalsIgnoreCase("info")) {
                    if (plugin.sqLiteCache.isAllowed(args[1]) == 1) {
                        return getPlayerInfo(sender,args[1]);
                    } else {
                        sender.sendMessage(Component.text("Whitelist: ", NamedTextColor.GREEN)
                                .append(Component.text("Ника " + args[1] + " нету в вайтлисте")));
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("list")) {
                    try {
                        return getWhitelist(sender,Integer.parseInt(args[1]));
                    } catch (NumberFormatException e) {
                        incorrectCommandNotif(sender);
                        return true;
                    }
                }  else {
                    incorrectCommandNotif(sender);
                    return true;
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("rest")) {
                    return restCommand(sender, args);
                } else {
                    incorrectCommandNotif(sender);
                    return true;
                }
            }



            else {
                incorrectCommandNotif(sender);
                return true;
            }

        }

        return false;
    }

    // 0 - false
    // !0 - tru

    private boolean getWhitelist(CommandSender sender, int numPage) {

        try {
            List<String> list = plugin.sqLiteDataBase.getPlayers();
            Component message = Component.text("Whitelist: ", NamedTextColor.GREEN);


            int total = list.size();
            int pagesCount = (total + 10 - 1) / 10;

            if (total == 0) {
                message = message.append(Component.text("Список пуст", NamedTextColor.GREEN));
                sender.sendMessage(message);
                return true;
            }

            if (numPage < 1 || numPage > pagesCount) {
                message = message.append(Component.text("Такой страницы нет", NamedTextColor.GREEN));
                sender.sendMessage(message);
                return true;
            }

            int from = (numPage -1 ) * 10;
            int to = Math.min(from + 10, total);


            message = message.append(Component.newline())
                    .append(Component.newline());
            for (String nick : list.subList(from,to)) {
                Component rest = Component.text(" [rest]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.suggestCommand("/wh rest " + nick))
                        .hoverEvent(HoverEvent.showText(Component.text("Нажмите для управления рестом игрока")));

                Component player = Component.text(nick, NamedTextColor.WHITE)
                        .clickEvent(ClickEvent.runCommand("/wh info " + nick))
                        .hoverEvent(HoverEvent.showText(Component.text("Нажмите для получения информации о игроке")));

                message = message.append(player)
                        .append(rest)
                        .append(Component.newline());
            }
            Component buttonBack;
            if (numPage > 1) {
                buttonBack = Component.text("<", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/wh list " + (numPage - 1)));
            } else {
                buttonBack = Component.text(" ");
            }

            Component buttonNext;

            if (numPage < pagesCount) {
                buttonNext = Component.text(">", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/wh list " + (numPage + 1)));
            } else {
                buttonNext = Component.text(" ");
            }


            Component buttons = Component.text("")
                    .append(Component.newline())
                    .append(buttonBack)
                    .append(Component.text("     " + numPage + "/" + pagesCount + "     "))
                    .append(buttonNext);


            sender.sendMessage(message.append(buttons).append(Component.newline()));
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка получения белого списка error: " + e);

            sender.sendMessage(
                    Component.text("Whitelist: ", NamedTextColor.DARK_RED)
                            .append(Component.text("ошибка получения белого списка проверьте консоль"))
            );
        }
        return true;
    }

    public boolean restCommand(CommandSender sender, String args[]) {
        try {

            if (args[2].equalsIgnoreCase("0") || args[2].equalsIgnoreCase("cancel")) {
                if (plugin.sqLiteDataBase.cancelRest(args[1]) > 0) {
                    sender.sendMessage(plugin.config.getMessage("message.rest.successCancel", Placeholder.unparsed("nick",args[1])));
                    return true;
                } else {
                    sender.sendMessage(plugin.config.getMessage("message.rest.cancelError", Placeholder.unparsed("nick",args[1])));
                    return true;
                }

            }

            int num = Integer.parseInt(args[2]);
            int restint = plugin.sqLiteDataBase.setRest(args[1], num, plugin.config.getMaxDay());

            if (restint == 0) {
                sender.sendMessage(plugin.config.getMessage("message.rest.nicknameDoesntExist", Placeholder.unparsed("nick",args[1])));
                return true;
            } else if (restint > 0 && restint != 105 && restint != 106) {
                sender.sendMessage(plugin.config.getMessage("message.rest.successfullyIssued", Placeholder.unparsed("nick",args[1]),Placeholder.unparsed("day",args[2])));
                return true;
            } else if (restint == 105) {
                sender.sendMessage(plugin.config.getMessage("message.rest.periodTooLong", Placeholder.unparsed("nick",args[1])));
                return true;
            } else {
                sender.sendMessage(plugin.config.getMessage("message.rest.periodTooShort", Placeholder.unparsed("nick",args[1])));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(
                    Component.text("Whitelist: ", NamedTextColor.GREEN)
                            .append(Component.text("неправильное использование команды", NamedTextColor.WHITE)));
            incorrectCommandNotif(sender);

            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка установки реста игроку " + args[1] + " error: " + e);

            sender.sendMessage(
                    Component.text("Whitelist: ", NamedTextColor.DARK_RED)
                            .append(Component.text("ошибка установки реста игроку " + args[1] + " проверьте консоль"))
            );
            return true;
        }
    }

    public boolean getPlayerInfo(CommandSender sender,String nick) {
        try {
            Map<String,Long> map = plugin.sqLiteDataBase.getPlayerInfo(nick);

            if (map.get("null") == 0) {
                Component mess = Component.text("Whitelist: ", NamedTextColor.GREEN)
                        .append(Component.newline())
                        .append(Component.text("Данного ника нет в белом списке (не удалось получить информацию о данном игроке!)", NamedTextColor.WHITE));
                sender.sendMessage(mess);
                return true;
            }

            Component message = Component.text("Whitelist: ", NamedTextColor.GREEN)
                    .append(Component.newline());
            message = message.append(Component.text("Nickname: " , NamedTextColor.WHITE)
                    .append(Component.text(nick, NamedTextColor.GREEN))
                    .append(Component.newline()));


            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            String last_seen = Instant.ofEpochMilli(map.get("last_seen"))
                    .atZone(ZoneId.systemDefault()).format(fmt);

            Player player = Bukkit.getPlayerExact(nick);

            String rest_until = "";

            long beforDelet = Math.max(0,TimeUnit.MILLISECONDS.toDays( TimeUnit.DAYS.toMillis(plugin.config.getDay()) + map.get("last_seen") - System.currentTimeMillis()));
            long untiltheend = Math.max(0, TimeUnit.MILLISECONDS.toDays(map.get("rest_until") - System.currentTimeMillis()));


            if (map.get("rest") == 1 && map.get("rest_until") != 0) {
                rest_until = Instant.ofEpochMilli(map.get("rest_until"))
                        .atZone(ZoneId.systemDefault()).format(fmt);

                if (player != null) {
                    message = message.append(Component.text("Последняя активность: ", NamedTextColor.WHITE))
                            .append(Component.text("в сети", NamedTextColor.GREEN))
                            .append(Component.newline());
                } else {
                    message = message.append(Component.text("Последняя активность: ", NamedTextColor.WHITE))
                            .append(Component.text(last_seen, NamedTextColor.GREEN))
                            .append(Component.text(" до удаления " + (untiltheend + plugin.config.getDay())))
                            .append(Component.newline());

                }


                message = message.append(Component.text("Рест: " , NamedTextColor.WHITE))
                        .append(Component.text("есть", NamedTextColor.GREEN))
                        .append(Component.newline())
                        .append(Component.text("Дата окончания реста; ", NamedTextColor.WHITE))
                        .append(Component.text( rest_until, NamedTextColor.GREEN))
                        .append(Component.text(" до конца " + untiltheend))
                        .append(Component.newline());

            } else {
                if (player != null) {
                    message = message.append(Component.text("Последняя активность: ", NamedTextColor.WHITE))
                            .append(Component.text("в сети", NamedTextColor.GREEN))
                            .append(Component.newline());
                } else {
                    if (beforDelet != 0) {
                        message = message.append(Component.text("Последняя активность: ", NamedTextColor.WHITE))
                                .append(Component.text(last_seen, NamedTextColor.GREEN))
                                .append(Component.text(" до удаления " + beforDelet))
                                .append(Component.newline());
                    } else {
                        message = message.append(Component.text("Последняя активность: ", NamedTextColor.WHITE))
                                .append(Component.text(last_seen, NamedTextColor.GREEN))
                                .append(Component.text(" будет удален при ближайшей проверке"))
                                .append(Component.newline());
                    }

                }

                message = message.append(Component.text("Рест: ", NamedTextColor.WHITE)
                        .append(Component.text("нету", NamedTextColor.GREEN))
                        .append(Component.newline()));
            }


            sender.sendMessage(message);




        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка получения информации о игроке " + nick + " error: " + e);
            sender.sendMessage(Component.text("Whitelist: ", NamedTextColor.DARK_RED)
                    .append(Component.text("Ошибка получения информации о игроке " + nick + " проверьте консоль", NamedTextColor.WHITE)));
        }

        return true;
    }

    private boolean addPlayer(CommandSender sender, String nick) {
        int num = plugin.sqLiteCache.addPlayer(nick);

        if (num > 0) {
            sender.sendMessage(plugin.config.getMessage("message.addPlayer.successAdded", Placeholder.unparsed("nick",nick)));
        } else if (num == 0) {
            sender.sendMessage(plugin.config.getMessage("message.addPlayer.alreadyBe", Placeholder.unparsed("nick",nick)));
        } else if (num == -1) {
            sender.sendMessage(plugin.config.getMessage("message.addPlayer.couldNotAdd", Placeholder.unparsed("nick",nick)));
        }
        return true;
    }

    private boolean deletePlayer(CommandSender sender, String nick) {
        int num = plugin.sqLiteCache.removePlayer(nick);

        if (num > 0) {
            sender.sendMessage(plugin.config.getMessage("message.deletePlayer.successDeleted", Placeholder.unparsed("nick",nick)));
            kickPlayer(nick);
        } else if (num == 0) {
            sender.sendMessage(plugin.config.getMessage("message.deletePlayer.alreadyBe", Placeholder.unparsed("nick",nick)));
        } else if (num == -1) {
            sender.sendMessage(plugin.config.getMessage("message.deletePlayer.couldNotAdd", Placeholder.unparsed("nick",nick)));
        }

        return true;
    }

    private void kickPlayer(String nick) {
        Player player = Bukkit.getPlayerExact(nick);
        if (player != null) {
            player.kick(plugin.config.getMessage("message.deletePlayer.kickMessage"));
        }
    }

    private void incorrectCommandNotif(CommandSender sender) {
        sender.sendMessage(
                Component.text("Whitelist: ", NamedTextColor.DARK_RED).append(
                        Component.text("неправильное использование команды. Правильное использование /wh add <nick> | del <nick> | info <nick> | list | reload | rest <nick> <period>", NamedTextColor.WHITE)));
    }


}
