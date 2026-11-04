package dev.neonlyrics.port;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * KeyMapping that refuses to let vanilla query SDL with an invalid key code.
 *
 * <p>26.3's {@code KeyMapping#setAll()} walks every keyboard binding whose key differs from
 * {@link InputConstants#UNKNOWN} and calls {@code InputConstants.isKeyDown(value)}, which is
 * {@code SDL_GetKeyboardState().get(index)} on a fixed-size buffer. A binding stored as
 * {@code key.keyboard.-1} (what older builds of this mod wrote for its unbound keys) therefore threw
 * {@code IndexOutOfBoundsException} from {@code MouseHandler#grabMouse} every time a screen opened
 * or closed - observed as a crash while the world finished loading.
 *
 * <p>{@link #shouldSetOnIngameFocus()} is vanilla's own gate for that query, so closing it for
 * out-of-range codes makes a corrupt options.txt harmless instead of fatal.
 */
public class SafeKeyMapping extends KeyMapping {
    /** Length of the array behind {@code SDL_GetKeyboardState()}. */
    public static final int SDL_KEY_COUNT = 512;

    public SafeKeyMapping(String name, InputConstants.Type type, int code, Category category) {
        super(name, type, code, category);
    }

    /** True when {@link InputConstants#isKeyDown(int)} can be called with this key's code. */
    public static boolean inRange(InputConstants.Key key) {
        if (key == null || key.getType() != InputConstants.Type.KEYBOARD) {
            return true;
        }
        int value = key.getValue();
        return value >= 0 && value < SDL_KEY_COUNT;
    }

    @Override
    protected boolean shouldSetOnIngameFocus() {
        return inRange(KeyMappingHelper.getBoundKeyOf(this)) && super.shouldSetOnIngameFocus();
    }
}
