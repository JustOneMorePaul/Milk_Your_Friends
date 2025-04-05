package com.JOMPILS.milkyourfriends;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class MilkYourFriends_init implements ModInitializer {
    public static final String MOD_ID = "milkyourfriends";

    @Override
    public void onInitialize() {

        Config.loadConfig();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            Commands.register(dispatcher);
        });

        EntityHandler.register();
    }
}
