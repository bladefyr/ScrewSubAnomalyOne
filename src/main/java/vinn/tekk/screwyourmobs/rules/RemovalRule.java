package vinn.tekk.screwyourmobs.rules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

/**
 * A single additive removal rule.
 * If any entity in {@code entities} spawns in any dimension in {@code dimensions},
 * it is removed. Empty {@code dimensions} means "all dimensions".
 */
public record RemovalRule(
        String name,
        Set<ResourceLocation> entities,
        Set<TagKey<EntityType<?>>> entityTags,
        Set<ResourceLocation> dimensions
) {
    public boolean isGlobal() {
        return dimensions.isEmpty();
    }

    public boolean appliesToDimension(ResourceLocation dimId) {
        return isGlobal() || dimensions.contains(dimId);
    }

    public boolean matchesEntity(ResourceLocation entityId) {
        return entities.contains(entityId);
    }
}