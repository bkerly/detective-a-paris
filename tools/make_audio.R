#!/usr/bin/env Rscript
# Makes the French voice clips and the sound effects for the game.
#
# It reads tools/lines.tsv, the list of every French line in every unit with
# the voice that says it, and writes one .ogg per line (plus a slow version)
# into app/src/main/assets/audio. It also writes audio/manifest.json, which
# the game uses to find the clip for a line. Lines that already have a clip
# are skipped, so after adding a unit only the new lines are made.
#
# The game itself writes tools/lines.tsv, so the list can never disagree with
# what the game will ask for. Refresh it after any change to French text or
# to a unit's "voices":
#   ./gradlew :desktop:lines
#
# The voices are Gradium's Phonon French voices, run by Ollama on an Apple
# Silicon Mac. You need:
#   - R with the jsonlite package
#   - ffmpeg and ffprobe on the PATH (brew install ffmpeg)
#   - curl (already on a Mac)
#   - Ollama 0.40.11 or later, running, with the French model pulled:
#       ollama pull phonon:40m-fr
#
# Run from the project folder:
#   Rscript tools/make_audio.R
# Options:
#   --lines FILE   the list of lines (default tools/lines.tsv)
#   --assets DIR   assets folder (default app/src/main/assets)
#   --wav DIR      also write .wav copies there, for the desktop player
#   --force        remake every clip, not only the missing ones
#   --limit N      make clips for the first N lines only (to listen before a full run)
#   --list         only print the lines, make nothing
#   --ollama URL   where Ollama listens (default http://localhost:11434)
#   --model NAME   the speech model (default phonon:40m-fr)
#   --tempo X      speed of the normal clips; 1 is as the model says it,
#                  0.95 a touch slower (default 1)
#
# The old Piper voices (siwis, tom) still work if a unit names them; they also
# need --tts /path/to/sherpa-onnx-offline-tts and --voices /path/to/voices.

suppressPackageStartupMessages(library(jsonlite))

# French text needs a UTF-8 session. Most computers have one already.
if (!isTRUE(l10n_info()[["UTF-8"]])) {
  for (loc in c("C.UTF-8", "en_US.UTF-8", "UTF-8")) {
    ok <- suppressWarnings(Sys.setlocale("LC_CTYPE", loc))
    if (nzchar(ok) && isTRUE(l10n_info()[["UTF-8"]])) break
  }
  if (!isTRUE(l10n_info()[["UTF-8"]])) stop("Could not switch R to a UTF-8 locale; French accents would be damaged.")
}

args <- commandArgs(trailingOnly = TRUE)
opt <- function(name, default = NULL) {
  i <- match(paste0("--", name), args)
  if (is.na(i)) default else args[i + 1]
}
flag <- function(name) paste0("--", name) %in% args
`%||%` <- function(a, b) if (is.null(a) || length(a) == 0 || identical(a, "")) b else a

assets  <- opt("assets", "app/src/main/assets")
lines_file <- opt("lines", "tools/lines.tsv")
tts     <- opt("tts", Sys.getenv("SHERPA_TTS", "sherpa-onnx-offline-tts"))
voices  <- opt("voices", Sys.getenv("VOICES_DIR", "voices"))
wav_dir <- opt("wav")
force   <- flag("force")
limit   <- as.integer(opt("limit", NA))
ollama  <- sub("/+$", "", opt("ollama", Sys.getenv("OLLAMA_HOST_URL", "http://localhost:11434")))
model   <- opt("model", "phonon:40m-fr")
tempo   <- as.numeric(opt("tempo", "1"))

# How each voice name in the unit files maps to a voice.
# Phonon voices: the name Ollama knows and Gradium's voice ID. The script tries
# the name first and falls back to the ID. Hear them all at
# https://huggingface.co/gradium/phonon-voices (the French table).
# To recast someone, change the voice next to their name in the unit's "voices".
phonon <- function(name, id) list(engine = "phonon", name = name, id = id, pitch = 1)
voice_table <- list(
  # women
  noemie   = phonon("Noémie",  "FXxJ9mANRq6BCTX5"),
  coralie  = phonon("Coralie",       "ZeSg853xFACESHHI"),
  tiphaine = phonon("Tiphaine",      "0fvH6XAKvA748v2v"),
  maelys   = phonon("Maëlys",   "s048cR1l2Jmu4k3B"),
  orianne  = phonon("Orianne",       "TUk4X11S6UytB3G6"),
  solene   = phonon("Solène",   "YhIHaAfQ0cQPDV9R"),
  ninon    = phonon("Ninon",         "qtPlUYAZ4EJDuWMm"),
  lilou    = phonon("Lilou",         "4iKxBtHBRzr1YOph"),
  helene   = phonon("Hélène", "Vvakv7iWZCUrBooR"),
  # men
  augustin = phonon("Augustin",      "Tek4tJXiX6_yvXq7"),
  arnaud   = phonon("Arnaud",        "aXMVwiUhITMrwPzd"),
  jules    = phonon("Jules",         "YKeBw3OV1RgpdhLh"),
  baptiste = phonon("Baptiste",      "rqqeErpASHVQXHDd"),
  lucien   = phonon("Lucien",        "LIv4NaG8ay4Mhqtd"),
  marius   = phonon("Marius",        "biuhvu17TxVKOcyy")
)
# The old Piper voices, kept so an older unit file still works.
#   speed  bigger is slower; pitch 1 is the voice as recorded
siwis <- list(engine = "piper", dir = "vits-piper-fr_FR-siwis-medium", model = "fr_FR-siwis-medium.onnx", sid = 0, speed = 1.10)
tom   <- list(engine = "piper", dir = "vits-piper-fr_FR-tom-medium",   model = "fr_FR-tom-medium.onnx",   sid = 0, speed = 1.10)
voice_table <- c(voice_table, list(
  siwis      = c(siwis, pitch = 1.00),
  siwis_low  = c(siwis, pitch = 0.93),
  siwis_high = c(siwis, pitch = 1.06),
  tom        = c(tom,   pitch = 1.00),
  tom_low    = c(tom,   pitch = 0.94),
  tom_high   = c(tom,   pitch = 1.06)
))
# The slow clip is the normal clip stretched in time, so it is always the same
# words said the same way. 0.78 means 78% of normal speed.
slow_tempo <- 0.78

# ---- 1. read the lines ------------------------------------------------------

if (!file.exists(lines_file)) stop("No list of lines at ", lines_file, ". Make it with: ./gradlew :desktop:lines")
raw <- readLines(lines_file, encoding = "UTF-8", warn = FALSE)
raw <- raw[nzchar(raw)]
tab <- regexpr("\t", raw, fixed = FALSE)
if (any(tab < 1)) stop("Every row of ", lines_file, " must be: voice, a tab, then the text.")
lines <- unique(data.frame(voice = substr(raw, 1, tab - 1), text = substr(raw, tab + 1, nchar(raw)), stringsAsFactors = FALSE))
bad <- grepl("{", lines$text, fixed = TRUE)
if (any(bad)) stop("This line has a {placeholder} and no \"say\" text: ", lines$text[bad][1])
cat(nrow(lines), "different lines in", lines_file, "\n")

if (!is.na(limit)) {
  lines <- head(lines, limit)
  cat("making only the first", nrow(lines), "lines\n")
}

if (flag("list")) {
  for (i in seq_len(nrow(lines))) cat(lines$voice[i], "|", lines$text[i], "\n", sep = "")
  quit(status = 0)
}

# ---- 2. file names ----------------------------------------------------------

slug <- function(x) {
  # accented letters, written as \u codes so this file stays plain ASCII
  x <- chartr("\u00e0\u00e2\u00e4\u00e7\u00e9\u00e8\u00ea\u00eb\u00ee\u00ef\u00f4\u00f6\u00f9\u00fb\u00fc\u00ff\u00c0\u00c2\u00c4\u00c7\u00c9\u00c8\u00ca\u00cb\u00ce\u00cf\u00d4\u00d6\u00d9\u00db\u00dc", "aaaceeeeiioouuuyAAACEEEEIIOOUUU", x)
  x <- gsub("\u0153", "oe", x)
  x <- gsub("[^a-z0-9]+", "-", tolower(x))
  x <- gsub("^-+|-+$", "", x)
  substr(x, 1, 44)
}

audio_dir <- file.path(assets, "audio")
dir.create(audio_dir, recursive = TRUE, showWarnings = FALSE)
manifest_path <- file.path(audio_dir, "manifest.json")
old <- if (file.exists(manifest_path)) fromJSON(manifest_path, simplifyVector = FALSE)$clips else list()

clips <- list()
used <- character()
for (i in seq_len(nrow(lines))) {
  key <- paste0(lines$voice[i], "|", lines$text[i])
  name <- old[[key]]$f
  if (is.null(name)) {
    base <- paste0(lines$voice[i], "_", slug(lines$text[i]))
    name <- base
    k <- 2
    taken <- c(used, vapply(old, function(c) c$f, ""))
    while (name %in% taken) { name <- paste0(base, "-", k); k <- k + 1 }
  }
  used <- c(used, name)
  clips[[key]] <- list(f = name)
}

# ---- 3. make the clips ------------------------------------------------------

run <- function(cmd, a) {
  out <- suppressWarnings(system2(cmd, a, stdout = TRUE, stderr = TRUE))
  status <- attr(out, "status") %||% 0
  if (status != 0) stop(cmd, " failed:\n", paste(tail(out, 8), collapse = "\n"))
  out
}

seconds <- function(path) {
  out <- run("ffprobe", c("-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", shQuote(path)))
  as.numeric(out[1])
}

# Cut the silence off both ends, shift the pitch if the voice asks for it,
# bring the loudest point to the same level in every clip, and add a short
# lead-in so a slow player does not clip the start.
finish <- function(raw, ogg, wav, pitch, tempo) {
  trim <- paste0(
    "aresample=22050,",
    "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.03,areverse,",
    "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.08,areverse"
  )
  probe <- run("ffmpeg", c("-hide_banner", "-i", shQuote(raw), "-af", shQuote(paste0(trim, ",volumedetect")), "-f", "null", "-"))
  peak <- as.numeric(sub(".*max_volume: (-?[0-9.]+) dB.*", "\\1", grep("max_volume", probe, value = TRUE)[1]))
  gain <- if (is.na(peak)) 0 else -2 - peak
  chain <- trim
  if (abs(pitch - 1) > 0.001) {
    chain <- paste0(chain, ",asetrate=", round(22050 * pitch), ",aresample=22050,atempo=", sprintf("%.4f", 1 / pitch))
  }
  if (abs(tempo - 1) > 0.001) chain <- paste0(chain, ",atempo=", tempo)
  chain <- paste0(chain, ",volume=", sprintf("%.1f", gain), "dB,adelay=60,apad=pad_dur=0.15")
  run("ffmpeg", c("-hide_banner", "-loglevel", "error", "-y", "-i", shQuote(raw), "-af", shQuote(chain),
                  "-ar", "22050", "-ac", "1", "-c:a", "libvorbis", "-q:a", "4", shQuote(ogg)))
  if (!is.null(wav)) {
    dir.create(dirname(wav), recursive = TRUE, showWarnings = FALSE)
    run("ffmpeg", c("-hide_banner", "-loglevel", "error", "-y", "-i", shQuote(raw), "-af", shQuote(chain),
                    "-ar", "22050", "-ac", "1", "-c:a", "pcm_s16le", shQuote(wav)))
  }
}

speak <- function(voice, text, out_wav) {
  v <- voice_table[[voice]]
  if (is.null(v)) stop("Unknown voice \"", voice, "\". Add it to voice_table in tools/make_audio.R.")
  # a line with no full stop can end too suddenly, so give it one
  if (!grepl("[.?!]$", text)) text <- paste0(text, ".")
  if (identical(v$engine, "phonon")) speak_phonon(voice, v, text, out_wav) else speak_piper(v, text, out_wav)
}

speak_piper <- function(v, text, out_wav) {
  d <- file.path(voices, v$dir)
  if (!dir.exists(d)) stop("Voice folder not found: ", d)
  run(tts, c(
    paste0("--vits-model=", shQuote(file.path(d, v$model))),
    paste0("--vits-tokens=", shQuote(file.path(d, "tokens.txt"))),
    paste0("--vits-data-dir=", shQuote(file.path(d, "espeak-ng-data"))),
    paste0("--sid=", v$sid),
    paste0("--vits-length-scale=", v$speed),
    paste0("--output-filename=", shQuote(out_wav)),
    shQuote(text)
  ))
}

# Ask Ollama for one line. Ollama may know a voice by its name, by its name
# without accents, or by its ID; the first form that works is remembered.
phonon_form <- new.env()
phonon_once <- function(voice_arg, text, out_wav) {
  body <- tempfile(fileext = ".json")
  on.exit(unlink(body))
  writeLines(toJSON(list(model = model, voice = voice_arg, input = text, response_format = "wav"),
                    auto_unbox = TRUE), body, useBytes = TRUE)
  unlink(out_wav)
  out <- suppressWarnings(system2("curl", c("-sS", "-o", shQuote(out_wav), "-w", "%{http_code}",
    "-H", shQuote("Content-Type: application/json"), "--data-binary", shQuote(paste0("@", body)),
    shQuote(paste0(ollama, "/v1/audio/speech"))), stdout = TRUE, stderr = TRUE))
  code <- tail(out, 1)
  ok <- identical(code, "200") && file.exists(out_wav) && file.size(out_wav) > 1000
  if (!ok) attr(ok, "why") <- paste0("HTTP ", code, ": ",
    if (file.exists(out_wav)) paste(readLines(out_wav, warn = FALSE), collapse = " ") else paste(out, collapse = " "))
  ok
}
speak_phonon <- function(key, v, text, out_wav) {
  known <- phonon_form[[key]]
  forms <- if (!is.null(known)) known else unique(c(v$name, chartr("éèë", "eee", v$name), v$id))
  why <- character()
  for (f in forms) {
    ok <- phonon_once(f, text, out_wav)
    if (isTRUE(ok)) { phonon_form[[key]] <- f; return(invisible()) }
    why <- c(why, paste0("  voice \"", f, "\": ", attr(ok, "why")))
  }
  stop("Ollama could not say a line in the voice ", key, ".\n", paste(why, collapse = "\n"),
       "\n'ollama show ", model, "' lists the voice names it knows.")
}

# Check Ollama is there and has the model before making 1,800 requests.
needs_phonon <- any(vapply(unique(lines$voice), function(x) identical(voice_table[[x]]$engine, "phonon"), TRUE))
if (needs_phonon) {
  tags <- tryCatch(suppressWarnings(fromJSON(paste0(ollama, "/api/tags"))), error = function(e) NULL)
  if (is.null(tags)) stop("Ollama is not answering at ", ollama, ". Open the Ollama app (or run 'ollama serve') and try again.")
  have <- tags$models$name %||% character()
  if (!any(have == model | have == paste0(model, ":latest"))) {
    stop("Ollama does not have ", model, ". Run: ollama pull ", model)
  }
}

tmp <- tempfile(fileext = ".wav")
made <- 0
for (i in seq_len(nrow(lines))) {
  key <- paste0(lines$voice[i], "|", lines$text[i])
  name <- clips[[key]]$f
  v <- voice_table[[lines$voice[i]]]
  if (is.null(v)) stop("Unknown voice \"", lines$voice[i], "\". Add it to voice_table in tools/make_audio.R.")
  spoken <- FALSE
  for (kind in c("", "_slow")) {
    ogg <- file.path(audio_dir, paste0(name, kind, ".ogg"))
    wav <- if (is.null(wav_dir)) NULL else file.path(wav_dir, "audio", paste0(name, kind, ".wav"))
    need <- force || !file.exists(ogg) || (!is.null(wav) && !file.exists(wav))
    if (need) {
      if (!spoken) { speak(lines$voice[i], lines$text[i], tmp); spoken <- TRUE }
      finish(tmp, ogg, wav, v$pitch, (if (identical(v$engine, "phonon")) tempo else 1) * (if (kind == "") 1 else slow_tempo))
      made <- made + 1
    }
  }
  clips[[key]]$ms   <- round(1000 * seconds(file.path(audio_dir, paste0(name, ".ogg"))))
  clips[[key]]$slow <- round(1000 * seconds(file.path(audio_dir, paste0(name, "_slow.ogg"))))
  if (i %% 20 == 0) cat("  ", i, "of", nrow(lines), "\n")
}
unlink(tmp)

writeLines(toJSON(list(clips = clips), auto_unbox = TRUE, pretty = TRUE), manifest_path, useBytes = TRUE)
cat("made", made, "clip files;", length(clips), "lines in", manifest_path, "\n")

# Clips that no unit uses any more are listed, not deleted.
on_disk <- sub("(_slow)?\\.ogg$", "", list.files(audio_dir, pattern = "\\.ogg$"))
stale <- setdiff(unique(on_disk), used)
if (length(stale) > 0) cat("no longer used (delete by hand if you like):", paste(stale, collapse = ", "), "\n")

# ---- 4. sound effects -------------------------------------------------------

sfx <- list(
  tap   = list(d = 0.06, expr = "0.35*sin(2*PI*880*t)*exp(-60*t)"),
  ok    = list(d = 0.32, expr = "0.45*sin(2*PI*660*t)*lt(t,0.10)*exp(-8*t)+0.45*sin(2*PI*990*t)*gte(t,0.10)*exp(-9*(t-0.10))"),
  no    = list(d = 0.34, expr = "0.40*sin(2*PI*196*t)*lt(t,0.13)+0.40*sin(2*PI*147*t)*gte(t,0.13)*exp(-6*(t-0.13))"),
  bump  = list(d = 0.25, expr = "0.9*sin(2*PI*95*t)*exp(-16*t)+0.15*sin(2*PI*300*t)*exp(-40*t)"),
  stamp = list(d = 0.22, expr = "0.95*sin(2*PI*70*t)*exp(-24*t)+0.2*sin(2*PI*1300*t)*exp(-70*t)"),
  win   = list(d = 0.75, expr = paste0(
    "0.4*sin(2*PI*523*t)*lt(t,0.12)+0.4*sin(2*PI*659*t)*gte(t,0.12)*lt(t,0.24)",
    "+0.4*sin(2*PI*784*t)*gte(t,0.24)*lt(t,0.36)+0.45*sin(2*PI*1047*t)*gte(t,0.36)*exp(-5*(t-0.36))")),
  # battles: a short sting when one starts, a bright blip for each point, a soft boing for a miss,
  # and a little tune for the win. All quiet and short so they never cover a voice for long.
  battle = list(d = 0.9, expr = paste0(
    "0.28*sin(2*PI*392*t)*lt(t,0.08)+0.28*sin(2*PI*523*t)*gte(t,0.08)*lt(t,0.16)",
    "+0.28*sin(2*PI*659*t)*gte(t,0.16)*lt(t,0.24)",
    "+(0.28*sin(2*PI*784*t)+0.10*sin(2*PI*1568*t))*(0.8+0.2*sin(2*PI*14*t))*gte(t,0.24)*exp(-3*(t-0.24))")),
  hit    = list(d = 0.30, expr = paste0(
    "0.30*sin(2*PI*784*t)*lt(t,0.06)",
    "+(0.30*sin(2*PI*1175*t)+0.07*sin(2*PI*2350*t))*gte(t,0.06)*exp(-11*(t-0.06))")),
  miss   = list(d = 0.30, expr = "0.26*sin(2*PI*(330*t-220*t*t))*exp(-7*t)"),
  victory = list(d = 1.35, expr = paste0(
    "0.28*sin(2*PI*523*t)*lt(t,0.1)+0.28*sin(2*PI*659*t)*gte(t,0.1)*lt(t,0.2)",
    "+0.28*sin(2*PI*784*t)*gte(t,0.2)*lt(t,0.3)+0.28*sin(2*PI*1047*t)*gte(t,0.3)*lt(t,0.5)",
    "+0.28*sin(2*PI*784*t)*gte(t,0.5)*lt(t,0.6)",
    "+(0.30*sin(2*PI*1047*t)+0.12*sin(2*PI*1319*t))*gte(t,0.6)*exp(-3.2*(t-0.6))"))
)
sfx_dir <- file.path(assets, "sfx")
dir.create(sfx_dir, recursive = TRUE, showWarnings = FALSE)
for (n in names(sfx)) {
  # the formula has commas in it, so it goes in quotes inside the filter
  src <- paste0("aevalsrc='", sfx[[n]]$expr, "':d=", sfx[[n]]$d, ":s=22050")
  fade <- paste0("afade=t=in:d=0.004,afade=t=out:st=", sfx[[n]]$d - 0.03, ":d=0.03")
  ogg <- file.path(sfx_dir, paste0(n, ".ogg"))
  if (force || !file.exists(ogg)) {
    run("ffmpeg", c("-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi", "-i", shQuote(src), "-af", shQuote(fade),
                    "-ac", "1", "-c:a", "libvorbis", "-q:a", "4", shQuote(ogg)))
  }
  if (!is.null(wav_dir)) {
    wav <- file.path(wav_dir, "sfx", paste0(n, ".wav"))
    dir.create(dirname(wav), recursive = TRUE, showWarnings = FALSE)
    if (force || !file.exists(wav)) {
      run("ffmpeg", c("-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi", "-i", shQuote(src), "-af", shQuote(fade),
                      "-ac", "1", "-c:a", "pcm_s16le", shQuote(wav)))
    }
  }
}
cat("sound effects:", paste(names(sfx), collapse = ", "), "\n")
