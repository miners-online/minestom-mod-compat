package uk.minersonline.games.minestom_mod_compat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;

public class DistantHorizonsHandler {

    private static final String CHANNEL = "distant_horizons:message";
    private static final short  PROTOCOL_VERSION = 10;
    private static final int SESSION_CONFIG_ID = 3;
    private static final short LEVEL_INIT_ID = 2;
    private static String prefix;

    public static void register(String prefix) {
        DistantHorizonsHandler.prefix = prefix;
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        globalEventHandler.addListener(PlayerPluginMessageEvent.class, event -> {
            if (!event.getIdentifier().equals(CHANNEL)) return;

            NetworkBuffer buffer = NetworkBuffer.wrap(event.getMessage(), 0, event.getMessage().length);

            int protocolVersion = buffer.read(NetworkBuffer.SHORT);
            int messageId = buffer.read(NetworkBuffer.SHORT);

            if (protocolVersion != PROTOCOL_VERSION || messageId != SESSION_CONFIG_ID)
                return;

            sendLevelInit(event.getPlayer());
        });

        globalEventHandler.addListener(PlayerLoadedEvent.class, event -> {
            Player player = event.getPlayer();
            if (player.getInstance() == null) return; // Ensure the player is in an instance

            // Send the level initialization message when the player loads
            sendLevelInit(player);
        });
    }

    private static void sendLevelInit(Player player) {
        NetworkBuffer buffer = NetworkBuffer.resizableBuffer();

        buffer.write(NetworkBuffer.SHORT, PROTOCOL_VERSION);
        buffer.write(NetworkBuffer.SHORT, LEVEL_INIT_ID);

        String levelKey = prefix + player.getInstance().getDimensionName();
        buffer.write(NetworkBuffer.VAR_INT, levelKey.length());
        buffer.write(NetworkBuffer.BYTE_ARRAY, levelKey.getBytes());

        buffer.write(NetworkBuffer.LONG, System.currentTimeMillis());

        byte[] result = new byte[(int) buffer.writeIndex()];
        buffer.copyTo(0, result, 0, result.length);

        player.sendPacket(new PluginMessagePacket(CHANNEL, result));
    }
}

