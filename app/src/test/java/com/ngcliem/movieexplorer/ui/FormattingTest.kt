package com.ngcliem.movieexplorer.ui

import com.ngcliem.movieexplorer.ui.components.formatRating
import com.ngcliem.movieexplorer.ui.detail.formatRuntime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

    @Test
    fun `runtime is shown as hours and minutes`() {
        assertEquals("2h 15m", formatRuntime(135))
        assertEquals("45m", formatRuntime(45))
        assertEquals("2h", formatRuntime(120))
    }

    @Test
    fun `rating is shown with one decimal`() {
        assertEquals("8.0", 8.0.formatRating())
        assertEquals("6.7", 6.66.formatRating())
    }
}
