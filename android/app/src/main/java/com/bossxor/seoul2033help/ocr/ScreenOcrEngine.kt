package com.bossxor.seoul2033help.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class ScreenOcrEngine {
    private val client = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    /** Returns recognized text, "" on failure. */
    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        client.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it.text) }
            .addOnFailureListener { cont.resume("") }
    }

    fun close() = client.close()
}
