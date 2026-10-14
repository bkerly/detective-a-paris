# Détective à Paris

A French-learning game for a child who reads well and knows no French. The
player is a private detective called to Paris. Each unit teaches a set of
words, uses them in a conversation, then sends her through an action scene
that depends on what she learned. The plan is in the doc "French detective
game: unit plan".

"Détective à Paris" is a working title. Change it in
`app/src/main/res/values/strings.xml` and in `TitleScreen` (`Screens.kt`).

## What is built

Three cases, twenty-four units, 412 words and 1,635 spoken lines. Each unit is split
into short sittings, shown as cards in the case file: a notebook section, a
conversation, a second notebook section, a second conversation, then Go. Each
notebook section teaches only what the next conversation needs. Unit 2 has one
conversation; its second notebook section leads straight into the taxi ride.

When she finishes unit 8, the button on the last card says "Next unit" and
opens unit 9; after unit 16 it opens unit 17. The case file then has a
"Cases" button that lists every case.
Each unit opens only when the one before it is done. The house button at the
top of the case file goes back to the title screen.

**Case 1, "The Letter from Paris"**

| Unit | New words | Talk 1 | Talk 2 | Go |
| --- | --- | --- | --- | --- |
| 1. The flight over | Greetings, Madame and Monsieur, airport words, le sac, la valise, voilà (20) | The airline desk. She reads the gate off her ticket | The bag check: hand over the bag and ticket; "C'est votre valise ?" "Non, Monsieur." | Find gate 7 past suitcases and a floor cleaner; hand over passport and ticket |
| 2. Getting to the hotel | Numbers 1 to 20, street words, left, right, straight on (32) | Pick the free taxi, give the address, correct "Cinq ?" to "Non, quinze" | | Tell the driver the way at six corners; pay 18 euros in coins |
| 3. Hotel check-in | Hotel words, days, aujourd'hui and demain, 21 to 31, the time (25) | Room 12, premier étage, breakfast at 8 | The porter brings a note: mardi, 20 h, Le Procope | The lift is broken: stairs, the right landing, luggage carts, door 12 |
| 4. Dinner in a café | Bonsoir, je voudrais, drinks and food, l'addition (13) | Order a drink; the wrong words bring the wrong drink | Call the waiter back, order food, ask for the bill, pay 14 euros | "Au voleur !" Chase the thief past waiters and pigeons, with a Pardon ! button |
| 5. Explaining to the police | He is, she has, colours, hair, clothes (22) | Describe the man; the officer's drawing changes with each word | Describe the lady to the inspector | Borrow the net from the fish stall and net the thief in the market |
| 6. Clothes shopping | Clothes, too big, too small, 40 to 60, la souris (15) | A yellow hat, two wrong sizes first; pay 30 euros | A green scarf at the scarf shop; pay 40 euros | Catch the mouse: "Derrière le rideau !" with a picture hint |
| 7. Notre-Dame | Asking the way, on, under, behind, near, far (18) | Ask the way in the street; a man talks too fast | On the bridge: trace the way on a map | "Derrière l'autel, sous le livre": find the papers, pick up nine pages, keep away from the priest |
| 8. The Orsay at night | Five museum words; the rest is review | The night guard gives a floor, a turn and a place | Léa, the guard at the door, on the radio: describe the lady to her | A dark museum with a torch: find the water lilies and net the lady |

**Case 2, "The Stolen Cheese"**

| Unit | New words | Talk 1 | Talk 2 | Go |
| --- | --- | --- | --- | --- |
| 9. A call at the Ritz | Phone talk, vehicles, le fromage, volé, vite, en bas (16) | A phone call from room service: no face, and no words to read until she has tried twice | Monsieur Noir on the phone: a white cheese truck, stolen, in front of the hotel | A protest: the truck drives round one block; fill a street next to the dead end so the truck has only the dead end left |
| 10. The sorry driver | Feelings with être and avoir; qui, quoi, pourquoi, quand, comment (13) | Name each feeling from the driver's face: he is Paul, the taxi driver from unit 2 | Question words; the wrong word gets an answer that does not fit | Roll four wheels of brie to the hotel's cold room; in the sun they go soft and slow, in the shade they firm up |
| 11. Pétanque in the park | Games, j'aime, je n'aime pas, je préfère; pétanque words (19) | Who likes what in the park | Chat with a dancer who will not show what is in her bag | Pétanque against the dancers, first to 3 |
| 12. The dance class | Ballet words, faster and slower; hurry words, la tour (16) | The teacher calls moves; she taps the matching picture of the feet | The teacher: a red truck, today, from the Eiffel Tower | Climb the tower past pigeons, then find the red truck through the telescope |
| 13. The Paris sewers | Under the city, cent, mille, 1867; mouse words (14) | The tour guide's quiz, and "Il y a des souris ici ?" | A sewer worker gives the way | A dark sewer: keep out of the guards' beams, read the street signs, find the hole behind the Rivoli sign |
| 14. The mouse revolution | Good and bad, king and queen, vive; hungry and full (13) | Georges Raton tells his side; she sorts the mice under gentil or méchant | King Loir XVI tells his; she sorts them again | Carry cheese to the mice who say "J'ai faim !" |
| 15. The cheese office | Five cheeses, the farm, rain; d'abord, puis, après; enough and too much (20) | Put the steps in order: no rain, no grass, less milk, less cheese | Speak for the mice and offer a split | Sort cheeses off a belt by their label: AOP, IGP, or plain for the mice |
| 16. A night at the Moulin Rouge | Tonight, on va…; theatre words (14) | Plans with Georges: "On va au Moulin Rouge !" | No tickets: she says the way in back to him, in order | Sneak in through the kitchen and backstage, past the stage manager and the head waiter; get a lemonade and sit down |

**Case 3, "The Fashion Week Jewels"**

| Unit | New words | Talk 1 | Talk 2 | Go |
| --- | --- | --- | --- | --- |
| 17. Lunch at Simone | Booking a table, lunch and dinner, the courses, tastes, avec (21) | Book a table by phone: how many, what time, what name | The waiter's courses; tap the drink that goes with each; the courses in order; dessert is a note | Read the street signs at each corner and find the green car |
| 18. The green car | Films and kinds of film; driving words, the roundabout, le quai (17) | Films with Monsieur Noir; sort three films by kind; Céleste always plays the villain | "On est suivis !" Tap the arrow he calls | She drives; he calls the turns; a red van behind; a jump over the Seine |
| 19. The jewellery shop | Jewellery, gold, silver, pearls; presents and the days for them (21) | Ask what it is, try it on, ask the price ("très, très cher"); what each piece is made of | A present for her mother; sort pieces under wedding, birthday, Christmas | Hear each shopper's reason; tell the jeweller which one is strange; follow him to the metro |
| 20. The metro | Paying with a card or a phone; lines, directions, changes; je suis perdue (18) | No more paper tickets: a Navigo Easy card or the app | The man drops a book with a bookshop's stamp; a woman gives the way | Tap in, pick the platform by its direction, count seven stops, change to line 10, count three |
| 21. Shakespeare and Company | Bookshop and library, reading; sleeping, il faut, writing your life (17) | The bookseller knows the book; bookshop or library? | Three rules for sleeping in the shop, built from word tiles | Search under, behind and on things; find Hugo's one-page life story |
| 22. Backstage at the show | The show, the runway, dressing room, make-up; safe, code, locked, before (15) | Mark each room on a plan of the backstage as the stage manager describes it | Where the jewels are kept; who wears them | A designer storms off: dress four models from the racks |
| 23. Talking it over | Strong and weak, the weak spot, nobody, only; exclamations (14) | Strong or weak: every part of the safety plan is strong | Pick the right exclamation at each turn: "Oh là là ! C'est elle !" | A night garden with guards and torches: put a tracker under Céleste's car |
| 24. The night of the show | Smart and plain, the evening dress, calling a taxi; styles (14) | The concierge: dressed right? Call a taxi | A designer's styles; the tracker beeps | Paul's taxi follows the tracker to a police roadblock |

## Secret codes

If the save is lost, a code opens a unit with every unit before it marked
done. On the title screen, tap the title five times. A keyboard opens. Type
the code and tap "Use code". If no name is saved, the game asks for one first.

| Unit | Code |
| --- | --- |
| 1. The flight over | BONJOUR |
| 2. Getting to the hotel | PASSEPORT |
| 3. Hotel check-in | VENDOME |
| 4. Dinner in a café | PROCOPE |
| 5. Explaining to the police | VOLEUR |
| 6. Clothes shopping | MOUSTACHE |
| 7. Notre-Dame | SOURIS |
| 8. The Orsay at night | CATHEDRALE |
| 9. A call at the Ritz | NYMPHEAS |
| 10. The sorry driver | CAMION |
| 11. Pétanque in the park | FROMAGE |
| 12. The dance class | PETANQUE |
| 13. The Paris sewers | EIFFEL |
| 14. The mouse revolution | EGOUTS |
| 15. The cheese office | REVOLUTION |
| 16. A night at the Moulin Rouge | CAMEMBERT |
| 17. Lunch at Simone | MOULINROUGE |
| 18. The green car | MICHELIN |
| 19. The jewellery shop | CINEMA |
| 20. The metro | PANTHERE |
| 21. Shakespeare and Company | NAVIGO |
| 22. Backstage at the show | TUMBLEWEED |
| 23. Talking it over | PODIUM |
| 24. The night of the show | SACREBLEU |

A code marks the earlier units' words as met and their tips and facts as
found, so later units can bring those words back. It does not change the
stars or the notebook's record of wrong answers. The codes are also in
`docs/STORY.md`, at the top of each unit. A new unit gets its own code in its
JSON file (`"code"`).

A save from the last build loads as it is: chapter 1 did not change shape.

English fades as the plan says. Units 1 and 2 show English under every French
line. From unit 3 the help shows only when she taps the line. From unit 5 a
line made of words she knows has no help at all, and a new word gets a short
word list in place of a full translation. In units 7 and 8 the storyteller
uses French where she knows the words. Chapters 2 and 3 work the same way as
units 7 and 8. Buttons and menus stay in English throughout.

## Build it for a Fire tablet

1. In Android Studio choose File, New, Project from Version Control, and give
   it `https://github.com/bkerly/detective-a-paris`. Let it sync. For each
   later build, use Git, Pull (or `git pull` in that folder) rather than a new
   download.
2. Plug in the tablet with USB debugging on, then press Run. Or build the
   file `app/build/outputs/apk/debug/app-debug.apk` with
   `./gradlew :app:assembleDebug` and install it with `adb install`.
3. The first build also downloads the small French speech model (Vosk,
   about 41 MB, Apache 2.0) into `app/src/main/assets/model-fr`. It is not
   kept in git. If the download fails the build still works, and the spoken
   battles fall back to "say it aloud, then tap J'ai dit !". The first time
   she taps Parler, the tablet asks to use the microphone; nothing she says
   leaves the tablet.

**Not yet compiled as an Android app.** The place this was written could not
download the Android build tools. Here is what was and was not checked:

| Part | Checked how |
| --- | --- |
| `core` (the whole game, about 10,000 lines) | Compiled with Kotlin 2.0.21. All twenty-four units played start to finish by script |
| `desktop` | Compiled and run |
| `app` (about 520 lines, with the Vosk microphone code) | Type-checked against hand-written stand-ins for the Android and Vosk classes. Never built into an APK or run on a device |
| Gradle files | Written by hand; you upgraded them in Android Studio. Never run here |

So the first sync or build may need a small fix.

**Faster rebuilds.** `gradle.properties` turns on Gradle's build cache,
parallel builds and the configuration cache, and gives Gradle 3 GB of memory.
The biggest saving comes from keeping one project folder: if each new build
lands in the same folder (a git pull, as in step 1), Android Studio only
syncs when a Gradle file changes and only rebuilds what changed. Unpacking a
whole new zip into a new folder makes it start from nothing each time. If a
Gradle build ever fails with a configuration cache error, delete the line
`org.gradle.configuration-cache=true` and build again. Android Studio's
upgrade assistant has raised the versions to Android Gradle Plugin 9.2.1,
Kotlin 2.2.10 for the app and Gradle 9.4.1. The `core` and `desktop` modules still name
Kotlin 2.0.21. The app targets minSdk 22 (Android 5.1, which is Fire OS 5).

Things to look at on the first run on a real tablet:

- The spoken battles. On first launch the app copies the speech model out
  (a few seconds; until then Parler shows as "J'ai dit !"). Each try is
  written to `Android/data/io.github.bkerly.detective/files/speech-log.tsv`,
  with what was heard and how sure the recogniser was. After a few battles,
  send that file to Claude to set the cut-offs in `core/.../Speech.kt` from
  her own voice. A child's voice may score lower than the cut-offs expect.

- Text fits its boxes. Layout was checked on the desktop with the same font
  files, and Android measures text a little differently.
- Sound starts without a cut-off first syllable.
- The floor tiles show no thin gaps between them when she walks.
- The frame rate in the walking scenes on an old tablet. The dark museum in
  unit 8 and the dark sewers in unit 13 draw one shaded square for each tile
  and are the heaviest.
- The sound files come to about 33 MB, so the app is about 35 MB.

Nobody has played this with a child yet. The scripted player proves every
scene can be finished; it cannot say whether a chase is too hard or a page of
the notebook too long. Each walking scene's speeds are numbers in its unit
file (`speed` on movers, runners and followers), so they are easy to turn down.

## Play it on a computer

```
./gradlew :desktop:run
```

The mouse is the finger. Arrow keys also walk. Escape is the back button.
This has no sound unless you pass a folder of `.wav` files as a second
argument (`tools/make_audio.R --wav DIR` makes them).

To jump ahead, tap the heading of the case file five times. That opens every
unit, for a parent or a tester.

## Read the story without playing it

```
Rscript tools/make_story.R
```

This writes `docs/STORY.md`: every unit in order, with the story cards, each
conversation on its right path, the wrong answers and what they lead to, the
walking scenes, the taxi ride and the sketches. `--short` leaves out the wrong
answers, side remarks and word lists (`docs/STORY_SHORT.md` is that version).
`--name` changes the detective's name. Both pages are made from the unit
files, so run the script again after changing a unit and do not edit them by
hand.

In the game itself there is no way to read ahead. The Story button on the
case file replays a unit's opening only, and five taps on the heading open
every unit but each must still be played.

## Check it after a change

```
./gradlew :desktop:playthrough
```

This first reads every unit file and reports anything inconsistent: an
unknown icon, a conversation step that leads nowhere, a tip no page shows, a
map letter the tile set does not have. Then it plays every unit from the title
to the end with no person at the controls. It gets some answers wrong on
purpose, bumps into things, takes a wrong turn in the taxi, puts a wrong word
in the police sketch, says "Pardon !" to people in the way, walks into a
guard's beam once, taps a wrong vehicle in the telescope and a wrong shelf on
the cheese belt, and in unit 1 stops at a checkpoint and comes back. It fails if a button is missing, a scene
never ends, the save does not load back, or any French line has no sound
file. It saves a picture of each screen in `desktop/build/playthrough/`, drawn
at 1024 x 600, the size of a Fire 7.

To play only some units: `./gradlew :desktop:playthrough --args="unit3 unit4"`.

## Writing a new unit or a new case

A unit is one JSON file. A case is a list of units in `units/index.json`. No
Kotlin is needed for either, as long as the unit uses the kinds of scene the
game already has:

- **Notebook pages**: word cards, pictures, listening, meaning, word tiles,
  tips, quiz questions, old words coming back.
- **Conversations**: pick a reply, build one from tiles, hand something over,
  pay with coins, write down a fact (read from a note or only heard), pick a
  route on a map.
- **Conversations** can also be a phone call (no face, words only after two
  tries), sort things into groups (gentil or méchant), or put steps in order.
- **A spoken battle in each unit**: a reluctant speaker "challenges" her
  ("L'agente vous défie !"). She wins them over by saying French phrases
  aloud; the tablet listens offline, and how well she says a phrase, and
  whether it suits the speaker's mood, sets how much doubt it takes away.
  The place is drawn in greys so the two people stand out. She cannot lose.
- **Walking scenes**: a map with signs, people, things that roll about, people
  to say "Pardon !" to, someone to chase, someone who follows, things to pick
  up, places to find from a spoken clue, darkness with a torch, guards with
  beams who send her back to the last star, things to carry to a place or to
  someone who wants them, sun and shade that soften and firm up what she
  carries.
- **A taxi ride** and **a police sketch**.
- **Small games**: the protest map (fill streets until a truck has one way
  out), pétanque, a telescope over Paris, and a sorting belt.

`docs/UNIT_FORMAT.md` lists every field. The steps:

1. Write `app/src/main/assets/units/<id>.json` and add it to `index.json`.
2. `./gradlew :desktop:playthrough` until it passes.
3. `./gradlew :desktop:lines`, then `Rscript tools/make_audio.R --tts ... --voices ...`
   to make sound for the new lines. Run the playthrough once more.

New places, people and pictures need art: tiles and sprites are text in
`art/sprites.txt` (one letter is one pixel), and icons are drawn in
`MoreIcons.kt` or `Icons2.kt`. `./gradlew :desktop:sheet` draws all of it on
one picture. There are tile sets for an airport, a hotel, a street, a shop, a
church, a museum, the Eiffel Tower, the sewers, the mouse hall and a cabaret.
Faces can be people or mice (`"kind": "mouse"`), with eleven moods.

## Where things are

```
core/      the game, in plain Kotlin. No Android code and no libraries.
  Game.kt          the game object, the save, the base class for screens
  Data.kt          reads the index and the unit files into objects
  Screens.kt       title, name, story cards, case file, list of cases
  DrillScreen.kt   the notebook
  TalkScreen.kt    conversations
  WalkScreen.kt    walking scenes
  RideScreen.kt    rides: the taxi, the car chase, following a tracker
  SketchScreen.kt  the police sketch
  Games.kt         the small games: protest map, pétanque, telescope, cheese belt
  Games3.kt        the metro ride and dressing the models
  NotebookScreen.kt  the word list that opens over any scene
  Ui.kt, Icons.kt, MoreIcons.kt, Icons2.kt, Icons3.kt, Art.kt  buttons, text, icons, pixel art, faces
  Json.kt          a small JSON reader and writer
app/       the Android app: one activity, one view, a painter, sound, save
  src/main/assets/units/      index.json and one file for each unit
  src/main/assets/art/        sprites.txt, tilesets.json, looks.json
  src/main/assets/audio/      voice clips and manifest.json
  src/main/assets/fonts/      three fonts and their licences
desktop/   a window to play in, the scripted check, the list of lines, the art sheet
tools/make_audio.R   makes the voice clips and sound effects
tools/make_story.R   writes the whole story as one page to read
docs/STORY.md        that page (docs/STORY_SHORT.md is the right path only)
tools/lines.tsv      every French line and its voice, written by the game
docs/UNIT_FORMAT.md  every field of a unit file
```

The game draws on a 400 x 240 screen and the platform scales it. On a Fire 7
that is 2.5 screen pixels to the unit.

## What the engine cannot do yet

- **French menus.** "Next", "Pay", "Got it" and the case file are English in
  every unit.
- **The name aloud.** The name is typed freely, so the voice says "Je
  m'appelle" and stops. The name shows on screen.
- **Other maps for route questions.** There is one, the island of Notre-Dame.
- **Saying a score or a year aloud from numbers.** Each spoken number is a
  clip made ahead of time, so pétanque reads the score on screen in French
  words and the voice only says "Un point pour nous !".
- **Other rides.** A ride is a taxi she gives the way to, a car she drives
  while someone calls the turns, or a taxi that follows a tracker. It ends at
  a hotel door, a jump over the river, or a roadblock.
- **Doors in side or bottom walls.** A door she walks into is always in the
  wall at the top of a room. Unit 7's church exit is an open passage instead.
- **A plan for when old words come back.** A page names the old words to ask
  again (`bringBack`) and can add the ones she has got wrong most
  (`refresh`). Nothing spaces them over days.

## Choices that differ from the unit plan, or add to it

- Lines that are a test ("Votre passeport, s'il vous plaît", the taxi fare,
  the breakfast time) hide their help until two wrong tries.
- The player's own reply choices are in French only. She can hear each one
  before she picks.
- Facts she must only hear (room 12, premier étage, the hotel's number) are
  written in the notebook from a card with Again and Slow buttons and nothing
  to read the answer from.
- Unit 2: the taxi also passes the Louvre and the Arc de Triomphe.
- Unit 3: numbers 21, 22, 23, 30 and 31 are taught with the rule for the
  rest. There is a second floor to climb to by mistake.
- Unit 4: she also asks for the bill and pays it. The helper speaks one line.
- Unit 1: a second conversation at the bag check teaches le sac, la valise
  and voilà.
- Unit 3: the porter's note is now a conversation of its own, with
  aujourd'hui and demain.
- Unit 5: a short conversation comes before each sketch, so that Bonjour
  still comes first. The beret is "un chapeau noir".
- Unit 6: the mouse hides in five places, two of them with words from outside
  the plan (la boîte, la chaise). The voice reads "pull" as "pul"; see Voices.
- Unit 7: l'autel and le livre are taught in the notebook, because the note
  needs them. The route question shows three coloured routes on a map.
- Unit 8: she describes the lady to Léa, a second guard, over a radio, with
  the same sketch screen as unit 5. The
  paintings carry the painters' names. L'ours blanc is a bonus word.
- Walking: she steers by holding a finger where she wants to go. The view is
  zoomed in so a child's finger can steer. Checkpoints are rows of stars.
- Invented to fit the plan: Air Lumière, Claire the airline agent, the bag
  check, the porter, the hat shop, the scarf shop, the fish stall, the man on
  the bridge, Léa.
- Unit 9: room service asks if she wants a croissant; the protest map uses
  real street names near the Ritz, though not in their real places. The map
  is one block with the dead end in the middle. She has one group of
  protesters, and the two streets next to the dead end glow yellow until she
  fills one.
- Unit 10: the driver is Paul, the taxi driver from unit 2. He says he took
  the truck at eight o'clock today.
- Unit 11: pétanque is played to 3, not 13. If the dancers win a game, it
  starts again: nobody loses.
- Unit 12: the climb is a walk up the tower's stairs; pigeons knock her back a
  few steps. The telescope shows a red car, a red bus, a red bike and a white
  truck before the red truck.
- Unit 13: the hole is behind a sign for the rue de Rivoli. A sign for the
  Place Vendôme marks the spot right under the Ritz.
- Unit 14: in the second sort, the queen goes under gentil too: the king says
  she is hungry and sad, not mean. Mice who are full say so.
- Unit 15: d'abord, puis and après are taught here (the plan expected them
  from unit 5, which does not teach them). The belt has ten cheeses: four AOP,
  three IGP and three plain.
- Unit 16: no line in chapter 2 says a price, so no number above 60 comes up.
- Chapter 3: the film star, Céleste Rivière, is made up, as the plan asks.
  Monsieur Noir calls her "la nouvelle Marion Cotillard", because like
  Cotillard she plays the villain in every film; a culture note says who
  Cotillard is. The three films are named in words only, never drawn.
- Unit 17: the note under the lid names the street (the rue des Lilas), so
  reading the corner signs matters. A green car parked at the wrong corner is
  a decoy. The courses come back as a "put them in order" question.
- Unit 18: a short practice conversation comes before the chase: he calls a
  way, she taps the arrow. The chase has seven corners, two of them "faster"
  or "slower". The van never ends the chase; a mistake only brings it closer.
- Unit 19: the shop sign reads "13, rue de la Paix" rather than the jeweller's
  name. A third conversation, with the jeweller, sorts what the shoppers said
  into normal and strange before the man runs.
- Unit 20: the stamp in the book comes first, then the way, so she knows why
  she is going to Cluny–La Sorbonne. The book's stamp gives the bookshop's
  real address, 37 rue de la Bûcherie. Either way of paying works; the gate
  takes both. A wrong platform costs one stop the wrong way; a missed station
  costs one stop back.
- Unit 21: the man is named Hugo in his life story. The shop's cat sleeps
  under the table.
- Unit 22: the plan has seven rooms; four are described in the first
  conversation and the safe in the second. The models are Inès, Zoé, Nina
  and Maya.
- Unit 23: the inspector from unit 5 is a man, so he, not she, waits at the
  roadblock in unit 24. Monsieur Noir is the one who says "Sacrebleu !".
- Unit 24: the hotel scene keeps the hotel tiles; the concierge's gold keys
  are an icon. The chase has six corners, with the Arc de Triomphe, a bridge
  and the Eiffel Tower out of the window.

## Credits and licences

The code is under the MIT licence (`LICENSE`). The story, unit files, art,
sound and docs are under CC BY 4.0 (`LICENSE-CONTENT`); the unit files are in
`app/src/main/assets/units/`. The voices and fonts below were made by others
and keep their own licences.

### Voices

The clips are computer speech made ahead of time with
[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) and two Piper voices.
Characters share the two voices at different pitches.

| Voice | Who | Licence of the voice |
| --- | --- | --- |
| fr_FR-siwis-medium | the notebook, the detective, all women | CC BY 4.0 (SIWIS data) |
| fr_FR-tom-medium | all men | AGPL v3 |

That is fine for a family game. Read the licences before giving the app to
anyone else; the AGPL voice is the one to replace first.

Nobody has listened to these clips. They were checked by having a speech
recogniser (Whisper small) write down each clip and comparing that with the
text. The voice varies a little from take to take, so a line that did not
come back right was made again, up to four more times, and the best take
kept. 1,448 of the 1,635 lines came back word for word, counting sound-alikes
such as l'autel and l'hôtel as the same. Most of the other 187 are years and
numbers the recogniser writes as digits (1867), noises it cannot spell (Euh,
Hmm, Oh là là), or near sound-alikes (l'horloge heard as "leur loge", brie
as "bruit"). The ones worth a human ear:

- **un** on its own (unit 2) and **cent**, **Qui ?**, **Où ?** (chapter 2).
  Very short words are the hardest for the recogniser.
- The taxi driver's **Oui, oui, cinq !** (unit 2).
- The shop owner's **Un pull vert. Voilà !** (unit 6).
- **gêné / gênée** and **en haut** (units 10 and 12).
- **Chut… Un trou !** (unit 13).
- Chapter 3: **Parfait. Allez-y !** and **Hmm… vous êtes sûre ?** (unit 18),
  **Non, votre badge !** (unit 22), **Il lit. Chut !** (unit 21), and the
  metro station names in unit 20 (Odéon, Maubert – Mutualité, Jussieu).

To replace a clip, delete its two `.ogg` files (normal and `_slow`) from
`app/src/main/assets/audio/` and run `tools/make_audio.R`; it makes a fresh
take of whatever is missing.

The voice reads "pull" as "poule". The unit files give it the spelling "pul"
in `say` wherever the word comes up; the screen still shows "pull". The same
trick fixes two lines in chapter 2: the voice drops the t of "Chut", so it
reads "Chute" (in chapter 3 too), and "Il a peur." came out as "il apporte",
so it reads "Il a peur !". Use `say` the same way for any word a later unit finds the
voice gets wrong.

### Fonts

Atkinson Hyperlegible (text), Lilita One (titles) and Patrick Hand
(handwriting), all under the SIL Open Font Licence. The licence files are
beside the fonts.
