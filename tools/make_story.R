#!/usr/bin/env Rscript
# Writes the whole story as one page to read: docs/STORY.md.
#
# It walks every unit in playing order and prints what the player would see
# and hear on the right path: the story cards, each conversation, the walking
# scenes, the taxi ride and the sketches. Wrong answers and what they lead to
# are listed under each question, because that is where most of the jokes are.
#
# Run from the project folder after changing any unit:
#   Rscript tools/make_story.R
# Options:
#   --assets DIR   assets folder (default app/src/main/assets)
#   --out FILE     where to write (default docs/STORY.md)
#   --name NAME    the detective's name (default Viv)
#   --short        leave out wrong answers, side remarks and word lists

suppressPackageStartupMessages(library(jsonlite))

if (!isTRUE(l10n_info()[["UTF-8"]])) {
  for (loc in c("C.UTF-8", "en_US.UTF-8", "UTF-8")) {
    ok <- suppressWarnings(Sys.setlocale("LC_CTYPE", loc))
    if (nzchar(ok) && isTRUE(l10n_info()[["UTF-8"]])) break
  }
}

args <- commandArgs(trailingOnly = TRUE)
opt <- function(name, default = NULL) {
  i <- match(paste0("--", name), args)
  if (is.na(i)) default else args[i + 1]
}
`%||%` <- function(a, b) if (is.null(a) || length(a) == 0 || identical(a, "")) b else a

assets <- opt("assets", "app/src/main/assets")
out_file <- opt("out", "docs/STORY.md")
hero <- opt("name", "Viv")
short <- "--short" %in% args

out <- character()
say <- function(...) out <<- c(out, paste0(...))
gap <- function() if (length(out) > 0 && nzchar(out[length(out)])) out <<- c(out, "")

fill <- function(s) gsub("{name}", hero, s %||% "", fixed = TRUE)
# keep a line break inside a story card as a break on the page
para <- function(s) gsub("\n", "  \n", fill(s), fixed = TRUE)

# The English for a line: its translation, or its list of new words, or nothing.
help_for <- function(ln) {
  if (isTRUE(ln$lost)) return(" *(too fast to follow)*")
  if (!is.null(ln$en) && nzchar(ln$en) && ln$en != "???") return(paste0(" *(", ln$en, ")*"))
  if (length(ln$gloss) > 0) return(paste0(" *(", paste(unlist(ln$gloss), collapse = "; "), ")*"))
  ""
}

# Names for speakers who are not the main person of a conversation.
known <- c(teacher = "Voice", hero = hero, noir = "Monsieur Noir", helper = "The helper", thief = "The thief",
           crew = "Cabin crew", customer = "A customer", owner = "Shop owner", officer = "Police officer",
           fishmonger = "Fishmonger", seller = "Stall keeper", seller_m = "Stall keeper", waiter = "Waiter", diner = "A diner",
           priest = "The priest", maid = "Housekeeper", bellhop = "Porter", clerk = "Hotel clerk",
           driver = "Taxi driver", guard = "Guard", claire = "Claire", barista = "Café man",
           cleaner = "Cleaner", passer = "Passer-by", fast = "Man on the bridge", lady = "The lady",
           room = "Room service", georges = "Georges Raton", king = "King Loir XVI", queen = "Queen Sourie Antoinette",
           lefevre = "Madame Lefèvre", prof = "Madame Duval", dancer = "A dancer", team = "Madame Martin", crowd = "The crowd",
           guide = "The guide", worker = "The sewer worker", manager = "Stage manager", maitre = "Head waiter",
           barman = "Barman", chef = "The chef", tourist = "A tourist",
           host = "Simone, on the phone", jeweller = "The jeweller", bride = "A bride", dad = "A father", granny = "A grandmother",
           bookman = "Hugo, the man with the book", guichet = "Ticket agent", woman = "A woman on the platform",
           announcer = "The announcer", staff = "The bookseller", designer = "A designer", ines = "Inès", zoe = "Zoé",
           nina = "Nina", maya = "Maya", concierge = "The concierge", paul = "Paul", inspector = "The inspector",
           celeste = "Céleste Rivière")
who_name <- function(who, default = "Someone") {
  if (is.null(who) || !nzchar(who)) return(default)
  if (!is.na(known[who])) return(unname(known[who]))
  if (grepl("^agent|^gate|^g[0-9]", who)) return("Gate agent")
  if (grepl("^m[0-9]+$", who)) return("A mouse")
  if (grepl("^guard[0-9]+$", who)) return("A guard")
  paste0(toupper(substr(who, 1, 1)), substr(who, 2, nchar(who)))
}

line_out <- function(ln, default_who, indent = "") {
  if (is.null(ln)) return(invisible())
  say(indent, "**", who_name(ln$who, default_who), ":** ", fill(ln$fr), help_for(ln))
  gap()
}

# ---- one kind of scene each -------------------------------------------------

story_out <- function(cards) {
  for (c in cards) {
    if (identical(c$scene, "letter")) {
      say("> ", gsub("\n", "  \n> ", fill(c$text), fixed = TRUE))
    } else {
      say(para(c$text))
    }
    gap()
    if (length(c$people) > 0 && !short) { say("*(A picture of the people to look at closely.)*"); gap() }
    line_out(c$line, "Voice")
    if (!is.null(c$note) && nzchar(c$note)) { say("*", fill(c$note), "*"); gap() }
  }
}

# Pages a notebook step covers: "all", or a 1-based range such as "1-3" or "4".
page_range <- function(ref, count) {
  if (is.null(ref) || ref %in% c("", "all")) return(seq_len(count))
  b <- as.integer(strsplit(ref, "-", fixed = TRUE)[[1]])
  if (length(b) == 1) b <- c(b, b)
  b <- pmin(pmax(b, 1), count)
  if (b[1] > b[2]) integer(0) else seq(b[1], b[2])
}

drill_out <- function(u, ref) {
  if (short) return(invisible())
  words <- list()
  for (w in u$words) words[[w$id]] <- w
  tips <- list()
  for (t in u$tips) tips[[t$id]] <- t
  shown <- character(0)
  for (i in page_range(ref, length(u$notebook$pages))) {
    pg <- u$notebook$pages[[i]]
    ws <- vapply(pg$words, function(id) paste0(words[[id]]$fr, " (", words[[id]]$en, ")"), "")
    say("- **", pg$title, ".** ", if (length(ws) > 0) paste(ws, collapse = "; ") else "Old words come back.")
    shown <- c(shown, unlist(pg$tip), unlist(pg$tips))
  }
  gap()
  for (id in unique(shown)) if (!is.null(tips[[id]])) say("- *Tip: ", tips[[id]]$title, ".* ", tips[[id]]$text)
  gap()
}

talk_out <- function(tk) {
  npc <- tk$name %||% who_name(tk$who)
  if (isTRUE(tk$phone)) { say("*On the phone: she hears the voice but sees no words until she has tried twice.*"); gap() }
  id <- tk$start
  seen <- character()
  while (!is.null(id) && !(id %in% seen)) {
    seen <- c(seen, id)
    n <- tk$nodes[[id]]
    if (is.null(n)) break
    # the character's line first, then the storyteller's remark on it
    if (!is.null(n[["npc"]])) line_out(n[["npc"]], npc)
    if (!is.null(n[["narr"]])) { say(fill(n[["narr"]])); gap() }
    ask <- n$ask %||% "choice"
    nxt <- n[["next"]]
    if (ask == "choice") {
      right <- NULL
      for (o in n$options) if (!is.null(o[["next"]])) right <- o
      if (!is.null(right)) { say("**", hero, ":** ", fill(right$fr)); gap(); nxt <- right[["next"]] }
      if (!short) {
        for (o in n$options) if (is.null(o[["next"]])) {
          r <- o$react
          say("- If she says \"", fill(o$fr), "\": ",
              if (!is.null(r)) paste0(npc, " says \"", fill(r$fr), "\"", help_for(r), ". ") else "",
              fill(o[["narr"]] %||% ""))
        }
        gap()
      }
    } else if (ask == "build") {
      b <- n$build
      say("**", hero, "** *(putting word tiles in order)*: ", fill(paste(unlist(b$tiles), collapse = " ")))
      gap()
    } else if (ask == "give") {
      pretty <- function(x) gsub("_", " ", sub("^dir:", "arrow ", sub("^(dress|shoes):([^:]+)", "\\1 (\\2)", x)))
      say("*She taps the right picture: ", pretty(n$answer), ", out of ", paste(pretty(unlist(n$items)), collapse = ", "), ".*")
      gap()
    } else if (ask == "pay") {
      say("*She pays ", n$pay$amount, " euros with coins and notes of ", paste(unlist(n$pay$coins), collapse = ", "), ".*")
      gap()
    } else if (ask == "fact") {
      f <- n$fact
      if (length(f$note) > 0 && !short) { say("> ", paste(unlist(f$note), collapse = "  \n> ")); gap() }
      say("*For her notebook. ", f$prompt, " Choices: ", paste(unlist(f$options), collapse = ", "), ". Answer: ", f$answer, ".*")
      gap()
    } else if (ask == "sort") {
      q <- n$sort
      bins <- vapply(q$bins, function(b) b$fr, "")
      for (it in q$items) {
        line_out(it[["line"]], npc)
        say("*", hero, " puts ", it$name, " under **", bins[it$bin + 1], "**.*")
        gap()
      }
    } else if (ask == "order") {
      q <- n$order
      labels <- unlist(q$labels)
      say("*", hero, " puts the steps in order:*")
      gap()
      for (k in seq_along(q$items)) say(k, ". ", if (k <= length(labels)) paste0(labels[k], ": ") else "", q$items[[k]]$fr)
      gap()
    } else if (ask == "plan") {
      q <- n$plan
      lab <- ""
      for (r in q$rooms) if (identical(r$id, q$answer)) lab <- r$label
      say("*", q$prompt, " She marks ", lab, " on the plan.*")
      gap()
    } else if (ask == "duel") {
      d <- n$duel
      say("*A spoken battle: \"", d$title %||% npc, " vous défie !\" ", hero,
          " wins the speaker over by saying phrases aloud. How well she says them, and whether they suit the speaker's mood, sets how much doubt each one takes away. She cannot lose.*")
      gap()
      tags <- c(greet = "SALUER", plead = "DEMANDER", insist = "INSISTER", who = "SE PRÉSENTER")
      for (m in d$moves) say("- ", tags[[m$kind]], ": ", m$fr, if (!short && !is.null(m$en)) paste0(" (", m$en, ")") else "")
      gap()
      if (!short) {
        say("*Moods: in a hurry (pressé), INSISTER works best; kind (gentil), DEMANDER; suspicious (méfiant), SE PRÉSENTER.",
            if (any(vapply(d$moves, function(m) m$kind == "greet", TRUE))) " Saying hello first matters: before it, every phrase does half." else "", "*")
        gap()
      }
      if (!is.null(d$win)) line_out(d$win, npc)
    } else if (ask == "route") {
      r <- n$route
      say("*", r$prompt, " There are ", length(r$routes), " routes drawn on a map. Answer: ", r$answer, ".*")
      gap()
    }
    if (ask == "end") break
    id <- nxt
  }
}

walk_out <- function(a) {
  if (!is.null(a$tip) && nzchar(a$tip)) { say("*", fill(a$tip), "*"); gap() }
  i <- 0
  for (q in a$quest) {
    i <- i + 1
    say(i, ". ", fill(q$hint %||% ""))
    gap()
    # [[ ]] and not $: R's $ would take "doneNarr" when asked for a missing "done"
    line_out(q[["line"]], who_name(q[["who"]]), "   ")
    for (pl in q[["places"]]) line_out(pl[["line"]], who_name(q[["who"]]), "   ")
    if (identical(q$type, "carry")) {
      say("   *She carries ", if (length(q$to) > 0) paste0("one to each of ", length(q$to), " people who want it") else "each one to the marked place",
          if (isTRUE(q$melt)) ". In the sun it gets soft and slow; in the shade it firms up again" else "", ".*")
      gap()
      if (!short && !is.null(q[["wrong"]])) line_out(q[["wrong"]], "Someone already served", "   ")
    }
    line_out(q[["done"]], who_name(q[["who"]]), "   ")
    if (!is.null(q[["doneNarr"]])) { say("   ", fill(q[["doneNarr"]])); gap() }
  }
  if (short) return(invisible())
  side <- character()
  for (n in a$npcs) if (!is.null(n[["line"]])) {
    side <- c(side, paste0("- ", who_name(n$line$who %||% n$id), ": \"", fill(n$line$fr), "\"", help_for(n$line),
                           if (!is.null(n[["narr"]])) paste0(" ", fill(n[["narr"]])) else ""))
  }
  for (s in a$spots) {
    side <- c(side, paste0("- ", if (!is.null(s[["line"]])) paste0(who_name(s$line$who), ": \"", fill(s$line$fr), "\"", help_for(s$line), " ") else "",
                           fill(s[["narr"]] %||% "")))
  }
  for (m in c(a$movers, a$followers)) if (!is.null(m[["line"]])) {
    side <- c(side, paste0("- On a bump, ", tolower(who_name(m$line$who %||% m$id)), ": \"", fill(m$line$fr), "\"", help_for(m$line)))
  }
  for (w in a$watchers) if (!is.null(w[["line"]])) {
    side <- c(side, paste0("- If ", tolower(who_name(w$line$who %||% w$id)), " sees her: \"", fill(w$line$fr), "\"", help_for(w$line), " Back to the last star."))
  }
  side <- unique(side)
  if (length(side) > 0) { say("Off the path:"); gap(); for (s in side) say(s); gap() }
}

ride_out <- function(r) {
  if (!is.null(r$tip)) { say("*", r$tip, "*"); gap() }
  drive <- identical(r$mode, "drive")
  moves <- c(left = "turns left", right = "turns right", straight = "goes straight on", faster = "speeds up", slower = "slows down")
  reply <- function(l) {
    if (drive) return(paste0("*(", moves[[l$turn]], ")*"))
    if (identical(l$ask, "confirm")) {
      if (identical(l$offer, l$turn)) r$says$yes$fr else r$says[[paste0("no_", l$turn)]]$fr
    } else r$says[[l$turn]]$fr
  }
  i <- 0
  for (l in r$legs) {
    i <- i + 1
    if (!is.null(l$sight)) { say("*Out of the window: ", l$sight$title, ". ", l$sight$text, "*"); gap() }
    line_out(l$line, who_name(r$who))
    say("**", hero, ":** ", reply(l)); gap()
  }
  if (!short && !is.null(r$wrong)) { say("- After a wrong turn, round the block: \"", r$wrong$fr, "\"", help_for(r$wrong)); gap() }
  line_out(r$arrive, who_name(r$who))
}

sketch_out <- function(s) {
  if (!is.null(s$tip)) { say("*", s$tip, "*"); gap() }
  for (p in s$people) {
    line_out(p$intro, who_name(s$who))
    for (f in p$features) {
      others <- setdiff(vapply(f$options, function(o) o$fr, ""), f$answer)
      say("**", hero, ":** ", sub("{x}", f$answer, f$fr, fixed = TRUE),
          if (!short) paste0(" *(not ", paste(others, collapse = ", "), ")*") else "")
      gap()
    }
    line_out(s$right, who_name(s$who))
  }
}

game_out <- function(gm) {
  if (is.null(gm)) return(invisible())
  if (!is.null(gm$tip)) { say("*", fill(gm$tip), "*"); gap() }
  if (identical(gm$kind, "block")) {
    if (!short) for (c in gm$chants) line_out(c, "The crowd")
    if (!short) line_out(gm$blocked, "The driver")
    line_out(gm$stuck, "The driver")
  } else if (identical(gm$kind, "boules")) {
    say("*Pétanque against the dancers, first to ", gm$to %||% 3, " points.*"); gap()
    for (k in c("pointUs", "pointThem", if (!short) "again", "win")) line_out(gm[[k]], who_name(gm$who))
  } else if (identical(gm$kind, "spy")) {
    for (t in gm$targets) if (isTRUE(t$right) || !short) {
      say("- ", if (isTRUE(t$right)) "**The right one:** " else "", fill(t$say$fr), help_for(t$say))
    }
    gap()
  } else if (identical(gm$kind, "metro")) {
    for (t in gm$trips) {
      names <- vapply(t$stations, function(x) x$fr, "")
      a <- match(t$from, names); b <- match(t$to, names)
      say("- Line ", t$line, ", direction ", if (b > a) t$ends[[2]] else t$ends[[1]], ": from ", t$from, " to ", t$to, ", ", abs(b - a), " stops.")
    }
    gap()
    if (!short) for (k in c("wrongWay", "early", "missed")) line_out(gm[[k]], hero)
    line_out(gm$change, who_name(gm$who))
    line_out(gm$done, hero)
  } else if (identical(gm$kind, "dress")) {
    for (m in gm$models) {
      for (w in m$wants) line_out(w$line, who_name(m$who))
      if (!short) line_out(m$thanks, who_name(m$who))
    }
    if (!short) line_out(gm$wrong, who_name(gm$who))
    line_out(gm$done, who_name(gm$who))
  } else if (identical(gm$kind, "belt")) {
    shelves <- vapply(gm$shelves, function(x) x$fr, "")
    if (!short) for (c in gm$cheeses) say("- ", c$name, if (!is.null(c$tag)) paste0(" (", c$tag, ")") else " (no label)", ": ", shelves[c$shelf + 1])
    gap()
    if (!short) line_out(gm$wrong, who_name(gm$who))
    line_out(gm$done, who_name(gm$who))
  }
  if (!is.null(gm$doneNarr)) { say(fill(gm$doneNarr)); gap() }
}

# ---- every unit, in playing order -------------------------------------------

index <- fromJSON(file.path(assets, "units", "index.json"), simplifyVector = FALSE)
say("# The story"); gap()
say("Written out by `tools/make_story.R` from the unit files, with the detective named ", hero,
    ". French lines are followed by the help the player can get for them, in brackets. ",
    "Do not edit this page: change the unit files and run the script again.")
gap()
kinds <- c(drill = "", story = "", talk = "Conversation", walk = "On foot", ride = "The ride", sketch = "The sketch")
n <- 0
for (case in index$cases) {
  say("# ", case$title); gap()
  for (info in case$units) {
    n <- n + 1
    u <- fromJSON(file.path(assets, "units", paste0(info$id, ".json")), simplifyVector = FALSE)
    say("## ", n, ". ", u$title); gap()
    if (!is.null(u$code) && nzchar(u$code)) { say("Secret code: **", u$code, "**"); gap() }
    if (!is.null(u$intro)) story_out(u$story[[u$intro]])
    for (st in u$stages) {
      say("### ", st$title, ": ", st$sub); gap()
      for (step in st$steps) {
        kind <- names(step)[1]
        ref <- step[[1]]
        if (kind == "drill") drill_out(u, ref)
        else if (kind == "story") story_out(u$story[[ref]])
        else if (kind == "talk") talk_out(u$talks[[ref]])
        else if (kind == "walk") walk_out(u$walks[[ref]])
        else if (kind == "ride") ride_out(u$rides[[ref]])
        else if (kind == "sketch") sketch_out(u$sketches[[ref]])
        else if (kind == "game") game_out(u$games[[ref]])
      }
      if (!is.null(st$done) && nzchar(st$done)) { say("*", fill(st$done), "*"); gap() }
    }
  }
}

dir.create(dirname(out_file), recursive = TRUE, showWarnings = FALSE)
writeLines(enc2utf8(out), out_file, useBytes = TRUE)
cat("wrote", out_file, ":", length(out), "lines,", sum(lengths(strsplit(out, "\\s+"))), "words\n")
