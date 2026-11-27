package com.ngcliem.movieexplorer.data

import com.ngcliem.movieexplorer.data.mapper.cleanText
import com.ngcliem.movieexplorer.data.mapper.hasNonLatinLetters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextCleanupTest {

    @Test
    fun cleanText_composesDecomposedVietnamese() {
        val decomposed = "Thất Đại Tội".let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
        assertTrue(decomposed.length > "Thất Đại Tội".length)
        assertEquals("Thất Đại Tội", decomposed.cleanText())
    }

    @Test
    fun cleanText_removesInvisibleCharsAndExtraSpaces() {
        assertEquals("chứng kiến vụ án", "chứng kiến ​​vụ  án ".cleanText())
        assertEquals("Josee, Khi Nàng Thơ Yêu", "Josee,  Khi Nàng Thơ Yêu".cleanText())
        assertEquals("a\n\nb", "a \r\n\r\n\r\n\r\n b".cleanText())
    }

    @Test
    fun hasNonLatinLetters_detectsForeignScripts() {
        assertTrue("కల్కి 2898-ఎ.డి".hasNonLatinLetters())
        assertTrue("ルックバック".hasNonLatinLetters())
        assertTrue("Иди и смотри".hasNonLatinLetters())
        assertFalse("Thất Đại Tội".hasNonLatinLetters())
        assertFalse("Kalki 2898-AD".hasNonLatinLetters())
        assertFalse("Se7en: 7 tội lỗi!".hasNonLatinLetters())
    }
}
