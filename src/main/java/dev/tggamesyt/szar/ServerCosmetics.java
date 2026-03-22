package dev.tggamesyt.szar;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class ServerCosmetics {

    public static final Identifier SYNC_PACKET =
            new Identifier(Szar.MOD_ID, "cosmetic_sync");
    public static final Identifier MOJANG_CAPES_SYNC = new Identifier(Szar.MOD_ID, "mojang_capes_sync");

    private static final String CAPES_URL =
            "https://raw.githubusercontent.com/tggamesyt/szar/main/capes.json";

    private static final String USERS_URL =
            "https://raw.githubusercontent.com/tggamesyt/szar/main/usercosmetics.json";

    public static final Map<String, String> CAPES = new HashMap<>();
    public static final Map<UUID, UserCosmetics> USERS = new HashMap<>();
    public static final Map<UUID, List<MojangCape>> PLAYER_MOJANG_CAPES = new HashMap<>();

    public static class MojangCape {
        public String id;
        public String name;
        public String url;
    }

    public static class UserCosmetics {
        public NameType nameType;
        public Integer staticColor;
        public Integer gradientStart;
        public Integer gradientEnd;
        public List<String> ownedCapes = new ArrayList<>();
        public String selectedCape;
    }

    /* ---------------- INITIALIZE ---------------- */

    public static void init() {
        loadJson();
        registerCommand();
        ServerPlayNetworking.registerGlobalReceiver(MOJANG_CAPES_SYNC, (server, player, handler, buf, responseSender) -> {
            UUID uuid = buf.readUuid();
            int size = buf.readInt();
            List<MojangCape> list = new ArrayList<>();

            System.out.println("SZAR: server received Mojang capes for " + uuid + ", count=" + size);

            for (int i = 0; i < size; i++) {
                MojangCape c = new MojangCape();
                c.id = buf.readString();
                c.name = buf.readString();
                c.url = buf.readString();
                list.add(c);
            }

            PLAYER_MOJANG_CAPES.put(uuid, list);
        });
    }

    /* ---------------- LOAD JSON ---------------- */

    private static void loadJson() {
        try {
            Gson gson = new Gson();

            // CAPES
            String capesRaw = readUrl(CAPES_URL);
            JsonObject capesJson = gson.fromJson(capesRaw, JsonObject.class);
            for (JsonElement e : capesJson.getAsJsonArray("capes")) {
                JsonObject obj = e.getAsJsonObject();
                CAPES.put(
                        obj.get("id").getAsString(),
                        obj.get("texture").getAsString()
                );
            }

            // USERS
            String usersRaw = readUrl(USERS_URL);
            JsonObject usersJson = gson.fromJson(usersRaw, JsonObject.class);
            for (JsonElement e : usersJson.getAsJsonArray("users")) {
                JsonObject obj = e.getAsJsonObject();

                UUID uuid = UUID.fromString(obj.get("uuid").getAsString());
                UserCosmetics user = new UserCosmetics();

                user.nameType = NameType.valueOf(obj.get("nameType").getAsString());

                if (obj.has("staticColor"))
                    user.staticColor = parseHex(obj.get("staticColor").getAsString());

                if (obj.has("gradientStart")) {
                    user.gradientStart = parseHex(obj.get("gradientStart").getAsString());
                    user.gradientEnd = parseHex(obj.get("gradientEnd").getAsString());
                }

                for (JsonElement cape : obj.getAsJsonArray("capes"))
                    user.ownedCapes.add(cape.getAsString());

                USERS.put(uuid, user);
            }

            Szar.LOGGER.info("Loaded server capes & user cosmetics from GitHub");

        } catch (Exception e) {
            Szar.LOGGER.error("Failed loading cosmetics", e);
        }
    }

    private static String readUrl(String url) throws Exception {
        try (InputStream in = new URL(url).openStream()) {
            return new String(in.readAllBytes());
        }
    }

    private static int parseHex(String hex) {
        return Integer.parseInt(hex.replace("#", ""), 16);
    }

    /* ---------------- COMMAND ---------------- */

    private static void registerCommand() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("cape")
                        .then(CommandManager.argument("id", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    if (player == null) return builder.buildFuture();

                                    UserCosmetics user = USERS.get(player.getUuid());

                                    // Only suggest capes this player actually owns
                                    if (user != null) {
                                        for (String id : user.ownedCapes) {
                                            builder.suggest(id);
                                        }
                                    }

                                    // Mojang capes from server-side map
                                    List<MojangCape> mojang = PLAYER_MOJANG_CAPES.get(player.getUuid());
                                    System.out.println("SZAR: suggestions - mojang capes for " + player.getName().getString() + ": " + (mojang == null ? "null" : mojang.size()));
                                    if (mojang != null) {
                                        for (MojangCape c : mojang) {
                                            builder.suggest(c.name);
                                        }
                                    }

                                    builder.suggest("none");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    String id = StringArgumentType.getString(ctx, "id");

                                    // Create a default entry if player has no cosmetics profile
                                    UserCosmetics user = USERS.computeIfAbsent(player.getUuid(), k -> {
                                        UserCosmetics u = new UserCosmetics();
                                        u.nameType = NameType.STATIC;
                                        u.ownedCapes = new ArrayList<>();
                                        return u;
                                    });

                                    // Deselect
                                    if (id.equalsIgnoreCase("none")) {
                                        user.selectedCape = null;
                                        sync(player, user);
                                        player.sendMessage(Text.literal("Unequipped cape."), false);
                                        return 1;
                                    }

                                    // Mojang cape selection
                                    List<MojangCape> mojang = PLAYER_MOJANG_CAPES.get(player.getUuid());
                                    if (mojang != null) {
                                        for (MojangCape c : mojang) {
                                            if (c.name.equalsIgnoreCase(id)) {
                                                user.selectedCape = c.id;
                                                sync(player, user);
                                                player.sendMessage(Text.literal("Equipped Mojang cape: " + c.name), false);
                                                return 1;
                                            }
                                        }
                                    }

                                    // Custom cape check
                                    if (!user.ownedCapes.contains(id)) {
                                        player.sendMessage(Text.literal("You don't own this cape."), false);
                                        return 0;
                                    }

                                    user.selectedCape = id;
                                    sync(player, user);
                                    player.sendMessage(Text.literal("Equipped cape: " + id), false);
                                    return 1;
                                })
                        )
                ));
    }

    /* ---------------- SYNC ---------------- */

    // Send ONE player's cosmetics to ALL online players (used for /cape changes)
    public static void sync(ServerPlayerEntity owner, UserCosmetics user) {
        for (ServerPlayerEntity p : owner.getServer().getPlayerManager().getPlayerList()) {
            syncTo(p, owner, user);
        }
    }
    public enum NameType {
        STATIC,
        GRADIENT
    }

    // Send ONE player's cosmetics to ONE recipient
    public static void syncTo(ServerPlayerEntity recipient, ServerPlayerEntity owner,
                              UserCosmetics user) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(owner.getUuid()); // whose cosmetics these are
        buf.writeEnumConstant(user.nameType);
        buf.writeBoolean(user.staticColor != null);
        if (user.staticColor != null) buf.writeInt(user.staticColor);
        buf.writeBoolean(user.gradientStart != null);
        if (user.gradientStart != null) {
            buf.writeInt(user.gradientStart);
            buf.writeInt(user.gradientEnd);
        }

        String textureUrl = null;
        if (user.selectedCape != null) {
            textureUrl = CAPES.get(user.selectedCape);
            if (textureUrl == null) {
                List<MojangCape> mojang = PLAYER_MOJANG_CAPES.get(owner.getUuid());
                if (mojang != null) {
                    for (MojangCape c : mojang) {
                        if (c.id.equalsIgnoreCase(user.selectedCape)) {
                            textureUrl = c.url;
                            break;
                        }
                    }
                }
            }
        }
        buf.writeString(textureUrl == null ? "" : textureUrl);
        ServerPlayNetworking.send(recipient, SYNC_PACKET, buf);
    }
}