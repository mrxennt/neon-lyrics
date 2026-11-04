package dev.neonlyrics;

import dev.neonlyrics.config.Config;
import dev.neonlyrics.config.Storage;
import dev.neonlyrics.cosmetic.DeathFx;
import dev.neonlyrics.cosmetic.PhoneMenu;
import dev.neonlyrics.gui.LicenseScreen;
import dev.neonlyrics.hud.BeatGlow;
import dev.neonlyrics.hud.Crosshair;
import dev.neonlyrics.hud.GlassHotbar;
import dev.neonlyrics.hud.Island;
import dev.neonlyrics.hud.StartupIntro;
import dev.neonlyrics.lyrics.Favorites;
import dev.neonlyrics.lyrics.SpotifyClient;
import dev.neonlyrics.port.SafeKeyMapping;
import dev.neonlyrics.sky.SkyRenderer;
import dev.neonlyrics.sky.WorldSky;
import dev.neonlyrics.util.AutoPerf;
import dev.neonlyrics.util.Fx;
import dev.neonlyrics.util.Profiles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import com.mojang.blaze3d.platform.InputConstants.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NeonLyrics implements ClientModInitializer {
   public static final String MOD_ID = "neonlyrics";
   public static final Logger LOG = LoggerFactory.getLogger("NeonLyrics");
   private static Config config = new Config();
   public static KeyMapping keyMenu;
   public static KeyMapping keySkip;
   public static KeyMapping keyPrev;
   public static KeyMapping keyPause;
   public static KeyMapping keyLyrics;
   public static KeyMapping keySync;
   public static KeyMapping keyFav;
   public static KeyMapping keyClip;
   public static KeyMapping keyProfile;
   private static boolean licenseHint;

   public static Config cfg() {
      return config;
   }

   public static void saveConfig() {
      config.clamp();
      Storage.save("config.json", config);
   }

   public void onInitializeClient() {
      Storage.migrateFromAuction();
      config = Storage.load("config.json", Config.class, Config::new);
      config.clamp();
      saveConfig();
      Category cat = Category.register(Identifier.fromNamespaceAndPath("neonlyrics", "main"));
      keyMenu = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.openmenu", InputConstants.Type.KEYBOARD, 74, cat));
      keySkip = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.skip", InputConstants.Type.KEYBOARD, 298, cat));
      // Unbound defaults must be InputConstants.UNKNOWN (KEYBOARD, value 0). Anything outside SDL's
      // keyboard-state range makes KeyMapping#shouldSetOnIngameFocus true, and KeyMapping.setAll then
      // calls InputConstants.isKeyDown(value), which indexes SDL's state buffer unchecked and throws
      // IndexOutOfBoundsException the moment a world finishes loading.
      keyPrev = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.prev", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), cat));
      keyPause = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.pause", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), cat));
      keyFav = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.fav", InputConstants.Type.KEYBOARD, 78, cat));
      keyClip = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.clip", InputConstants.Type.KEYBOARD, 299, cat));
      keyProfile = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.profile", InputConstants.Type.KEYBOARD, 297, cat));
      keySync = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.sync", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), cat));
      keyLyrics = KeyMappingHelper.registerKeyMapping(new SafeKeyMapping("key.neonlyrics.lyrics", InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), cat));
      // Older builds wrote "key.keyboard.-1" into options.txt for the unbound keys above. Vanilla parses
      // that straight back into a KEYBOARD key with value -1, which KeyMapping#setAll feeds to
      // InputConstants.isKeyDown and crashes on world load, so self-heal such bindings here (this runs
      // after options.txt is read and before the first setAll).
      for (KeyMapping k : new KeyMapping[]{keyMenu, keySkip, keyPrev, keyPause, keyFav, keyClip, keyProfile, keySync, keyLyrics}) {
         InputConstants.Key bound = KeyMappingHelper.getBoundKeyOf(k);
         if (!SafeKeyMapping.inRange(bound)) {
            k.setKey(InputConstants.UNKNOWN);
            LOG.warn("Ungueltige Tastenbelegung {}={} fuer {} zurueckgesetzt", bound.getName(), bound.getValue(), k.getName());
         }
      }
      HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath("neonlyrics", "sky"), SkyRenderer::render);
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("neonlyrics", "island"), Island::render);
      HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, GlassHotbar::wrap);
      HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, Crosshair::wrap);
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("neonlyrics", "beatglow"), BeatGlow::render);
      LevelRenderEvents.COLLECT_SUBMITS.register(WorldSky::render);
      SpotifyClient.init();
      StartupIntro.register();
      AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, level, hand, entity, hit) -> {
         DeathFx.onAttack(entity);
         return InteractionResult.PASS;
      });
      ClientTickEvents.END_CLIENT_TICK.register(this::tick);
      ClientTickEvents.START_CLIENT_TICK.register(PhoneMenu::startTick);
      LOG.info("Lyrics Cord geladen");
   }

   private void tick(Minecraft mc) {
      if (!License.ok()) {
         while (keyMenu.consumeClick()) {
            if (mc.gui.screen() == null) {
               mc.setScreenAndShow(new LicenseScreen());
            }
         }

         for (KeyMapping k : new KeyMapping[]{keySkip, keyPrev, keyPause, keySync, keyFav, keyClip, keyProfile, keyLyrics}) {
            while (k.consumeClick()) {
            }
         }

         if (!licenseHint && mc.player != null) {
            licenseHint = true;
            mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("neonlyrics.lic.chat"));
         }
      } else {
         AutoPerf.tick(mc);
         PhoneMenu.tick(mc);
         DeathFx.tick(mc);
         SkyRenderer.tick(mc);

         while (keyMenu.consumeClick()) {
            if (mc.gui.screen() == null) {
               PhoneMenu.open(mc);
            }
         }

         while (keySkip.consumeClick()) {
            SpotifyClient.control(SpotifyClient.Cmd.NEXT);
         }

         while (keyPrev.consumeClick()) {
            SpotifyClient.control(SpotifyClient.Cmd.PREVIOUS);
         }

         while (keyPause.consumeClick()) {
            SpotifyClient.control(SpotifyClient.Cmd.PLAYPAUSE);
         }

         while (keySync.consumeClick()) {
            SpotifyClient.syncHere();
         }

         while (keyFav.consumeClick()) {
            Favorites.saveCurrent();
         }

         while (keyClip.consumeClick()) {
            mc.gui.hud.toggle();
         }

         while (keyProfile.consumeClick()) {
            Profiles.toggle();
         }

         while (keyLyrics.consumeClick()) {
            config.lyricsSky = !config.lyricsSky;
            saveConfig();
            Island.notify(Fx.t(config.lyricsSky ? "neonlyrics.media.lyricson" : "neonlyrics.media.lyricsoff"), config.lyricsSky);
         }
      }
   }
}
