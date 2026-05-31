package net.pemiridosa.combind.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.api.InputKey;
import net.pemiridosa.combind.api.KeyCombo;
import net.pemiridosa.combind.impl.CombindConfig;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Redirects {@link KeyMapping#isDown()} and {@link KeyMapping#consumeClick()}
 * for Combind-managed bindings so that in-game actions respond to combo presses
 * rather than raw single-key presses.
 *
 * <p>Vanilla tracks key state via its internal {@code isDown} boolean (updated
 * every tick from physical key state) and {@code clickCount} (incremented by
 * {@code KeyMapping.click()}). Neither mechanism understands multi-key combos,
 * so we intercept at the read site and return our own combo-aware state.
 *
 * <p>When the binding's combo is unbound we fall through to vanilla, preserving
 * normal behavior for any binding that has not been assigned a combo.
 */
@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Shadow public abstract InputConstants.Key getDefaultKey();

    @Inject(
        method = "getTranslatedKeyMessage()Lnet/minecraft/network/chat/Component;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$getTranslatedKeyMessage(CallbackInfoReturnable<Component> cir) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get((KeyMapping)(Object) this);

        if (binding == null)
            return;

        cir.setReturnValue(Component.literal(binding.getComboDisplayName()));
    }

    @Inject(
        method = "isDown()Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$isDown(CallbackInfoReturnable<Boolean> cir) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get((KeyMapping) (Object) this);

        if (binding != null && !binding.getCombo().isUnbound()) {
            // guard on screen == null, mirroring vanilla's KeyMapping.releaseAll() on screen open.
            cir.setReturnValue(binding.isActive() && Minecraft.getInstance().screen == null);
        }
    }

    @Inject(
        method = "consumeClick()Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$consumeClick(CallbackInfoReturnable<Boolean> cir) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get((KeyMapping) (Object) this);

        if (binding != null && !binding.getCombo().isUnbound()) {
            cir.setReturnValue(binding.consumeClick());
        }
    }

    /**
     * Routes vanilla's screen-level key-match check through our combo system.
     *
     * <p>Vanilla screens (e.g. inventory F-swap) call {@code matches()} when a key
     * is pressed. We check the trigger key, then verify every modifier is physically
     * held via GLFW. Querying GLFW directly works even in GUI context where our
     * tracked heldKeys set has been cleared by {@code releaseAll()}.
     *
     * <p>Sequences return false: there is no press-history available in a screen
     * {@code keyPressed()} call.
     */
    @Inject(
        method = "matches(Lnet/minecraft/client/input/KeyEvent;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$matches(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get((KeyMapping)(Object) this);

        if (binding == null)
            return;

        KeyCombo combo = binding.getCombo();

        if (combo.isUnbound() || combo.isSequence()) {
            cir.setReturnValue(false);
            return;
        }

        if (!(combo.triggerKey() instanceof InputKey.Keyboard(int glfwCode)) || glfwCode != event.key()) {
            cir.setReturnValue(false);
            return;
        }

        Window window = Minecraft.getInstance().getWindow();

        for (InputKey mod : combo.modifiers()) {
            if (!isPhysicallyHeld(window, mod)) {
                cir.setReturnValue(false);
                return;
            }
        }

        cir.setReturnValue(true);
    }

    @Inject(
        method = "matchesMouse(Lnet/minecraft/client/input/MouseButtonEvent;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$matchesMouse(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get((KeyMapping)(Object) this);

        if (binding == null)
            return;

        KeyCombo combo = binding.getCombo();

        if (combo.isUnbound() || combo.isSequence()) {
            cir.setReturnValue(false);
            return;
        }

        if (!(combo.triggerKey() instanceof InputKey.Mouse(int glfwButton)) || glfwButton != event.button()) {
            cir.setReturnValue(false); return;
        }

        Window window = Minecraft.getInstance().getWindow();

        for (InputKey mod : combo.modifiers()) {
            if (!isPhysicallyHeld(window, mod)) {
                cir.setReturnValue(false);
                return;
            }
        }

        cir.setReturnValue(true);
    }

    /**
     * Two Combind bindings conflict if their key sets overlap — i.e. any key
     * (trigger or modifier) from one combo appears in the other. This means
     * {@code Left Shift + F} conflicts with both {@code F} (same trigger) and
     * {@code Shift} (modifier used as trigger elsewhere), for example.
     */
    @Inject(
        method = "same(Lnet/minecraft/client/KeyMapping;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$same(KeyMapping that, CallbackInfoReturnable<Boolean> cir) {
        CombindKeyBinding thisBinding = CombindRegistry.INSTANCE.get((KeyMapping)(Object) this);
        CombindKeyBinding otherBinding = CombindRegistry.INSTANCE.get(that);

        if (thisBinding == null || otherBinding == null)
            return; // non-Combind: let vanilla handle

        KeyCombo thisCombo  = thisBinding.getCombo();
        KeyCombo otherCombo = otherBinding.getCombo();

        cir.setReturnValue(thisCombo.overlaps(otherCombo));
    }

    /** Reflects the Combind combo's unbound state into vanilla's isUnbound() check. */
    @Inject(
        method = "isUnbound()Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$isUnbound(CallbackInfoReturnable<Boolean> cir) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get((KeyMapping)(Object) this);

        if (binding == null)
            return;

        cir.setReturnValue(binding.getCombo().isUnbound());
    }

    /**
     * Returns true when the current combo equals the default key's combo, so that
     * the per-key Reset button in the {@code Controls... > Key Binds...} screen is enabled/disabled correctly.
     */
    @Inject(
        method = "isDefault()Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$isDefault(CallbackInfoReturnable<Boolean> cir) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get((KeyMapping) (Object) this);

        if (binding == null)
            return;

        KeyCombo combo = binding.getCombo();

        InputConstants.Key defaultKey = this.getDefaultKey();

        KeyCombo defaultCombo = inputKeyToCombo(defaultKey);

        cir.setReturnValue(combo.equals(defaultCombo));
    }

    /**
     * Syncs the Combind combo whenever vanilla sets a new key (reset / rebind flows).
     */
    @Inject(
        method = "setKey(Lcom/mojang/blaze3d/platform/InputConstants$Key;)V",
        at = @At("TAIL")
    )
    private void combind$setKey(InputConstants.Key key, CallbackInfo ci) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get((KeyMapping) (Object) this);

        if (binding == null)
            return;

        binding.setCombo(inputKeyToCombo(key));

        CombindConfig.save();
    }


    @Unique
    private static boolean isPhysicallyHeld(Window window, InputKey key) {
        return switch (key) {
            case InputKey.Keyboard kb -> InputConstants.isKeyDown(window, kb.glfwCode());
            case InputKey.Mouse    m  -> GLFW.glfwGetMouseButton(window.handle(), m.glfwButton()) == GLFW.GLFW_PRESS;
        };
    }

    @Unique
    private static KeyCombo inputKeyToCombo(InputConstants.Key key) {
        if (key.getType() == InputConstants.Type.KEYSYM) {
            if (key.getValue() == InputConstants.UNKNOWN.getValue())
                return KeyCombo.unbound();

            return new KeyCombo(InputKey.keyboard(key.getValue()));
        } else if (key.getType() == InputConstants.Type.MOUSE) {
            return new KeyCombo(InputKey.mouse(key.getValue()));
        }

        return KeyCombo.unbound();
    }
}
