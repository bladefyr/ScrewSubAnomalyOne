package vinn.tekk.screwyourmobs.rules;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire-friendly snapshot of every loaded rule plus the current dimension list
 * and the server's validation warnings.
 * Serializes to NBT for transmission over a CustomPacketPayload.
 */
public record RuleSetSnapshot(
        List<RuleEntry> rules,
        List<String> knownDimensions,
        List<String> warnings
) {

    public record RuleEntry(
            String name,
            boolean disabled,
            boolean isWorldRule,
            String displayPath,
            List<String> entities,
            List<String> dimensions
    ) {
    }

    // ---- Server side: capture current state ----

    public static RuleSetSnapshot capture() {
        List<RuleEntry> out = new ArrayList<>();

        for (RemovalRule rule : RuleManager.getAllRules().values()) {
            out.add(entryFor(rule, RuleManager.getSource(rule.name())));
        }
        for (RemovalRule rule : RuleManager.getDisabledRules().values()) {
            out.add(entryFor(rule, RuleManager.getSource(rule.name())));
        }

        List<String> dims = new ArrayList<>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.levelKeys().forEach(key -> dims.add(key.location().toString()));
        }
        dims.sort(String::compareToIgnoreCase);

        List<String> warnings = new ArrayList<>(RuleManager.getLastWarnings());

        return new RuleSetSnapshot(out, dims, warnings);
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

        ListTag dims = new ListTag();
        for (String d : knownDimensions) dims.add(StringTag.valueOf(d));
        root.put("knownDimensions", dims);

        ListTag warnList = new ListTag();
        for (String w : warnings) warnList.add(StringTag.valueOf(w));
        root.put("warnings", warnList);

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

        List<String> knownDims = new ArrayList<>();
        ListTag knownDimTag = root.getList("knownDimensions", Tag.TAG_STRING);
        for (int i = 0; i < knownDimTag.size(); i++) {
            knownDims.add(knownDimTag.getString(i));
        }

        List<String> warnings = new ArrayList<>();
        ListTag warnTag = root.getList("warnings", Tag.TAG_STRING);
        for (int i = 0; i < warnTag.size(); i++) {
            warnings.add(warnTag.getString(i));
        }

        return new RuleSetSnapshot(out, knownDims, warnings);
    }
}