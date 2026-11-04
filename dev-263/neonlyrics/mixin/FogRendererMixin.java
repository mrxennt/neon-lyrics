package dev.neonlyrics.mixin;

import dev.neonlyrics.sky.WorldSky;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tints the world fog towards the live sky colour so terrain fades into the sky instead of into
 * vanilla's blue.
 *
 * <p>26.3 removed {@code FogRenderer#getFogColor(Camera, float, ClientLevel, int, float)} - the
 * method this mixin used to target - and moved the computation into the private
 * {@code computeFogColor(Camera, float, ClientLevel, int, float, Vector4f)}, which writes into an
 * out parameter that {@code setupFog} then packs into fog data. The old injection carried
 * {@code require = 0}, so instead of failing it silently did nothing and distant terrain kept
 * vanilla fog under a custom sky - visible as a hard, shifting seam where land met sky.
 *
 * <p>{@code require = 1} now: if this target ever disappears again the client says so instead of
 * quietly rendering the wrong fog.
 */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
   @Inject(
      method = {"computeFogColor(Lnet/minecraft/client/Camera;FLnet/minecraft/client/multiplayer/ClientLevel;IFLorg/joml/Vector4f;)V"},
      at = {@At("TAIL")},
      require = 1
   )
   private void neonlyrics$skyFog(Camera camera, float pt, ClientLevel level, int renderDistance, float darken, Vector4f out, CallbackInfo ci) {
      if (camera.getFluidInCamera() == FogType.NONE && out != null) {
         WorldSky.tintFog(out);
      }
   }
}
