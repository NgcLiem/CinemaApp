package com.ngcliem.movieexplorer.ui.booking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.ngcliem.movieexplorer.BuildConfig
import com.ngcliem.movieexplorer.R
import com.ngcliem.movieexplorer.domain.model.VietQr
import com.ngcliem.movieexplorer.ui.components.AppDialog
import com.ngcliem.movieexplorer.ui.components.DialogPrimaryButton
import com.ngcliem.movieexplorer.ui.components.DialogSecondaryButton
import com.ngcliem.movieexplorer.ui.components.DialogTone
import com.ngcliem.movieexplorer.ui.components.formatVnd
import kotlinx.coroutines.delay

@Composable
fun QrPaymentDialog(
    payment: PaymentSession,
    isVerifying: Boolean,
    onPaid: () -> Unit,
    onRenew: () -> Unit,
    onCancel: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(payment.expiresAt) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= payment.expiresAt) break
            delay(1000)
        }
    }
    val remainingSec = ((payment.expiresAt - now) / 1000).coerceAtLeast(0)
    val expired = remainingSec == 0L
    val accent = DialogTone.INFO.accent

    AppDialog(
        onDismissRequest = onCancel,
        dismissible = !isVerifying,
        tone = DialogTone.INFO,
        icon = Icons.Filled.QrCode2,
        title = stringResource(R.string.pay_qr_title),
        confirmButton = {
            if (expired) {
                DialogPrimaryButton(stringResource(R.string.pay_qr_renew), onClick = onRenew)
            } else {
                DialogPrimaryButton(
                    text = stringResource(if (isVerifying) R.string.pay_qr_verifying else R.string.pay_qr_done),
                    onClick = onPaid,
                    loading = isVerifying,
                )
            }
        },
        dismissButton = {
            DialogSecondaryButton(stringResource(R.string.action_cancel), onClick = onCancel, enabled = !isVerifying)
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                formatVnd(payment.amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
            Text(
                stringResource(R.string.pay_qr_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // QR card (always black on white so every scanner can read it).
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(2.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                key(payment.qrPayload) { QrCode(payment.qrPayload, Modifier.fillMaxSize()) }
                if (expired) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.pay_qr_expired),
                            color = DialogTone.DANGER.accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Countdown pill.
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background((if (remainingSec < 60) DialogTone.DANGER.accent else accent).copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = null,
                    tint = if (remainingSec < 60) DialogTone.DANGER.accent else accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.pay_qr_expires_in, "%02d:%02d".format(remainingSec / 60, remainingSec % 60)),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (payment.isRealTransfer) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.07f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    PaymentLine(stringResource(R.string.label_bank), VietQr.bankName(BuildConfig.VIETQR_BANK_BIN))
                    PaymentLine(stringResource(R.string.label_account_no), BuildConfig.VIETQR_ACCOUNT_NO, copyable = true)
                    PaymentLine(stringResource(R.string.label_account_name), BuildConfig.VIETQR_ACCOUNT_NAME)
                    PaymentLine(stringResource(R.string.label_transfer_note), payment.transferNote, copyable = true)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DialogTone.GOLD.accent.copy(alpha = 0.12f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = DialogTone.GOLD.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.pay_qr_demo), style = MaterialTheme.typography.bodySmall)
                }
                PaymentLine(stringResource(R.string.label_transfer_note), payment.transferNote, copyable = true)
            }
        }
    }
}

@Composable
private fun PaymentLine(label: String, value: String, copyable: Boolean = false) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.width(104.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (copyable) FontFamily.Monospace else FontFamily.Default,
        )
        if (copyable) {
            IconButton(onClick = { clipboard.setText(AnnotatedString(value)); copied = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy),
                    tint = if (copied) DialogTone.SUCCESS.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** Renders [content] as a QR code (error correction M, quiet zone handled by the caller's padding). */
@Composable
private fun QrCode(content: String, modifier: Modifier = Modifier) {
    val matrix: BitMatrix = remember(content) {
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0),
        )
    }
    Canvas(modifier) {
        val cell = size.minDimension / matrix.width
        val offset = Offset((size.width - cell * matrix.width) / 2, (size.height - cell * matrix.height) / 2)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    drawRect(
                        Color(0xFF1B1B2F),
                        topLeft = Offset(offset.x + x * cell, offset.y + y * cell),
                        // Slight overlap avoids hairline gaps between cells.
                        size = Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
        }
    }
}
