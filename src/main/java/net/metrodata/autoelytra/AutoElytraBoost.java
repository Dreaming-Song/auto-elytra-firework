package net.metrodata.autoelytra;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.metrodata.autoelytra.config.ModConfig;
import net.metrodata.autoelytra.core.ElytraBoostController;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AutoElytraBoost implements ClientModInitializer {
    public static final String MOD_ID = "autoelytra";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** 默认绑定空格,可在 选项 -> 控制 -> 移动 中修改。 */
    public static KeyMapping BOOST_KEY;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        BOOST_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.autoelytra.boost",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_SPACE,
                KeyMapping.Category.MOVEMENT
        ));

        ClientTickEvents.END_CLIENT_TICK.register(ElytraBoostController::onEndTick);
        LOGGER.info("[Auto Elytra Boost] Loaded for Minecraft 26.2.");
    }
}
