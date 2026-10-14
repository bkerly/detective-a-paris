# Notes for Claude

French-learning detective game for a 7-year-old. Read `README.md` first, then
`docs/UNIT_FORMAT.md`. The design is in the Claude doc "French detective game:
unit plan". The code is at https://github.com/bkerly/detective-a-paris
(branch `main`); the owner pulls it into Android Studio. Cases 1 to 3
(twenty-four units) are built. The owner will add units and cases.

## Rules

- `core` is plain Kotlin with **no Android imports and no dependencies**. That
  is what lets it be compiled and played by script on any machine. Keep it so.
- `core` must run on Android API 22. Do not call Java 8+ library methods
  there (`Map.getOrDefault`, `Map.forEach { k, v -> }`, `removeIf`,
  `java.time`, `java.nio.file`, streams). Kotlin's own extensions are fine.
- Content goes in the unit JSON, not in Kotlin. A new unit or case should need
  no code. New kinds of scene, new icons and new tiles do.
- Text shown to the child: plain short sentences, one new idea at a time. Each
  new thing (a briefcase, an altar) gets one plain sentence the first time.
- Design rules from the plan: Bonjour first in every conversation (Bonsoir at
  night); the conversation gives a fact the action needs; nothing moves faster
  than a walk; no clock, no lives; no weapons, and the big net is the only way
  to catch anyone; no price above 60; words from earlier units come back.
- English fades: units 1 and 2 show English under each line; from unit 3 help
  shows on a tap; from unit 5 a line of old words has no `en` at all and new
  words get a `gloss`; from unit 7 on the storyteller uses French where she
  knows the words.
- A unit has at most five stages: two notebook sections, two conversations,
  and Go. Each notebook section teaches what the next conversation needs.
  Every unit has its own secret `code` (see README).
- One conversation per unit holds a spoken battle (`"ask": "duel"`, see
  UNIT_FORMAT). She says phrases aloud; a battle never takes anything away,
  and a misheard try only asks "Encore une fois ?". Keep its sound effects
  short and quieter than the voices. Speech recognition stays in the `app`
  module (Vosk); `core` only grades what the platform heard (`Speech`).
- Every button has an `id`; the playthrough check taps by id. Keep ids stable.
- Screens draw on a 400 x 240 canvas. A touch target is at least 22 units tall.
- Word ids are unique across all units.

## After any change

1. `./gradlew :desktop:playthrough` must print `CHECK PASSED`. It validates
   every unit file, then plays every unit. Add unit ids to play only those.
2. Look at the pictures it saves in `desktop/build/playthrough/`.
3. If French text changed: `./gradlew :desktop:lines`, then
   `Rscript tools/make_audio.R --tts ... --voices ...`, then the check again.
   It fails if a line has no sound file.
4. `Rscript tools/make_story.R` and `Rscript tools/make_story.R --short --out docs/STORY_SHORT.md`
   refresh the two story pages. They are made from the unit files; never edit them by hand.

Without Gradle or the Android SDK, compile `core` and `desktop` with `kotlinc`
and run `detective.desktop.Check <assets> <out> [--no-audio] [unit ids]` and
`detective.desktop.Lines <assets> tools/lines.tsv` directly. In that case say
plainly that the `app` module was not built.

## Where to add things

- A new icon: `Icons3.kt` (add the name to `NAMES`, draw it in `draw`). Look
  at it on the art sheet (`detective.desktop.Sheet <assets> <out.png>`) before
  shipping: a hand or finger shape can read as a rude gesture.
- A new tile or sprite: `art/sprites.txt`; a new place: `art/tilesets.json`.
- A new person: `art/looks.json` (a base sprite and colour swaps).
- A new kind of walking-scene step: `WalkScreen.checkQuest` and `goal()`,
  then `Check.validate`.
- A new kind of question in a conversation: `TalkScreen` (buttons, drawing,
  `rightTaps`, `wrongTaps`), then `Check.validate`.
- A new small game: a `MiniGame` subclass in `Games.kt` or `Games3.kt`, its kind in
  `Games.open` and `Games.KINDS`, then `Check.validate` and a player in
  `Check` (see `block()`, `boules()`, `spy()`, `belt()`, `metro()`, `dress()`).

## Scripts

Helper scripts for the owner are written in R (`tools/`).
