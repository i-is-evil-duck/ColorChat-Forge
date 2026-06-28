package com.j3ly.colorchat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ColorChatData {

    private static final Path CONFIG_PATH = Path.of("config", "colorchat.json");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Map<String, Group> groups = new LinkedHashMap<>();
    private Map<String, String> playerGroups = new LinkedHashMap<>();
    private Map<String, ChatFormatting> playerNameColors = new LinkedHashMap<>();
    private Map<String, ChatFormatting> playerChatColors = new LinkedHashMap<>();
    private ChatFormatting bracketColor = ChatFormatting.LIGHT_PURPLE;
    private ChatFormatting plusColor = ChatFormatting.GOLD;

    public record Group(String name, ChatFormatting color) {}

    private static class GroupData {
        String name;
        String color;
    }

    private static class ConfigFile {
        Map<String, GroupData> groups = new LinkedHashMap<>();
        Map<String, String> groupMembers = new LinkedHashMap<>();
        Map<String, String> playerColors = new LinkedHashMap<>();
        Map<String, String> chatColors = new LinkedHashMap<>();
        String bracketColor = "LIGHT_PURPLE";
        String plusColor = "GOLD";
    }

    public void load() {
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(CONFIG_PATH)) {
                createDefault();
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ConfigFile config = gson.fromJson(reader, ConfigFile.class);
                if (config == null) { createDefault(); return; }

                groups.clear();
                if (config.groups != null) {
                    for (Map.Entry<String, GroupData> entry : config.groups.entrySet()) {
                        ChatFormatting color = parseColor(entry.getValue().color);
                        groups.put(entry.getKey(), new Group(entry.getValue().name, color));
                    }
                }

                playerGroups.clear();
                if (config.groupMembers != null) {
                    playerGroups.putAll(config.groupMembers);
                }

                playerNameColors.clear();
                if (config.playerColors != null) {
                    for (Map.Entry<String, String> e : config.playerColors.entrySet()) {
                        playerNameColors.put(e.getKey().toLowerCase(), parseColor(e.getValue()));
                    }
                }

                playerChatColors.clear();
                if (config.chatColors != null) {
                    for (Map.Entry<String, String> e : config.chatColors.entrySet()) {
                        playerChatColors.put(e.getKey().toLowerCase(), parseColor(e.getValue()));
                    }
                }

                if (config.bracketColor != null) bracketColor = parseColor(config.bracketColor);
                if (config.plusColor != null) plusColor = parseColor(config.plusColor);
            }
        } catch (Exception e) {
            System.out.println("[ColorChat] Failed to load config: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            ConfigFile config = new ConfigFile();
            for (Map.Entry<String, Group> e : groups.entrySet()) {
                GroupData gd = new GroupData();
                gd.name = e.getValue().name();
                gd.color = e.getValue().color().name();
                config.groups.put(e.getKey(), gd);
            }
            config.groupMembers = new LinkedHashMap<>(playerGroups);

            Map<String, String> pc = new LinkedHashMap<>();
            for (Map.Entry<String, ChatFormatting> e : playerNameColors.entrySet()) {
                pc.put(e.getKey(), e.getValue().name());
            }
            config.playerColors = pc;

            Map<String, String> cc = new LinkedHashMap<>();
            for (Map.Entry<String, ChatFormatting> e : playerChatColors.entrySet()) {
                cc.put(e.getKey(), e.getValue().name());
            }
            config.chatColors = cc;

            config.bracketColor = bracketColor.name();
            config.plusColor = plusColor.name();

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                gson.toJson(config, writer);
            }
        } catch (Exception e) {
            System.out.println("[ColorChat] Failed to save config: " + e.getMessage());
        }
    }

    private void createDefault() throws IOException {
        ConfigFile config = new ConfigFile();

        GroupData vip = new GroupData(); vip.name = "VIP+"; vip.color = "GOLD";
        config.groups.put("vip+", vip);
        GroupData mvp = new GroupData(); mvp.name = "MVP+++"; mvp.color = "LIGHT_PURPLE";
        config.groups.put("mvp+++", mvp);

        config.groupMembers.put("ducky", "vip+");
        config.groupMembers.put("timmy", "vip+");
        config.groupMembers.put("lenard", "mvp+++");
        config.playerColors.put("mikey", "GREEN");
        config.chatColors.put("mikey", "BLUE");
        config.bracketColor = "RED";
        config.plusColor = "GOLD";

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            gson.toJson(config, writer);
        }

        load();
    }

    public Map<String, Group> getGroups() { return groups; }
    public String getPlayerGroup(String name) { return playerGroups.get(name); }

    public void setPlayerGroup(String name, String group) {
        playerGroups.put(name, group);
        if (!groups.containsKey(group)) {
            groups.put(group, new Group(group, ChatFormatting.WHITE));
        }
    }

    public ChatFormatting getPlayerNameColor(String name) { return playerNameColors.get(name); }
    public void setPlayerNameColor(String name, ChatFormatting color) { playerNameColors.put(name, color); }
    public ChatFormatting getPlayerChatColor(String name) { return playerChatColors.get(name); }
    public void setPlayerChatColor(String name, ChatFormatting color) { playerChatColors.put(name, color); }

    public void clearPlayer(String name) {
        playerGroups.remove(name);
        playerNameColors.remove(name);
        playerChatColors.remove(name);
    }

    public ChatFormatting getBracketColor() { return bracketColor; }
    public ChatFormatting getPlusColor() { return plusColor; }

    private ChatFormatting parseColor(String name) {
        try {
            return ChatFormatting.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return ChatFormatting.WHITE;
        }
    }
}
