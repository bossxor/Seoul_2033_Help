package com.bossxor.seoul2033help.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class OcrLine(val text: String, val centerY: Float)

class ScreenOcrEngine {
    private val client = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    /** Returns recognized text, "" on failure. */
    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        client.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it.text) }
            .addOnFailureListener { cont.resume("") }
    }

    /** 줄 단위 결과. centerY 는 bitmap 안의 세로 중심(px). 실패 시 빈 목록. */
    suspend fun recognizeLines(bitmap: Bitmap): List<OcrLine> = suspendCancellableCoroutine { cont ->
        client.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { r ->
                cont.resume(r.textBlocks.flatMap { it.lines }.mapNotNull { l ->
                    l.boundingBox?.let { OcrLine(l.text, it.centerY().toFloat()) }
                })
            }
            .addOnFailureListener { cont.resume(emptyList()) }
    }

    fun close() = client.close()
}
