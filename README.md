# Neonlyrics

**Spotify-style synced karaoke lyrics in 3D in the sky** — a client-side Fabric mod for Minecraft **26.3**.

Neonlyrics shows the lyrics of whatever music you're currently listening to, synced line by line, in three places at once: as floating 3D lyrics in the sky, on a HUD lyric bar at the top of the screen, and it re-styles the whole game look while it's at it (custom animated skies, glass hotbar, custom fonts).

## Features

### Lyrics
- **3D world lyrics** — the current line floats in the sky / at your eye level, with glow, color, size, opacity and animation all configurable
- **HUD lyric bar** — clean top bar with the current and next line
- **Word-level karaoke** — LRCLIB word timestamps let individual words light up as they're sung, when a track provides them
- **Lyrics sources** — [LRCLIB](https://lrclib.net/), free and open, no keys required
- **Music sources**
  - **Spotify** — connect your own Spotify app via OAuth (client ID setup in-game), with play/pause/skip controls
  - **Any local player** — on Windows the mod reads the system media session, so browser players, Spotify, Winamp, anything works without any account
- **Album covers** — resolved via the iTunes Search API
- **Lyrics translation** — on-the-fly, with configurable target language
- **Favorites** — save the songs whose lyrics you like, offline cache included
- **Timing offset** — nudge the lyrics in milliseconds if your player drifts

### Skies
- **28 animated sky presets** (Synthwave, Aurora, Nebula, Black Hole, Neon City, Sakura, Inferno, Matrix, Thunderstorm, …) plus a **fully configurable Custom sky**
- **Real weather sync**, day/night switching, sky music-reactive effects, constellations, aurora, beams, streaming stars — all toggles
- The sky also fades the fog so terrain blends into the view instead of vanilla blue

### Look & feel
- **Glass hotbar**, HUD island with clock, ping and FPS, crosshair styles, startup intro
- **Main menu overhaul** — the menu in front of a live sky of your choice
- **22 included font families** (separately configurable for UI, lyrics and sky)
- **Beat glow, song intro flashes, weather particles, sound effects**
- **Death effects** — a configurable send-off when you die, others' included optionally
- **Performance** — quality presets, FPS boost mode and automatic performance scaling that keeps the game smooth while the sky is heavy

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | >= 0.20.1 |
| Fabric API | latest for 26.3 |
| Java | >= 25 |

Client-only — install it on your game, servers don't need anything.

## Install

1. Download the latest `neon-lyrics-2.1.0+mc26.3.jar` from [Releases](../../releases) (or your distribution source).
2. Drop it into your `.minecraft/mods` folder (same for MultiMC/CurseForge instances).
3. Launch the game, open the Neonlyrics screen and pick a sky and lyrics mode.

For Spotify connect, the in-game tutorial screen walks you through creating a Spotify app and entering the client ID.

## Building from source

```bash
./gradlew build
```

The jar ends up in `build/libs/neon-lyrics-2.1.0+mc26.3.jar`.

Notes for building on your machine:

- Sources are Java 25 bytecode (`--release 25`); compilation forks onto a **JDK 26** install — set its location in the `forkOptions.javaHome` line of `build.gradle`.
- Gradle itself runs on a **JDK 25** — set `org.gradle.java.home` in `gradle.properties` to your JDK 25 path, or remove the line if Gradle already runs on one.

## Project layout

```
dev/neonlyrics/            original 1.21.11 sources (unmaintained, kept for reference)
dev-263/neonlyrics/        current 26.3 sources — the tree that builds
assets/                    resources packed into the jar
```

## License

[MIT](LICENSE) — do whatever, attribution appreciated. Lyrics and metadata come from LRCLIB/iTunes and belong to their rights holders. Not affiliated with Mojang, Microsoft or Spotify.
