package com.ngcliem.movieexplorer.domain

import com.ngcliem.movieexplorer.domain.model.VietQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VietQrTest {

    @Test
    fun crc16_matchesCcittFalseCheckValue() {
        assertEquals("29B1", VietQr.crc16("123456789"))
    }

    @Test
    fun payload_containsAccountAmountAndValidChecksum() {
        val payload = VietQr.payload("970436", "0123456789", 150000, "Meil ab-12!")

        assertTrue(payload.startsWith("000201010212"))
        assertTrue(payload.contains("0006970436")) // bank BIN
        assertTrue(payload.contains("01100123456789")) // account number
        assertTrue(payload.contains("5406150000")) // amount
        assertTrue(payload.contains("0809MEIL AB12")) // cleaned transfer note
        val body = payload.dropLast(4)
        assertTrue(body.endsWith("6304"))
        assertEquals(VietQr.crc16(body), payload.takeLast(4))
    }
}
