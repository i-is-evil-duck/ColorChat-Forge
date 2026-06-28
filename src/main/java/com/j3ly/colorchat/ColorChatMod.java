package com.j3ly.colorchat;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("colorchat")
public class ColorChatMod {

    private ColorChatData data;

    public ColorChatMod() {
        data = new ColorChatData();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        data.load();
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String messageText = event.getMessage().getString();
        String nameKey = player.getName().getString().toLowerCase();

        ChatFormatting nameColor = data.getPlayerNameColor(nameKey);
        ChatFormatting chatColor = data.getPlayerChatColor(nameKey);
        String groupKey = data.getPlayerGroup(nameKey);

        MutableComponent full = Component.literal("");

        if (groupKey != null && data.getGroups().containsKey(groupKey)) {
            ColorChatData.Group g = data.getGroups().get(groupKey);
            String rankName = g.name();

            full.append(Component.literal("[").withStyle(data.getBracketColor()));

            for (int i = 0; i < rankName.length(); i++) {
                char c = rankName.charAt(i);
                if (c == '+') {
                    full.append(Component.literal("+").withStyle(data.getPlusColor()));
                } else {
                    full.append(Component.literal(String.valueOf(c)).withStyle(g.color()));
                }
            }

            full.append(Component.literal("] ").withStyle(data.getBracketColor()));
        }

        full.append(Component.literal(player.getName().getString()).withStyle(nameColor != null ? nameColor : ChatFormatting.WHITE));
        full.append(Component.literal(": ").withStyle(ChatFormatting.WHITE));
        full.append(Component.literal(messageText).withStyle(chatColor != null ? chatColor : ChatFormatting.WHITE));

        event.setCanceled(true);

        if (player.getServer() != null) {
            player.getServer().getPlayerList().broadcastSystemMessage(full, false);
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("colorchat")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(ctx -> {
                        data.load();
                        ctx.getSource().sendSuccess(() -> Component.literal("ColorChat config reloaded.").withStyle(ChatFormatting.GREEN), true);
                        return 1;
                    }))
                .then(Commands.literal("role")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("group", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String player = StringArgumentType.getString(ctx, "player");
                                String group = StringArgumentType.getString(ctx, "group");
                                data.setPlayerGroup(player.toLowerCase(), group);
                                data.save();
                                ctx.getSource().sendSuccess(() -> Component.literal("Set role of " + player + " to " + group), true);
                                return 1;
                            }))))
                .then(Commands.literal("name")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("color", StringArgumentType.word())
                            .executes(ctx -> {
                                String player = StringArgumentType.getString(ctx, "player");
                                String color = StringArgumentType.getString(ctx, "color");
                                data.setPlayerNameColor(player.toLowerCase(), ChatFormatting.valueOf(color.toUpperCase()));
                                data.save();
                                ctx.getSource().sendSuccess(() -> Component.literal("Set name color of " + player + " to " + color), true);
                                return 1;
                            }))))
                .then(Commands.literal("chat")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .then(Commands.argument("color", StringArgumentType.word())
                            .executes(ctx -> {
                                String player = StringArgumentType.getString(ctx, "player");
                                String color = StringArgumentType.getString(ctx, "color");
                                data.setPlayerChatColor(player.toLowerCase(), ChatFormatting.valueOf(color.toUpperCase()));
                                data.save();
                                ctx.getSource().sendSuccess(() -> Component.literal("Set chat color of " + player + " to " + color), true);
                                return 1;
                            }))))
                .then(Commands.literal("clear")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            String player = StringArgumentType.getString(ctx, "player");
                            String key = player.toLowerCase();
                            data.clearPlayer(key);
                            data.save();
                            ctx.getSource().sendSuccess(() -> Component.literal("Cleared all color and role settings for " + player), true);
                            return 1;
                        })))
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal("/colorchat reload").withStyle(ChatFormatting.GOLD), false);
                        ctx.getSource().sendSuccess(() -> Component.literal("/colorchat role <player> <group>").withStyle(ChatFormatting.GOLD), false);
                        ctx.getSource().sendSuccess(() -> Component.literal("/colorchat name <player> <color>").withStyle(ChatFormatting.GOLD), false);
                        ctx.getSource().sendSuccess(() -> Component.literal("/colorchat chat <player> <color>").withStyle(ChatFormatting.GOLD), false);
                        ctx.getSource().sendSuccess(() -> Component.literal("/colorchat clear <player>").withStyle(ChatFormatting.GOLD), false);
                        return 1;
                    }))
        );
    }
}
