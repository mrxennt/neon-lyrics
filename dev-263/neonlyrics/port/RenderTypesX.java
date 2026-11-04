package dev.neonlyrics.port;

import java.lang.reflect.Method;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import dev.neonlyrics.port.RenderTypesX;

/**
 * 26.3 made {@code RenderTypesX.create(String, RenderSetup)} package-private, so the mod cannot
 * build custom render types directly. Fabric's access widener covers this at runtime, and this
 * shim covers the compile-time surface with the same call shape.
 */
public final class RenderTypesX {
    private static Method create;

    private RenderTypesX() {
    }

    public static RenderType create(String name, RenderSetup setup) {
        try {
            if (create == null) {
                Method m = RenderType.class.getDeclaredMethod("create", String.class, RenderSetup.class);
                m.setAccessible(true);
                create = m;
            }
            return (RenderType) create.invoke(null, name, setup);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not create RenderType " + name, e);
        }
    }
}
