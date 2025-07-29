package uk.minersonline.games.minestom_mod_compat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.instance.AddEntityToInstanceEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class DistantHorizonsHandler {
    private static final String CHANNEL = "distant_horizons:message";
    private static final short  PROTOCOL_VERSION = 10;
    private static final int SESSION_CONFIG_ID = 3;
    private static final short LEVEL_INIT_ID = 2;
    private static final ConcurrentHashMap<Player, Integer> protocolVersions = new ConcurrentHashMap<>();

    public static void register() {
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(PlayerPluginMessageEvent.class, event -> {
            if (!event.getIdentifier().equals(CHANNEL)) return;

            NetworkBuffer buffer = NetworkBuffer.wrap(event.getMessage(), 0, event.getMessage().length);

            int protocolVersion = buffer.read(NetworkBuffer.SHORT);
            int messageId = buffer.read(NetworkBuffer.SHORT);

            if (protocolVersion < PROTOCOL_VERSION || messageId != SESSION_CONFIG_ID)
                return;

            protocolVersions.put(event.getPlayer(), protocolVersion);

            sendLevelInit(event.getPlayer());
        });

        globalEventHandler.addListener(AddEntityToInstanceEvent.class, event -> {
            Entity entity = event.getEntity();
            if (entity instanceof Player player) {
                sendLevelInit(player);
            }
        });
    }

    private static void sendLevelInit(Player player) {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        buffer.write(NetworkBuffer.SHORT, (short) getProtocolVersion(player));
        buffer.write(NetworkBuffer.SHORT, LEVEL_INIT_ID);

        QueryDistantHorizonsLevelIDEvent event = new QueryDistantHorizonsLevelIDEvent(player.getInstance(), player);
        EventDispatcher.call(event);

        String levelKey = event.getLevelID();
        byte[] bytes = levelKey.getBytes(StandardCharsets.UTF_8);
        buffer.write(NetworkBuffer.SHORT, (short) bytes.length);
        buffer.write(NetworkBuffer.RAW_BYTES, bytes);

        buffer.write(NetworkBuffer.LONG, System.currentTimeMillis());

        byte[] result = new byte[(int) buffer.writeIndex()];
        buffer.copyTo(0, result, 0, result.length);

        player.sendPacket(new PluginMessagePacket(CHANNEL, result));
    }

    private static int getProtocolVersion(Player player) {
        return protocolVersions.getOrDefault(player, (int) PROTOCOL_VERSION);
    }

    /**
     * Called by the Distant Horizons mod to query the level ID for the current instance.
     */
    public static class QueryDistantHorizonsLevelIDEvent implements InstanceEvent, PlayerEvent {
        private final Instance instance;
        private final Player player;

        private String levelID;

        public QueryDistantHorizonsLevelIDEvent(Instance instance, Player player) {
            this.instance = instance;
            this.player = player;
        }

        @Override
        public @NotNull Instance getInstance() {
            return instance;
        }

        @Override
        public @NotNull Player getPlayer() {
            return player;
        }

        public String getLevelID() {
            return levelID;
        }

        public void setLevelID(String levelID) {
            this.levelID = levelID;
        }
    }
}

