package vinn.tekk.screwyourmobs.rules;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Wire-friendly snapshot of every loaded rule. Serializes to NBT for
 * transmission over a CustomPacketPayload.
 *
 * Includes active rules, disabled rules, and per-rule metadata (source
 * display path + world-rule flag). Does not include the raw JsonObject —
 * the client only needs the display data, not the file bytes.
 */
public final class RuleSetSnapshot {

    public record RuleEntry(
            String name,
            boolean disabled,
            boolean isWorldRule,
            String displayPath,
            List<String> entities,       // already-stringified IDs, including #tags
            List<String> dimensions      // already-stringified dimension IDs
    ) {}

    public final List<RuleEntry> rules;

    public RuleSetSnapshot(List<RuleEntry> rules) {
        this.rules = rules;
    }

    // ---- Server side: capture current state ----

    public static RuleSetSnapshot capture() {
        List<RuleEntry> out = new ArrayList<>();

        // Active rules
        for (RemovalRule rule : RuleManager.getAllRules().values()) {
            out.add(entryFor(rule, RuleManager.getSource(rule.name())));
        }

        // Disabled rules
        for (RemovalRule rule : RuleManager.getDisabledRules().values()) {
            out.add(entryFor(rule, RuleManager.getSource(rule.name())));
        }

        return new RuleSetSnapshot(out);
    }

    private static RuleEntry entryFor(RemovalRule rule, RuleSource source) {
        List<String> entities = new ArrayList<>();
        for (ResourceLocation id : rule.entities()) entities.add(id.toString());
        for (TagKey<EntityType<?>> tag : rule.entityTags()) {
            entities.add("#" + tag.location());
        }

        List<String> dimensions = new ArrayList<>();
        for (ResourceLocation dim : rule.dimensions()) dimensions.add(dim.toString());

        String path = source != null ? source.path().toString() : "(unknown)";
        boolean isWorld = source != null && source.isWorldRule();

        return new RuleEntry(
                rule.name(),
                rule.disabled(),
                isWorld,
                path,
                entities,
                dimensions
        );
    }

    // ---- NBT serialization ----

    public CompoundTag toNbt() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();

        for (RuleEntry e : rules) {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", e.name());
            tag.putBoolean("disabled", e.disabled());
            tag.putBoolean("isWorld", e.isWorldRule());
            tag.putString("path", e.displayPath());

            ListTag entities = new ListTag();
            for (String s : e.entities()) entities.add(StringTag.valueOf(s));
            tag.put("entities", entities);

            ListTag dims = new ListTag();
            for (String s : e.dimensions()) dims.add(StringTag.valueOf(s));
            tag.put("dimensions", dims);

            list.add(tag);
        }

        root.put("rules", list);
        return root;
    }

    public static RuleSetSnapshot fromNbt(CompoundTag root) {
        List<RuleEntry> out = new ArrayList<>();
        ListTag list = root.getList("rules", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);

            List<String> entities = new ArrayList<>();
            ListTag entityTag = tag.getList("entities", Tag.TAG_STRING);
            for (int j = 0; j < entityTag.size(); j++) {
                entities.add(entityTag.getString(j));
            }

            List<String> dimensions = new ArrayList<>();
            ListTag dimTag = tag.getList("dimensions", Tag.TAG_STRING);
            for (int j = 0; j < dimTag.size(); j++) {
                dimensions.add(dimTag.getString(j));
            }

            out.add(new RuleEntry(
                    tag.getString("name"),
                    tag.getBoolean("disabled"),
                    tag.getBoolean("isWorld"),
                    tag.getString("path"),
                    entities,
                    dimensions
            ));
        }

        return new RuleSetSnapshot(out);
    }
}