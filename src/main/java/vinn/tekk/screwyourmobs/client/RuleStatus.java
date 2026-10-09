package vinn.tekk.screwyourmobs.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public enum RuleStatus {
    GOOD("good", "screwyourmobs.screen.status.good"),
    PROBLEM("problem", "screwyourmobs.screen.status.problem"),
    BAD("bad", "screwyourmobs.screen.status.bad");

    private final String textureName;
    private final String translationKey;

    RuleStatus(String textureName, String translationKey) {
        this.textureName = textureName;
        this.translationKey = translationKey;
    }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(
                "screwyourmobs", "textures/gui/status_" + textureName + ".png");
    }

    public Component label() {
        return Component.translatable(translationKey);
    }
}