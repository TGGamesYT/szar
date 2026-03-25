package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

public class SzarGameRules {

    public static GameRules.Key<GameRules.BooleanRule> RULE_RACIST;
    public static GameRules.Key<GameRules.BooleanRule> RULE_GAMBLING;
    public static GameRules.Key<GameRules.BooleanRule> RULE_NSFW;

    public static void register() {
        RULE_RACIST = GameRuleRegistry.register(
                "szarBlockRacist",
                GameRules.Category.PLAYER,
                GameRules.BooleanRule.create(false, (server, rule) -> onRuleChanged(server))
        );
        RULE_GAMBLING = GameRuleRegistry.register(
                "szarBlockGambling",
                GameRules.Category.PLAYER,
                GameRules.BooleanRule.create(false, (server, rule) -> onRuleChanged(server))
        );
        RULE_NSFW = GameRuleRegistry.register(
                "szarBlockNsfw",
                GameRules.Category.PLAYER,
                GameRules.BooleanRule.create(true, (server, rule) -> onRuleChanged(server))
        );
    }

    private static void onRuleChanged(MinecraftServer server) {
        GameRules rules = server.getGameRules();
        ServerConfig.set("racist",   rules.getBoolean(RULE_RACIST));
        ServerConfig.set("gambling", rules.getBoolean(RULE_GAMBLING));
        ServerConfig.set("nsfw",     rules.getBoolean(RULE_NSFW));
        ServerConfig.save();
    }

    /** Call on world load to push saved ServerConfig values back into gamerules. */
    public static void pushToGameRules(MinecraftServer server) {
        GameRules rules = server.getGameRules();
        rules.get(RULE_RACIST).set(ServerConfig.get("racist"), server);
        rules.get(RULE_GAMBLING).set(ServerConfig.get("gambling"), server);
        rules.get(RULE_NSFW).set(ServerConfig.get("nsfw"), server);
    }
}