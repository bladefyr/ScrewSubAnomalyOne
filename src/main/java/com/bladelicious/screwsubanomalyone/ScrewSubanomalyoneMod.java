package com.bladelicious.screwsubanomalyone;

import com.bladelicious.screwsubanomalyone.config.EntityRemovalConfig;
import com.bladelicious.screwsubanomalyone.procedures.DespawnHim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ScrewSubanomalyoneMod.MODID)
public class ScrewSubanomalyoneMod {
    public static final Logger LOGGER = LogManager.getLogger(ScrewSubanomalyoneMod.class);
    public static final String MODID = "screw_subanomalyone";

    public ScrewSubanomalyoneMod(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, EntityRemovalConfig.SPECIFICATION);

        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);

        if (FMLLoader.getDist() == Dist.CLIENT) {
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        LOGGER.info("ScrewSubAnomaly1 successfully loaded!!11!111!1!!");
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == EntityRemovalConfig.SPECIFICATION) {
            LOGGER.info("Loading entity removal config");
            DespawnHim.markConfigForReload();
        }
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == EntityRemovalConfig.SPECIFICATION) {
            LOGGER.info("Reloading entity removal config");
            DespawnHim.markConfigForReload();
        }
    }
}