package org.allivilsey.youmuchan;

import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;
import com.velocitypowered.api.proxy.player.*;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.api.util.ServerLink;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YoumuVirtualize {

    public Player create(String username) {
        return new FictionalPlayer(username);
    }

    private static final class FictionalPlayer implements Player {
        private final String username;
        private final UUID uniqueId;
        private final Identity identity;
        private final GameProfile gameProfile;
        private final InetSocketAddress mockAddress;

        // 使用一个极简的内部类来应付其他插件对 PlayerSettings 和 TabList 的获取
        private final PlayerSettings dummySettings = new DummyPlayerSettings();
        private final TabList dummyTabList = new DummyTabList();

        private FictionalPlayer(String username) {
            this.username = username;
            this.uniqueId = UUID.nameUUIDFromBytes(("youmuchan:virtual-player:" + username).getBytes(StandardCharsets.UTF_8));
            this.identity = Identity.identity(uniqueId);
            this.gameProfile = new GameProfile(uniqueId, username, List.of());
            this.mockAddress = InetSocketAddress.createUnresolved("127.0.0.1", 25565);
        }

        // --- 核心身份信息 (Essential Identity) ---
        @Override public String getUsername() { return username; }
        @Override public UUID getUniqueId() { return uniqueId; }
        @Override public Identity identity() { return identity; }
        @Override public GameProfile getGameProfile() { return gameProfile; }
        @Override public boolean isActive() { return true; }
        @Override public boolean isOnlineMode() { return true; }
        @Override public long getPing() { return 0L; }
        @Override public InetSocketAddress getRemoteAddress() { return mockAddress; }

        // --- 兼容性占位 (Compatibility Stubs) ---
        // 返回 Optional.empty() 防止其他插件报 NullPointerException
        @Override public Optional<ServerConnection> getCurrentServer() { return Optional.empty(); }
        @Override public Optional<InetSocketAddress> getVirtualHost() { return Optional.empty(); }
        @Override public Optional<ModInfo> getModInfo() { return Optional.empty(); }
        @Override public IdentifiedKey getIdentifiedKey() { return null; }

        // --- 权限与协议 (Permissions & Protocol) ---
        @Override public Tristate getPermissionValue(String permission) {
            // 如果你的机器人需要特定权限（如跨服聊天权限），可以在这里写简单的判断逻辑
            // 默认返回未定义，交由底层的权限插件接管（虽然机器人通常不需要）
            return Tristate.UNDEFINED;
        }
        @Override public ProtocolVersion getProtocolVersion() { return ProtocolVersion.MAXIMUM_VERSION; }
        @Override public ProtocolState getProtocolState() { return ProtocolState.PLAY; }

        // --- Adventure/Velocity 接口要求，但对机器人无用的方法 (No-ops) ---
        @Override public PlayerSettings getPlayerSettings() { return dummySettings; }
        @Override public TabList getTabList() { return dummyTabList; }
        @Override public Locale getEffectiveLocale() { return Locale.CHINA; }
        @Override public void setEffectiveLocale(Locale locale) {}
        @Override public boolean hasSentPlayerSettings() { return true; }
        @Override public List<GameProfile.Property> getGameProfileProperties() { return List.of(); }
        @Override public void setGameProfileProperties(List<GameProfile.Property> properties) {}
        @Override public Component getPlayerListHeader() { return Component.empty(); }
        @Override public Component getPlayerListFooter() { return Component.empty(); }
        @Override public void clearPlayerListHeaderAndFooter() {}
        @Override public void disconnect(Component reason) {}
        @Override public void spoofChatInput(String input) {}
        @Override public void sendResourcePack(String url) {}
        @Override public void sendResourcePack(String url, byte[] hash) {}
        @Override public void sendResourcePackOffer(ResourcePackInfo packInfo) {}
        @Override public ResourcePackInfo getAppliedResourcePack() { return null; }
        @Override public ResourcePackInfo getPendingResourcePack() { return null; }
        @Override public Collection<ResourcePackInfo> getAppliedResourcePacks() { return List.of(); }
        @Override public Collection<ResourcePackInfo> getPendingResourcePacks() { return List.of(); }
        @Override public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) { return false; }
        @Override public boolean sendPluginMessage(ChannelIdentifier identifier, PluginMessageEncoder dataEncoder) { return false; }
        @Override public String getClientBrand() { return "YoumuChan-VirtualClient"; }
        @Override public void addCustomChatCompletions(Collection<String> completions) {}
        @Override public void removeCustomChatCompletions(Collection<String> completions) {}
        @Override public void setCustomChatCompletions(Collection<String> completions) {}
        @Override public void transferToHost(InetSocketAddress address) {}
        @Override public void storeCookie(Key key, byte[] value) {}
        @Override public void requestCookie(Key key) {}
        @Override public void setServerLinks(List<ServerLink> links) {}

        @Override
        public ConnectionRequestBuilder createConnectionRequest(RegisteredServer server) {
            throw new UnsupportedOperationException("Virtual players cannot connect to backend servers.");
        }

        @Override
        public Pointers pointers() {
            return Pointers.builder()
                    .withStatic(Identity.UUID, uniqueId)
                    .withStatic(Identity.NAME, username)
                    .withStatic(Identity.DISPLAY_NAME, Component.text(username))
                    .build();
        }

        // --- 极简内部类：应付那些试图读取玩家设置/Tab的聊天插件 ---
        private static final class DummyPlayerSettings implements PlayerSettings {
            @Override public Locale getLocale() { return Locale.CHINA; }
            @Override public byte getViewDistance() { return 12; }
            @Override public ChatMode getChatMode() { return ChatMode.SHOWN; }
            @Override public boolean hasChatColors() { return true; }
            @Override public SkinParts getSkinParts() { return new SkinParts((byte) 0x7F); }
            @Override public MainHand getMainHand() { return MainHand.RIGHT; }
            @Override public boolean isClientListingAllowed() { return false; }
        }

        private static final class DummyTabList implements TabList {
            @Override public void setHeaderAndFooter(Component header, Component footer) {}
            @Override public void clearHeaderAndFooter() {}
            @Override public void addEntry(TabListEntry entry) {}
            @Override public Optional<TabListEntry> removeEntry(UUID uuid) { return Optional.empty(); }
            @Override public boolean containsEntry(UUID uuid) { return false; }
            @Override public Optional<TabListEntry> getEntry(UUID uuid) { return Optional.empty(); }
            @Override public Collection<TabListEntry> getEntries() { return List.of(); }
            @Override public void clearAll() {}
            @Override public TabListEntry buildEntry(GameProfile profile, Component displayName, int latency, int gameMode, ChatSession chatSession, boolean listed) {
                return null; // 对于虚构玩家，不需要构建真实的 Tab 实体
            }
        }
    }
}