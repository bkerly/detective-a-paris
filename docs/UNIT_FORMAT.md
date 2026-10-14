# Unit file format

Everything a unit teaches, says and shows is data. A new unit is a new JSON
file; a new case is a new entry in the index. No Kotlin changes are needed
unless you want a kind of scene the game does not have yet.

All paths are under `app/src/main/assets/`.

| File | Holds |
| --- | --- |
| `units/index.json` | The cases, and the units in each, in playing order |
| `units/<id>.json` | One unit |
| `art/sprites.txt` | Pixel art as text: one letter is one pixel |
| `art/tilesets.json` | For each kind of place, which map letter is which tile |
| `art/looks.json` | People and moving things: a base sprite plus a change of colours |
| `audio/manifest.json` | Which sound file belongs to which line. Written by `tools/make_audio.R` |

The units are full examples. `unit1.json` is the simplest; `unit5.json` has a
sketch and a chase; `unit8.json` has darkness; `unit18.json` a car chase,
`unit20.json` the metro, `unit22.json` a plan and dressing the models.

## Adding a unit or a case

1. Write `units/<id>.json`.
2. Add `{"id": "<id>", "title": "..."}` to a case in `units/index.json`, or add
   a new case: `{"id": "case2", "title": "...", "units": [...]}`. Units open in
   the order listed, each when the one before it is finished. With more than
   one case, the case file shows a **Cases** button.
3. `./gradlew :desktop:playthrough` checks the file and plays the unit by
   script. It names anything inconsistent: an unknown icon, a conversation
   step that leads nowhere, a letter the tile set does not have.
4. `./gradlew :desktop:lines`, then `Rscript tools/make_audio.R ...` to make
   the sound for the new French lines. Run the playthrough again; it fails if
   a line has no sound file.

Word ids must be unique across all units, because the notebook and the
"words come back" drills look words up by id.

## A line of speech

Many places hold a "line": something a character says.

| Field | Meaning |
| --- | --- |
| `fr` | The French shown on screen. `{name}` becomes the detective's name |
| `en` | The English for it. Leave it out when every word is an old one |
| `gloss` | A list such as `["le filet = the net"]`: help for the new words only, in place of `en` |
| `who` | Who speaks. Looked up in `voices` to pick the voice |
| `say` | What the voice reads, when it differs from `fr`. Needed if `fr` has `{name}` or "…" |
| `mood` | The face: `neutral`, `smile`, `cold`, `confused`, `busy`, and the feelings `angry`, `sad`, `scared`, `shy` (embarrassed), `curious` |
| `lost` | `true` shows "? ? ?" in place of the help: she cannot follow this line |
| `test` | `true` hides the help until two wrong tries: this line is the test |
| `mute` | `true` if the line needs no sound file |

A space before `?` `!` `:` `;` is kept from breaking, and `'` is drawn curly.

## Top level of a unit

| Field | Meaning |
| --- | --- |
| `id`, `number`, `title` | `"unit3"`, `3`, `"Hotel check-in"` |
| `english` | `"under"`: English under each line (units 1 and 2). `"tap"`: help shows only when she taps the line (units 3 on). In either mode a line with no `en` and no `gloss` has no help |
| `intro` | Name of the story shown the first time the unit is opened |
| `code` | The secret code that opens this unit: capital letters only, 4 to 12 of them, no accents, different from every other unit's code |
| `voices` | Map from a speaker to a voice in `tools/make_audio.R`. `teacher` reads word cards; `hero` reads the player's replies. Every `who` in the unit needs an entry |
| `hero` | Optional colour swaps for the detective's sprite, e.g. `{"h": "f2b233", "w": "3a9e6a"}` for a yellow hat and green scarf |
| `stages` | The cards on the case file, in order (see below) |
| `case` | Sentences for the Case tab of the notebook |
| `words`, `bonus` | Words taught in the notebook; words found in walking scenes |
| `tips` | Culture tips: `id`, `title`, `text` |
| `notebook.pages` | The drill pages |
| `story` | Named lists of story cards |
| `scenes` | Named pictures for story cards |
| `talks`, `walks`, `rides`, `sketches`, `games` | Named scenes of each kind |
| `ticket` | A boarding pass: `airline`, `to`, `fields` as `[label, value]` pairs |

## Stages and steps

A stage is one sitting. `id`, `title`, `sub` (keep it under 18 letters),
`icon`, `done` (the sentence on the "done" card), and `steps`, a list of
one-key objects run in order:

```json
"steps": [{"story": "toPolice"}, {"talk": "report"}, {"sketch": "police"}]
```

Step kinds: `drill`, `story`, `talk`, `walk`, `ride`, `sketch`, `game`. Any
mix and any number.

A `drill` step names the notebook pages it covers, counted from 1: `"1-3"`,
`"4"`, or `"all"`. Every page must be in some drill step; the check fails if
one is left out. The usual shape, used by every unit in case 1, is five
stages: words for the first conversation, the first conversation, words for
the second, the second conversation, and Go:

```json
"stages": [
  {"id": "words1", "title": "Notebook", "sub": "Hotel words", "icon": "book", "steps": [{"drill": "1-2"}], "done": "..."},
  {"id": "talk1", "title": "Talk", "sub": "At the hotel desk", "icon": "talk", "steps": [{"story": "toDesk"}, {"talk": "desk"}], "done": "..."},
  {"id": "words2", "title": "Notebook", "sub": "Days and times", "icon": "book", "steps": [{"drill": "3-5"}], "done": "..."},
  {"id": "talk2", "title": "Talk", "sub": "A message", "icon": "note", "steps": [{"story": "toMessage"}, {"talk": "message"}], "done": "..."},
  {"id": "go", "title": "Go!", "sub": "Find room 12", "icon": "key", "steps": [{"story": "toRoom"}, {"walk": "hotel"}, {"story": "outro"}], "done": "..."}]
```

The case file has room for five cards. Keep to two conversations a unit.

## Words

`id`, `fr`, `en`, `icon` (see "Icons"), `note` (one plain sentence, may be
empty), and `say` if the voice should read something other than `fr`.

## Notebook pages

| Field | Meaning |
| --- | --- |
| `title` | Shown at the top |
| `words` | Word ids taught on this page. May be empty on a review page |
| `tasks` | What happens to each word: `learn` (a card), `picture` (hear it, tap the picture), `listen` (hear it, tap the French), `meaning` (French to English), `reverse` (English to French). Default: learn, listen, meaning, reverse |
| `strip` | `true` teaches five words on one card, for sets such as numbers and days |
| `builds` | Word-tile sentences: `en`, `tiles` (right order), `extra` (wrong tiles), `say` |
| `tip` | A tip id, shown as a card |
| `quiz` | One question: `en`, `options`, `answer` (index of the right one), `speak` (`true` if the options are French and should be read aloud) |
| `review` | `true` adds questions on every word from earlier pages of this unit |
| `bringBack` | Ids of words from earlier units to ask again |
| `refresh` | A number: also ask that many earlier words she has got wrong most |

## Story cards

`text`, and one of: `scene` (a name from `scenes`, a built-in picture, or
`icon:<name>` for one big icon), `scene: "letter"` (the text is drawn as a
letter; short last lines are the signature), or `people` (a list of face
descriptions with `tall`, drawn side by side for "look closely"). Optional:
`note` (small print) and `line` (one French line in a speech bubble).

Built-in pictures: `office`, `office_letter`, `map`, `notebook`, `airport`,
`hall`, `cabin`, `sky`, `sky_night`, `paris`, `paris_night`.

### Scenes

A picture built from tiles, 12 letters wide and 6 rows high:

| Field | Meaning |
| --- | --- |
| `tileset` | A name from `art/tilesets.json` |
| `rows` | Rows of map letters. `.` is floor, a space is empty |
| `sky`, `night` | `true` for a blue or dark background behind empty cells |
| `signs` | `x`, `y` (in tiles), `text`, `dir` |
| `sprites` | `look` (`hero` or a name from `looks.json`), `x`, `y`, `scale`, `flip`, `frame` (`-1` to walk on the spot) |
| `icons` | `name`, `x`, `y`, `size` (in screen units) |

## Conversations (`talks`)

| Field | Meaning |
| --- | --- |
| `who`, `name` | The speaker (for the voice) and a label |
| `face` | `hair`, `hairStyle` (`short`, `bun`, `long`, `bald`), `hat` (`cap`, `kepi`, `beret`, `hat`), `hatColor`, `clothes`, `scarf`, `stripes`, `moustache`, `glasses`. Colours are hex. `"kind": "mouse"` draws a mouse instead: `skin` is the fur, and `hat` can also be `crown` or `bonnet` (the red cap of the revolution); `hairStyle: "long"` gives a powdered wig |
| `phone` | `true` for a phone call: a telephone in place of the face, and each line's words show only after two wrong tries (lines on `auto` and `end` nodes show at once) |
| `battle` | `{"title": "Le serveur"}` plays this conversation as a battle, like an old Game Boy game. The title goes in the opponent's box ("Le serveur vous défie !"); it defaults to `name`. Each right answer on the main path knocks a piece off the opponent's bar and scores points (two on a first try, one after a miss); a wrong answer costs nothing. The bar empties at the `end` node, or when the conversation runs out of nodes, with a short win tune. At most one battle per unit; it needs at least two questions on its main path; the title is at most 24 letters |
| `board`, `boardStyle` | The sign above the character; `yellow` or dark |
| `wall`, `sky`, `counter` | Background colour; `true` for a window; `false` for no desk |
| `start` | The first node |
| `nodes` | The steps |

Each node may have `narr` (the storyteller's line), `npc` (a line), `mood`,
`tip` (shown after two misses), `show` (an icon the character holds up),
`next`, and one `ask`:

| `ask` | The player… | Needs |
| --- | --- | --- |
| `choice` | picks a French reply | `options`: `fr`, and `next` on the one right option. Wrong ones take `react` (a line), `narr`, and `show` (an icon, such as the wrong drink arriving). With `"any": true` on the node, every option with a `next` is right (her favourite film); each may lead to a different node |
| `build` | puts word tiles in order | `build`: `en`, `tiles`, `extra`, `say`. `wrong`: a line |
| `give` | hands something over | `items` (icon names), `answer`, `wrong` |
| `pay` | taps coins and notes to a total | `pay`: `amount`, `coins` (e.g. `[10, 5, 2, 1]`). `wrong` |
| `fact` | writes a fact in her notebook | `fact`: `prompt`, `options`, `answer`, `hint`, `key`, `label`, and `card` |
| `route` | picks a route on a map | `route`: `map` (`island`), `routes` (`id`, `pts` as points from 0 to 100), `answer`, `prompt` |
| `sort` | puts each thing in a group | `sort`: `bins` (two or three, each `{"fr": "gentil"}`), `items` (`name`, `face` or `icon`, `bin` counted from 0, and a `line` the speaker says about it). `wrong`: a line |
| `order` | puts steps in the order she hears them | `order`: `labels` (e.g. `["D'abord", "Puis", "Après"]`), `items` in the right order (`fr`, `icon`). After two misses the first step goes in by itself |
| `plan` | taps the room the speaker describes on a plan of a place | `plan`: `rooms` (`id`, `x`, `y`, `w`, `h` from 0 to 100, `label`, `icon`, and `fixed: true` for a room shown from the start), `answer` (a room id), `prompt`, `here` (`[x, y]`, the "Vous êtes ici" dot). Rooms she has marked stay marked for the rest of the conversation |
| `auto` | reads, then taps Next | |
| `end` | nothing; the conversation ends | |

A fact's `card` says what she reads the answer from: `ticket` (the boarding
pass), `note` (with `note`, a list of handwritten lines), or `hear` (nothing
to read: the character's line, with Again and Slow buttons). The game stores
the fact as `label` + answer and shows it on the Case tab.

## Walking scenes (`walks`)

| Field | Meaning |
| --- | --- |
| `tileset` | A name from `art/tilesets.json` |
| `map` | Rows of letters. `.` is floor, `*` is a star (a checkpoint), a space is nothing. Other letters come from the tile set |
| `start` | `[x, y]` |
| `tip` | The card shown before the scene starts |
| `quest` | The things to do, in order (see below). The scene ends when the last is done |
| `bump` | What she says when something bumps her |
| `pardon` | If present, a button with this line makes people in the way step aside |
| `ticket` | `true` adds a button that shows the boarding pass |
| `carry` | Things she has from the start, e.g. `["net"]` |
| `dark` | `{"near": 30, "far": 74}`: darkness with a pool of torchlight, in pixels |
| `zoom` | 1.5 by default |
| `signs` | `x`, `y`, `text`, `dir` (`left`, `right`, `up`, `down` or empty) |
| `totems` | Signposts: `x`, `y`, `lines` as `[dir, text]` pairs. Shown large when she stands near |
| `npcs` | People who stand still: `id`, `x`, `y`, `look`, and `line` and `narr`, or `talk` (a conversation, started by a `talk` quest step). `found` adds a bonus word |
| `spots` | Places on a wall she can walk up into: `x`, `y`, `w`, then `line`, `narr`, `found` |
| `movers` | Things that go to and fro: `look`, `path`, `speed`, `phase`, and a `line` said on a bump |
| `blockers` | People standing in a gap: `x`, `y`, `look`. They move for "Pardon !" |
| `runners` | `id`, `look`, `path`, `speed`, `mode`: `lead` keeps ahead of her and waits when she falls behind (`near`, in pixels); `loop` goes round and round. `from` is the quest step at which they start to move |
| `followers` | People who walk after her: `id`, `look`, `x`, `y`, `speed`, `from`, `line` |
| `watchers` | Guards who walk a `path` and shine a beam ahead of them: `id`, `look`, `speed`, `range` (in pixels), `pause` (seconds at each end, turning), `line`. If she is in the beam, the guard says the line and she goes back to the last star, or the start. Walls stop the beam |

Nothing should be faster than her walk, which is 58 pixels a second. A tile is
16 pixels.

### Quest steps

Each has `type`, `hint` (the banner at the top), and optionally `line` (said
when the step begins), `done` and `doneNarr` (said when it ends), `who` (the
speaker of those lines), and `found` (a bonus word).

| `type` | Done when | Fields |
| --- | --- | --- |
| `talk` | she finishes a conversation | `npc`: the id of a person with a `talk` |
| `reach` | she stands on a place | `at`, `w`, `h`; `open` turns a door tile into an open door; `img: "arrow"` marks the place |
| `get` | she stands on a place and picks something up | `at`, `item`, `img` (an icon lying there) |
| `enter` | she pushes up into a door | `at`: the door tile |
| `collect` | she has picked up every one | `items`: a list of `[x, y]`; `img`: a sprite |
| `lead` | a `lead` runner reaches the end of his path with her close behind | `runner` |
| `catch` | she touches a runner | `runner`, `needs` (an item, e.g. `net`) |
| `seek` | she has gone to each place in turn | `places`: `at`, `line`, `hint` (an icon), `found`. `wrong`: a line for a wrong place. `img`: what runs between them |
| `carry` | she has carried enough things | `items`: where they lie, as `[x, y]`; `img`: a sprite. Either `at`, `w`, `h` (the place to bring them) or `to` (ids of people who want one each; walk into them). `count` if fewer than all are needed. `wrong`: what someone already served says. `melt: true`: in the sun what she carries gets soft and slows her; on a tile marked `shade` in the tile set it firms up again |

## Small games (`games`)

Each has a `kind`, a `who` (the default speaker for its lines), a `tip`, a
`hint`, and optionally `found` (bonus word ids put in the notebook at the
end) and `doneNarr`. Every object with an `fr` inside a game is a spoken
line; give it its own `who` when someone else says it.

| `kind` | What she does | Fields |
| --- | --- | --- |
| `block` | taps streets to fill them with people and signs until a truck that drives round has only the dead end left | `nodes` (name to `[x, y]` on the 400 x 240 screen), `streets` (`id`, `name`, `a`, `b`; straight across or straight down; exactly one with `alley: true`), `groups` (how many streets she can fill), `start` (the truck's first street), `speed`, `marks` (`at`, `icon`, `name`), `chants`, `blocked` and `stuck` (lines). Until she fills a street next to the alley, those streets glow yellow; a barrier closes the alley's far end |
| `boules` | plays pétanque: touch where the ball should stop | `to` (points to win, 3), lines `pointUs`, `pointThem`, `win`, `again` (if the other side wins, the game starts over) |
| `spy` | drags a telescope over Paris and taps the right vehicle | `targets` (`icon`, `x`, `y` in a 1000 x 340 city, `say`, `right: true` on one, `mice: true` to draw mice beside it), `camX`, `camY` (where the view starts), `done` |
| `belt` | taps the shelf for each cheese on a belt | `shelves` (`fr`, `say`; give each `"who": "hero"`), `cheeses` (`name`, `icon`, `shelf` counted from 0, `tag` such as `AOP` or `IGP`), `speed`, `wrong`, `done`. The belt speeds up while she is right and slows after a miss; a cheese she misses comes round again |
| `metro` | taps her card on the gate, picks the platform by its direction sign, counts the stops and taps Descendre at her station, changes lines, goes out | `card` (the icon on the reader), `trips` (`line`, `color`, `ends` (the two end stations, first the one at the start of the list), `stations` (each `{"fr": …}`, in order along the line; the announcer reads them), `from`, `to`), lines `wrongWay`, `early`, `missed`, `change`, `exit`, `done`, and `exitSign`. A wrong platform costs one stop the wrong way and back; a missed station costs one stop back |
| `dress` | dresses models: each asks for something, and she taps it on the rack | `racks` (`name`, `items`: icons), `models` (`name`, `who`, `face`, `wants`: `item` and `line`, and `thanks`), `wrong`, `done` |

## Rides (`rides`)

`who`, `tip`, `wrong` and `arrive` (lines), `says` (the player's replies:
`left`, `right`, `straight`, `yes`, `no_left`, `no_right`, `no_straight`, each
with `fr`), and `legs`. A leg has `turn` (where the map says to go), `line`
(the driver's question), `ask` (`pick`: she chooses a way; `confirm`: the
driver offers `offer` and she says yes or corrects him), and optionally
`sight` (`icon`, `title`, `text`) for what passes the window.

`mode` picks the kind of ride:

| `mode` | What happens | Fields |
| --- | --- | --- |
| `taxi` (default) | She reads the way off a map and tells the driver | `says` |
| `drive` | She drives. The passenger's `line` at each corner is the way to go; she taps an arrow. A leg's `turn` may also be `faster` or `slower`. A van follows: a mistake brings it closer, nothing ends the chase | `chaser` (a look seen from above), `near` (a line when the van is close); no `says` needed |
| `follow` | A car drives ahead; a blinking dot on her tracker shows which way it turned, and she tells the driver | `lead` (a look seen from above), `says` |

Also: `car` (her car, a look seen from above; default `taxi_up`; the same
name ending in `_side` is used when it turns), `panel` (the label over the
right-hand panel), `scene` (the picture on arrival: `hotel`, `jump` over the
river, or `roadblock`).

## Police sketches (`sketches`)

`who`, `face`, `tip`, `wrong`, `right`, and `people`. Each person has `title`,
`intro` (a line), `base` (a face description to start from, with `tall`), and
`features`. A feature is one sentence with a gap:

```json
{"id": "hair", "fr": "Il a les cheveux {x}.", "answer": "noirs",
 "options": [{"fr": "noirs", "set": {"hair": "22223b"}}, {"fr": "roux", "set": {"hair": "b5484d"}}]}
```

`set` changes the drawing when that word is chosen. The detective's sound
files are made for every sentence that can be built.

## Icons

Used for words, stages, `give` items, `show`, and `seek` hints. A wrong name
draws a grey bubble, and the playthrough check reports it.

Plain names include: `book`, `talk`, `walk`, `wave`, `bye`, `please`,
`thanks`, `sorry`, `yes`, `no`, `lady`, `man`, `plane`, `gate`, `ticket`,
`passport`, `loupe`, `croissant`, `exit`, `wc`, `cafe`, `warning`, `letter`,
`taxi`, `taxi_green`, `taxi_red`, `street`, `hotel`, `here`, `there`,
`where`, `goto`, `howmuch`, `key`, `stairs`, `lift`, `bed`, `floor`,
`ground`, `booking`, `breakfast`, `water`, `juice`, `hotchoc`, `lemonade`,
`croque`, `crepe`, `fries`, `icecream`, `bill`, `yum`, `thief`, `menucard`,
`tall`, `short`, `moustache`, `glasses`, `hanger`, `hat`, `coat`, `scarf`,
`pull`, `dress`, `trousers`, `shirt`, `shoes`, `jacket`, `hat_big`,
`hat_small`, `hat_ok`, `mirror`, `mouse`, `cathedral`, `bridge`, `river`,
`island`, `route`, `cross_bridge`, `first_right`, `near`, `far`, `museum`,
`painting`, `bigclock`, `lilies`, `guardian`, `net`, `page`, `note`, `map`,
`sketch`, `eiffel`, `arc`, `pyramid`, `moon`, `sun`, `he`, `she`, `home`,
`suitcase`, `voila`, `today`, `tomorrow`.

Chapter 2 added (in `Icons2.kt`): `phone`, `phone_hold`, `moment`,
`no_thanks`, `good_day`, `truck`, `truck_red`, `car`, `car_red`, `bike`,
`moto`, `bus`, `bus_red`, `cheese`, `stolen`, `fast`, `downstairs`,
`because`, `tennis`, `foot`, `petanque`, `dance`, `music`, `heart`,
`heart_no`, `heart_plus`, `play`, `boule`, `cochonnet`, `throw`, `point`,
`trophy`, `lost`, `clap`, `plie`, `tendu`, `jete`, `releve`, `pointes`, `up`,
`down`, `faster`, `hurry`, `now`, `wait`, `stop`, `sewer`, `tunnel`, `torch`,
`through`, `along`, `rat`, `tail`, `ears`, `hole`, `shh`, `good`, `bad`,
`kind`, `mean`, `crown`, `queen`, `cheer`, `hungry`, `very_hungry`, `full`,
`eat`, `everyone`, `more_cheese`, `brie`, `camembert`, `comte`, `roquefort`,
`chevre`, `cow`, `milk`, `grass`, `rain`, `no_rain`, `first`, `then`,
`after`, `help`, `enough`, `not_enough`, `lots`, `too_much`, `need`,
`missing`, `lets_go`, `show`, `cinema`, `restaurant`, `watch`, `kitchen`,
`bar`, `stage`, `backstage`, `table`, `maitre`, `label_aop`, `label_igp`,
`label_none`, `tree`.

Chapter 3 added (in `Icons3.kt`) about 150 more. The art sheet shows them all
with their names. A few of the groups: the restaurant (`starter`, `main`,
`dessert`, `soup`, `chicken`, `veg`, `sparkling`, `juice_apple`, `cloche`),
films (`film`, `comedy`, `drama`, `cartoon`, `scary`, `hero_cape`), driving
(`roundabout`, `quay`, `behind_us`, `followed`, `car_green`, `van_red`),
jewellery (`ring`, `wristwatch`, `necklace_pearl`, `bracelet`, `earrings`,
`diamond`, `pearl`, `gold`, `silver`, `panther`), presents (`gift`,
`birthday`, `wedding`, `christmas`, `mothers_day`), the metro (`navigo`,
`mobile`, `validate`, `gate_metro`, `metro`, `metro_line`, `change`,
`get_off`), the bookshop (`bookshop`, `library`, `read`, `write`, `rule`,
`must`, `autobio`, `cat`), backstage (`runway`, `dressing_room`, `model`,
`makeup`, `hairdo`, `safe`, `keypad`, `locked`), the plan (`strong`, `weak`,
`weak_spot`, `nobody`, `only`, `tracker`, `mansion`), and dressing up (`chic`,
`elegant`, `evening_dress`, `call_taxi`, `classic`, `modern`, `colourful`,
`black_white`, `full_skirt`, `trouser_suit`, `golden_keys`).

Names that take a value after a colon:

| Name | Draws |
| --- | --- |
| `num:7` | a number |
| `coin:5` | a coin (1, 2) or a banknote (5, 10, 20, 50) |
| `swatch:d9534f` | a colour |
| `hair:5b3a1e` | a head with hair that colour |
| `day:lun.` | a calendar page |
| `clock:8`, `clock:20`, `clock:?` | a clock at that hour, with a sun or moon |
| `dir:left`, `dir:right`, `dir:straight` | a road-sign arrow |
| `prep:sous`, `prep:sous:table` | a ball under a box, or under a table. Relations: `sur`, `sous`, `dans`, `devant`, `derriere`, `acote`, `entre`. Objects: `box`, `table`, `chair`, `curtain`, `hat`, `altar`, `book`, `clock`, `bear`, `shelf`, `bed` |
| `thing:altar` | one of those objects alone |
| `feel:angry` | a face with that mood: `angry`, `sad`, `scared`, `shy`, `curious`, `smile` |
| `q:who` | a question word: `who`, `what`, `why`, `when`, `where`, `how` |
| `feet:1`, `feet:2` | ballet feet in first or second position |
| `dress:d9534f:long`, `dress:bw:short` | a dress on a hanger, that colour (or `bw`, black and white), long or short |
| `shoes:22223b` | a pair of shoes that colour |
| `bubble:scared` | a speech bubble with a face (`angry`, `scared`, `curious`, `smile`) or a picture (`gift`, `stars`, `oldbook`), for exclamations |

`./gradlew :desktop:sheet` draws every sprite and icon on one picture. New
icons are added in `Icons3.kt`.

## Tile sets, looks and sprites

`art/tilesets.json` has one entry for each kind of place (`airport`, `hotel`,
`street`, `shop`, `church`, `museum`, `tower`, `sewer`, `mousehall`,
`cabaret`, `jewel`, `metro`, `books`, `garden`): the floor tile, the colour behind the map, and for each map
letter the tile `img`, whether it is `solid` (default true), `under: true` if
floor should be drawn beneath it, and `shade: true` for a shady floor tile
(the street's `,`). A new place is a
new entry here plus its tiles in `sprites.txt`.

`art/looks.json` gives each look a `base` sprite and `swaps`: palette letters
to recolour, e.g. `{"base": "npcf", "swaps": {"r": "e8c55a", "c": "3a9e6a"}}`
is the woman sprite with blond hair and a green top. Sprites named
`<base>_0`, `_1`, `_2` are standing still and two steps of a walk.

In `sprites.txt`, a `pal` line gives a letter a colour and holds for every
picture after it, so a picture can bring its own colours on `pal` lines just
above its `img` line. Tiles are 16 x 16. People are 16 wide and about 20 tall.
