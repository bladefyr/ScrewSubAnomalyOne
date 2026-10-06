package vinn.tekk.screwyourmobs;

import vinn.tekk.screwyourmobs.config.EntityRemovalConfig;
import vinn.tekk.screwyourmobs.procedures.EntityRemovalHandler;
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

@Mod(ScrewYourMobsMod.MODID)
public class ScrewYourMobsMod {

    public static final String MODID = "screwyourmobs";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public ScrewYourMobsMod(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(
                ModConfig.Type.COMMON,
                EntityRemovalConfig.SPECIFICATION,
                "sym/screwyourmobs-common.toml"
        );

        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);

        if (FMLLoader.getDist() == Dist.CLIENT) {
            container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        LOGGER.info("ScrewYourMobs! initialized.");
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == EntityRemovalConfig.SPECIFICATION) {
            EntityRemovalHandler.markConfigForReload();
        }
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == EntityRemovalConfig.SPECIFICATION) {
            EntityRemovalHandler.markConfigForReload();
        }
    }
}