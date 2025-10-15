package com.bladelicious.screwsubanomalyone;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScrewSubanomalyoneMod {
	public static final Logger LOGGER = LogManager.getLogger(ScrewSubanomalyoneMod.class);
	public static final String MODID = "screw_subanomalyone";

	public ScrewSubanomalyoneMod(IEventBus modEventBus) {
		NeoForge.EVENT_BUS.register(this);
		LOGGER.info("ScrewSubAnomaly1 successfully loaded!!11!111!1!!");
	}
}