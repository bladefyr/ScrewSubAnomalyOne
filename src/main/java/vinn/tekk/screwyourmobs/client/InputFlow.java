package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class InputFlow {

    private InputFlow() {}

    @FunctionalInterface
    public interface Validator {
        ValidationResult check(String input);
    }

    public record ValidationResult(boolean valid, String suggestion) {
        public static ValidationResult ok() { return new ValidationResult(true, null); }
        public static ValidationResult bad(String suggestion) {
            return new ValidationResult(false, suggestion);
        }
    }

    public static void prompt(Minecraft mc, Screen parent, Component title,
                              List<String> options, String initialValue,
                              Validator validator, Consumer<String> onSuccess) {
        prompt(mc, parent, title, options, initialValue, Set.of(),
                validator, onSuccess);
    }

    public static void prompt(Minecraft mc, Screen parent, Component title,
                              List<String> options, String initialValue,
                              Set<String> markedOptions,
                              Validator validator, Consumer<String> onSuccess) {
        mc.setScreen(new InputScreen(parent, title, options, initialValue,
                markedOptions, entered -> {
            if (entered == null) {
                mc.setScreen(parent);
                return;
            }
            validateAndProceed(mc, parent, title, options, markedOptions,
                    entered, validator, onSuccess);
        }));
    }

    private static void validateAndProceed(Minecraft mc,
                                           Screen parent,
                                           Component title,
                                           List<String> options,
                                           Set<String> markedOptions,
                                           String input,
                                           Validator validator,
                                           Consumer<String> onSuccess) {
        ValidationResult result = validator.check(input);
        if (result.valid()) {
            mc.setScreen(parent);
            onSuccess.accept(input);
            return;
        }

        mc.setScreen(new CorrectionScreen(parent, title, input, result.suggestion(), outcome -> {
            switch (outcome) {
                case ACCEPT_SUGGESTION -> {
                    if (result.suggestion() != null) {
                        mc.setScreen(parent);
                        onSuccess.accept(result.suggestion());
                    }
                }
                case EDIT -> prompt(mc, parent, title, options, input,
                        markedOptions, validator, onSuccess);
                case CANCEL -> mc.setScreen(parent);
            }
        }));
    }
}