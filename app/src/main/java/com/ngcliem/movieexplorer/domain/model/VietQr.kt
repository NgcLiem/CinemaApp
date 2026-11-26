package com.ngcliem.movieexplorer.domain.model

/**
 * Builds VietQR (NAPAS 247, EMVCo) payloads that any Vietnamese banking app can scan
 * to pre-fill a bank transfer with the receiving account, amount and transfer note.
 */
object VietQr {

    private const val NAPAS_GUID = "A000000727"
    private const val SERVICE_TRANSFER_TO_ACCOUNT = "QRIBFTTA"
    private const val CURRENCY_VND = "704"
    private const val COUNTRY_VN = "VN"

    /**
     * @param bankBin 6-digit NAPAS bank BIN (e.g. 970436 for Vietcombank).
     * @param accountNo receiving account number.
     * @param amount amount in VND.
     * @param note transfer note; only A-Z, 0-9 and spaces are kept so every bank accepts it.
     */
    fun payload(bankBin: String, accountNo: String, amount: Long, note: String): String {
        val beneficiary = field("00", bankBin) + field("01", accountNo)
        val merchantAccount = field("00", NAPAS_GUID) +
            field("01", beneficiary) +
            field("02", SERVICE_TRANSFER_TO_ACCOUNT)
        val cleanNote = note.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' || it == ' ' }.take(25)

        val body = field("00", "01") +
            field("01", "12") + // dynamic QR: amount is fixed for this order
            field("38", merchantAccount) +
            field("53", CURRENCY_VND) +
            field("54", amount.toString()) +
            field("58", COUNTRY_VN) +
            field("62", field("08", cleanNote)) +
            "6304"
        return body + crc16(body)
    }

    /** Short name of common banks by NAPAS BIN, for display only. */
    fun bankName(bin: String): String = when (bin) {
        "970436" -> "Vietcombank"
        "970415" -> "VietinBank"
        "970418" -> "BIDV"
        "970405" -> "Agribank"
        "970422" -> "MB Bank"
        "970407" -> "Techcombank"
        "970416" -> "ACB"
        "970432" -> "VPBank"
        "970423" -> "TPBank"
        "970403" -> "Sacombank"
        "970441" -> "VIB"
        "970443" -> "SHB"
        else -> bin
    }

    private fun field(id: String, value: String): String =
        id + value.length.toString().padStart(2, '0') + value

    /** CRC-16/CCITT-FALSE, as required by EMVCo tag 63. */
    internal fun crc16(data: String): String {
        var crc = 0xFFFF
        data.toByteArray(Charsets.UTF_8).forEach { byte ->
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc.toString(16).uppercase().padStart(4, '0')
    }
}
