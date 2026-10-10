package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import vinn.tekk.screwyourmobs.client.gui.CorrectionScreen;
import vinn.tekk.screwyourmobs.client.gui.InputScreen;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class InputFlow {

    private InputFlow() {}

    @FunctionalInterface
    public interface Validator {
        ValidationResult check(String input);
    }

    public record ValidationResult(boolean valid, String suggestion, String reason) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null, null);
        }
        public static ValidationResult bad(String suggestion) {
            return new ValidationResult(false, suggestion, null);
        }
        public static ValidationResult bad(String suggestion, String reason) {
            return new ValidationResult(false, suggestion, reason);
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
        promptInternal(mc, parent, title, options, initialValue, markedOptions,
                validator, onSuccess, false);
    }

    /**
     * Same as {@link #prompt} but the input stays open after each successful
     * submission. The user can add several entries in a row without leaving
     * the input screen.
     */
    public static void promptAndStay(Minecraft mc, Screen parent, Component title,
                                     List<String> options, String initialValue,
                                     Set<String> markedOptions,
                                     Validator validator, Consumer<String> onSuccess) {
        promptInternal(mc, parent, title, options, initialValue, markedOptions,
                validator, onSuccess, true);
    }

    private static void promptInternal(Minecraft mc, Screen parent, Component title,
                                       List<String> options, String initialValue,
                                       Set<String> markedOptions,
                                       Validator validator, Consumer<String> onSuccess,
                                       boolean stayOpen) {
        mc.setScreen(new InputScreen(parent, title, options, initialValue,
                markedOptions,
                entered -> {
                    if (entered == null) {
                        mc.setScreen(parent);
                        return;
                    }
                    validateAndProceed(mc, parent, title, options, markedOptions,
                            entered, validator, onSuccess, stayOpen);
                },
                stayOpen ? value -> validateAndProceed(mc, parent, title, options,
                        markedOptions, value, validator, onSuccess, true) : null
        ));
    }

    private static void validateAndProceed(Minecraft mc,
                                           Screen parent,
                                           Component title,
                                           List<String> options,
                                           Set<String> markedOptions,
                                           String input,
                                           Validator validator,
                                           Consumer<String> onSuccess,
                                           boolean stayOpen) {
        ValidationResult result = validator.check(input);

        if (result.valid()) {
            onSuccess.accept(input);
            if (stayOpen) {
                // Reopen a fresh input on top of the parent so the user can keep adding
                promptInternal(mc, parent, title, options, "", markedOptions,
                        validator, onSuccess, true);
            } else {
                mc.setScreen(parent);
            }
            return;
        }

        mc.setScreen(new CorrectionScreen(parent, title, input,
                result.suggestion(), result.reason(), outcome -> {
            switch (outcome) {
                case ACCEPT_SUGGESTION -> {
                    if (result.suggestion() != null) {
                        onSuccess.accept(result.suggestion());
                        if (stayOpen) {
                            promptInternal(mc, parent, title, options, "", markedOptions,
                                    validator, onSuccess, true);
                        } else {
                            mc.setScreen(parent);
                        }
                    }
                }
                case EDIT -> promptInternal(mc, parent, title, options, input,
                        markedOptions, validator, onSuccess, stayOpen);
                case CANCEL -> mc.setScreen(parent);
            }
        }));
    }
}