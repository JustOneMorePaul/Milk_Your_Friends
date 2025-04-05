package com.JOMPILS.milkyourfriends;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class Commands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)  {
        dispatcher.register(CommandManager.literal("milkable")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("milk")
                        // /milkable milk add [username]
                        .then(CommandManager.literal("add")
                                .then(CommandManager.argument("username", StringArgumentType.word())
                                        .executes(context -> {
                                            String username = StringArgumentType.getString(context, "username");
                                            ServerCommandSource source = context.getSource();
                                            MinecraftServer server = source.getServer();
                                            Optional<GameProfile> optionalProfile = server.getUserCache().findByName(username);
                                            UUID uuid;
                                            if (!optionalProfile.isPresent()) {
                                                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
                                                source.sendFeedback(() -> Text.literal("Player not found in cache. Using offline UUID for " + username + "."), true);
                                            } else {
                                                uuid = optionalProfile.get().getId();
                                            }
                                            Config.addMilkPlayer(uuid);
                                            source.sendFeedback(() -> Text.literal("Added " + username + " to the milkable list."), true);
                                            return 1;
                                        })
                                )
                        )
                        // /milkable milk remove [username]
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("username", StringArgumentType.word())
                                        .executes(context -> {
                                            String username = StringArgumentType.getString(context, "username");
                                            ServerCommandSource source = context.getSource();
                                            MinecraftServer server = source.getServer();
                                            Optional<GameProfile> optionalProfile = server.getUserCache().findByName(username);
                                            UUID uuid;
                                            if (!optionalProfile.isPresent()) {
                                                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
                                                source.sendFeedback(() -> Text.literal("Player not found in cache. Using offline UUID for " + username + "."), true);
                                            } else {
                                                uuid = optionalProfile.get().getId();
                                            }
                                            Config.removeMilkPlayer(uuid);
                                            source.sendFeedback(() -> Text.literal("Removed " + username + " from the milkable list."), true);
                                            return 1;
                                        })
                                )
                        )
                        // /milkable milk listtype [type]
                        .then(CommandManager.literal("listtype")
                                // If no argument is provided, return the current list type.
                                .executes(context -> {
                                    String currentType = Config.getMilkListType();
                                    context.getSource().sendFeedback(() -> Text.literal("Current list type: " + currentType), true);
                                    return 1;
                                })
                                .then(CommandManager.argument("type", StringArgumentType.word())
                                        .executes(context -> {
                                            String type = StringArgumentType.getString(context, "type");
                                            if (!("whitelist".equalsIgnoreCase(type) || "blacklist".equalsIgnoreCase(type))) {
                                                context.getSource().sendError(Text.literal("List type must be either 'whitelist' or 'blacklist'."));
                                                return 0;
                                            }
                                            Config.setMilkListType(type.toLowerCase());
                                            context.getSource().sendFeedback(() -> Text.literal("Set list type to " + type + "."), true);
                                            return 1;
                                        })
                                )
                        )
                        // /milkable milk list
                        .then(CommandManager.literal("list")
                                .executes(context -> {
                                    StringBuilder listMessage = new StringBuilder("Milkable players: ");
                                    MinecraftServer server = context.getSource().getServer();
                                    for (UUID uuid : Config.getMilkPlayers()) {
                                        Optional<GameProfile> profileOptional = server.getUserCache().getByUuid(uuid);
                                        String name = profileOptional.map(GameProfile::getName).orElse(uuid.toString());
                                        listMessage.append(name).append(" ");
                                    }
                                    context.getSource().sendFeedback(() -> Text.literal(listMessage.toString()), true);
                                    return 1;
                                })
                        )
                )
                // /milkable reload
                .then(CommandManager.literal("reload")
                        .executes(context -> {
                            Config.loadConfig();
                            context.getSource().sendFeedback(() -> Text.literal("Config reloaded."), true);
                            return 1;
                        })
                )
        );
    }
}
