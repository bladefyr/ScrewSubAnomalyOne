package vinn.tekk.screwyourmobs.rules;

import java.nio.file.Path;

public record RuleSource(String name, Path path, boolean isWorldRule) {}