<!-- managed by ~/homelab/bin/claude-md; hand-written notes go below the line -->
# afk-crab-helper

A RuneLite plugin that provides a distraction-reducing overlay when training on the Gemstone Crab, helping you focus on other tasks while AFK training.

## Run
- No gradle wrapper here: build with LightbearerHelper's — `cd ~/projects/afk-crab-helper && ~/projects/LightbearerHelper/gradlew build`

## Where things live
- Git: https://github.com/1504681/afk-crab-helper.git (PUBLIC).

## Rules for this repo
- RuneLite plugin. Process, hub submission and the auto-merge update path: ~/homelab/runbooks/runelite-plugin.md.
- Keep README and Plugin Hub PR bodies short: what it does, settings, changelog. No gradle/run boilerplate in the README.
- Keep `runelite-plugin.properties` and `runelite_plugin.json` in sync; bump the version constant + changelog with every hub update.
- Verify RuneLite API symbols offline with `javap -cp ~/.gradle/caches/modules-2/files-2.1/net.runelite/<jar>` before coding against them.

## Conventions
Box-wide conventions and the port map: ~/homelab (BOX.md is imported into every session). Registry: add/edit services.toml then bin/apply. No Claude attribution or session links in anything pushed.

<!-- hand-written notes below this line are preserved by bin/claude-md -->
---
