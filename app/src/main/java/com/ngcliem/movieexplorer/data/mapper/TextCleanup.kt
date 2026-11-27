package com.ngcliem.movieexplorer.data.mapper

import java.text.Normalizer

// TMDB text is typed in by many contributors and is not always clean. These helpers
// fix the problems that make movie titles and overviews render badly on screen.

private val INVISIBLE_CHARS = Regex("[\\u200B-\\u200F\\u2060\\uFEFF\\u00AD]")
private val HORIZONTAL_SPACES = Regex("[ \\t\\u00A0\\u3000]+")
private val SPACES_AROUND_NEWLINE = Regex(" *\\n *")
private val EXTRA_NEWLINES = Regex("\\n{3,}")

/**
 * - Composes decomposed Vietnamese (NFD: "e" + combining accent) into single characters
 *   (NFC); otherwise accents are drawn detached or misplaced.
 * - Removes zero-width / invisible characters that cause odd gaps and line breaks.
 * - Collapses repeated spaces and blank lines, and trims.
 */
fun String.cleanText(): String =
    Normalizer.normalize(this, Normalizer.Form.NFC)
        .replace("\r\n", "\n")
        .replace(INVISIBLE_CHARS, "")
        .replace(HORIZONTAL_SPACES, " ")
        .replace(SPACES_AROUND_NEWLINE, "\n")
        .replace(EXTRA_NEWLINES, "\n\n")
        .trim()

/**
 * True when the text contains letters outside the Latin alphabet (Telugu, Tamil, Hindi,
 * Japanese, Korean, Thai, Cyrillic…). TMDB shows the original title when a movie has no
 * Vietnamese title, which most users cannot read.
 */
fun String.hasNonLatinLetters(): Boolean = any { char ->
    char.isLetter() && Character.UnicodeScript.of(char.code) != Character.UnicodeScript.LATIN
}
