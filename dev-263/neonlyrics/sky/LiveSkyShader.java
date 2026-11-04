package dev.neonlyrics.sky;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import dev.neonlyrics.NeonLyrics;
import dev.neonlyrics.config.Config;
import dev.neonlyrics.config.Storage;
import java.nio.file.Files;
import java.time.LocalDateTime;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import dev.neonlyrics.port.RenderTypesX;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;

public final class LiveSkyShader {
   private static final Identifier TEX = Identifier.fromNamespaceAndPath("neonlyrics", "live_sky_params");
   private static final RenderPipeline PIPE = RenderPipeline.builder(new Snippet[0])
      .withLocation(Identifier.fromNamespaceAndPath("neonlyrics", "pipeline/live_sky"))
      .withVertexShader(Identifier.fromNamespaceAndPath("neonlyrics", "core/live_sky"))
      .withFragmentShader(Identifier.fromNamespaceAndPath("neonlyrics", "core/live_sky"))
      .withBindGroupLayout(BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER).build())
      .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
      
      .withCull(false)
      .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
      .build();
   private static RenderType type;
   private static DynamicTexture tex;
   private static int state = 0;
   private static final int[] last = new int[16];
   private static final int[] next = new int[16];

   private LiveSkyShader() {
   }

   public static boolean ready() {
      if (state != 0) {
         return state > 0;
      } else {
         try {
            if (false) {
               NeonLyrics.LOG.warn("Himmel-Shader laesst sich nicht kompilieren - nehme den alten Himmel");
               state = -1;
               return false;
            }

            NativeImage img = new NativeImage(16, 1, false);
            tex = new DynamicTexture(() -> "neonlyrics_live_sky", img);
            Minecraft.getInstance().getTextureManager().register(TEX, tex);
            type = RenderTypesX.create("neonlyrics_live_sky", RenderSetup.builder(PIPE).withTexture("Sampler0", TEX).createRenderSetup());
            state = 1;
            NeonLyrics.LOG.info("Himmel-Shader aktiv");
         } catch (Throwable var1) {
            NeonLyrics.LOG.warn("Himmel-Shader nicht verfuegbar - nehme den alten Himmel", var1);
            state = -1;
         }

         status(state > 0 ? "aktiv" : "aus (alter Himmel)");
         return state > 0;
      }
   }

   private static void status(String s) {
      try {
         String gpu = "";

         try {
            gpu = String.valueOf(RenderSystem.getDevice());
         } catch (Throwable var3) {
         }

         Files.writeString(Storage.dir().resolve("sky-status.txt"), LocalDateTime.now() + "\nHimmel-Shader: " + s + "\nGrafik: " + gpu + "\n");
      } catch (Throwable var4) {
      }
   }

   public static void disable(Throwable t) {
      NeonLyrics.LOG.warn("Himmel-Shader abgeschaltet", t);
      state = -1;
      status("Fehler beim Zeichnen: " + t);
   }

   public static RenderType type() {
      return type;
   }

   public static void params(SkyPreset p, int a1, int a2, double t, Config c, Matrix4f pose) {
      float pattern = 1.0F;
      float lively = 0.6F;
      float breath = 1.0F;
      float aurora = 1.0F;
      if (c != null) {
         pattern = Math.max(0.2F, Math.min(1.5F, c.skyPattern));
         lively = Math.max(0.0F, Math.min(1.0F, c.skyLively));
         breath = c.skyBreath <= 0.01F ? 1.0F : 0.85F + 0.15F * (float)Math.sin(t * 0.45 * Math.max(0.2F, c.skyBreath));
         aurora = c.fxAurora ? 1.0F : 0.0F;
      }

      next[0] = rgba(p.top, 255);
      next[1] = rgba(p.horizon, 255);
      next[2] = rgba(a1, 255);
      next[3] = rgba(a2, 255);
      LiveSky.Body b = LiveSky.body(p);
      next[4] = b == null ? 0 : rgba(b.color(), b.kind() + 1);
      next[5] = bytes(b8(pattern / 1.5F), b8(lively), b8(breath), b8(aurora));
      next[6] = b == null ? 0 : pair(b.az() * 100.0, (b.el() + 90.0) * 100.0);
      next[7] = (b == null ? 0 : pair(b.radius() * 1000.0, 0.0) & -65536) | p.ordinal() << 8;
      Matrix3f inv = new Matrix3f().set(pose).invert();
      float[] m = new float[]{inv.m00, inv.m01, inv.m02, inv.m10, inv.m11, inv.m12, inv.m20, inv.m21, inv.m22, 0.0F};

      for (int i = 0; i < 5; i++) {
         next[8 + i] = pair(enc(m[i * 2]), enc(m[i * 2 + 1]));
      }

      boolean changed = false;

      for (int i = 0; i < 16; i++) {
         if (next[i] != last[i]) {
            last[i] = next[i];
            changed = true;
            int v = next[i];               // The packed value is ARGB (see rgba()/pair()). setPixelABGR would interpret it as
               // 0xAABBGGRR, swapping R/B and scrambling every 16-bit pair the shaders decode, which is
               // what made the sky render as purple, geometrically broken patterns.
               tex.getPixels().setPixel(i, 0, (v & 0xFF) << 24 | v >>> 8);
         }
      }

      if (changed) {
         tex.upload();
      }
   }

   private static int pair(double v1, double v2) {
      int a = (int)Math.max(0L, Math.min(65535L, Math.round(v1)));
      int b = (int)Math.max(0L, Math.min(65535L, Math.round(v2)));
      return a << 16 | b;
   }

   private static double enc(float v) {
      return (Math.max(-2.0F, Math.min(2.0F, v)) + 2.0) / 4.0 * 65535.0;
   }

   private static int b8(float v) {
      return Math.max(0, Math.min(255, Math.round(v * 255.0F)));
   }

   private static int rgba(int rgb, int a) {
      return (rgb & 16777215) << 8 | a & 0xFF;
   }

   private static int bytes(int r, int g, int b, int a) {
      return r << 24 | g << 16 | b << 8 | a;
   }
}
