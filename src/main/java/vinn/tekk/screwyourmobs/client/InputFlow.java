package vinn.tekk.screwyourmobs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Orchestrates the prompt → validate → correct → callback loop.
 *
 * One call site does all of:
 *  - opens InputScreen for the user to type
 *  - runs the validator on submit
 *  - if valid: calls onSuccess
 *  - if invalid: opens CorrectionScreen with the suggested fix
 *  - handles Use / Edit / Cancel outcomes
 */
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

    /**
     * @param parent       screen to return to on cancel
     * @param title        shown on the input screen
     * @param options      autocomplete candidates (may be empty for no autocomplete)
     * @param initialValue pre-filled text (may be null)
     * @param validator    checks input, produces suggestion if invalid
     * @param onSuccess    called with a validated value
     */
    public static void prompt(Minecraft mc,
                              Screen parent,
                              Component title,
                              List<String> options,
                              String initialValue,
                              Validator validator,
                              Consumer<String> onSuccess) {
        mc.setScreen(new InputScreen(parent, title, options, initialValue, entered -> {
            if (entered == null) {
                // cancelled — return to parent
                mc.setScreen(parent);
                return;
            }
            validateAndProceed(mc, parent, title, options, entered, validator, onSuccess);
        }));
    }

    private static void validateAndProceed(Minecraft mc,
                                           Screen parent,
                                           Component title,
                                           List<String> options,
                                           String input,
                                           Validator validator,
                                           Consumer<String> onSuccess) {
        ValidationResult result = validator.check(input);
        if (result.valid()) {
            onSuccess.accept(input);
            return;
        }

        mc.setScreen(new CorrectionScreen(parent, title, input, result.suggestion(), outcome -> {
            switch (outcome) {
                case ACCEPT_SUGGESTION -> {
                    if (result.suggestion() != null) {
                        onSuccess.accept(result.suggestion());
                    }
                }
                case EDIT -> prompt(mc, parent, title, options, input, validator, onSuccess);
                case CANCEL -> mc.setScreen(parent);
            }
        }));
    }
}