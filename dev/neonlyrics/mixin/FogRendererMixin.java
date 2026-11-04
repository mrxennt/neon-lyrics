package dev.neonlyrics.mixin;

import dev.neonlyrics.sky.WorldSky;
import net.minecraft.client.render.Camera;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FogRenderer.class})
public abstract class FogRendererMixin {
   @Inject(
      method = {"getFogColor(Lnet/minecraft/Camera;FLnet/minecraft/ClientWorld;IF)Lorg/joml/Vector4f;"},
      at = {@At("RETURN")},
      cancellable = true,
      require = 0
   )
   private void neonlyrics$skyFog(Camera camera, float pt, ClientWorld level, int renderDistance, float darken, CallbackInfoReturnable<Vector4f> cir) {
      if (camera.getSubmersionType() == CameraSubmersionType.NONE) {
         Vector4f v = (Vector4f)cir.getReturnValue();
         if (v != null && WorldSky.tintFog(v)) {
            cir.setReturnValue(v);
         }
      }
   }
}
