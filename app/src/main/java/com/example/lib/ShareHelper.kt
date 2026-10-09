package com.example.lib

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.Purchase
import java.io.File
import java.io.FileOutputStream

object ShareHelper {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")
    private val FACEBOOK_PACKAGES = listOf("com.facebook.katana", "com.facebook.lite")

    fun buildShareText(purchase: Purchase, stockRemaining: Int? = null): String {
        purchase.marketingCopy?.takeIf { it.isNotBlank() }?.let { return it.trim() }

        val stock = stockRemaining ?: purchase.quantity
        return buildString {
            append(purchase.name.trim())
            purchase.description?.takeIf { it.isNotBlank() }?.let {
                append("\n").append(it.trim())
            }
            if (purchase.potentialSellingPriceUsd > 0) {
                append("\nPrecio: $${String.format("%.2f", purchase.potentialSellingPriceUsd)} USD")
            }
            if (stock > 0) {
                append("\nDisponibles: $stock unidades")
            }
            append("\n¡Escríbeme para tu pedido!")
        }
    }

    fun imageUriFromBase64(context: Context, base64: String?): Uri? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(dir, "share_image.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareToWhatsApp(context: Context, text: String, imageUri: Uri?): Boolean =
        shareWithPackages(context, WHATSAPP_PACKAGES, text, imageUri)

    fun shareToFacebook(context: Context, text: String, imageUri: Uri?): Boolean =
        shareWithPackages(context, FACEBOOK_PACKAGES, text, imageUri)

    fun shareViaChooser(context: Context, text: String, imageUri: Uri?): Boolean {
        return try {
            val intent = buildSendIntent(text, imageUri)
            context.startActivity(Intent.createChooser(intent, "Compartir con..."))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun shareWithPackages(
        context: Context,
        packages: List<String>,
        text: String,
        imageUri: Uri?
    ): Boolean {
        for (pkg in packages) {
            try {
                val intent = buildSendIntent(text, imageUri).setPackage(pkg)
                context.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                // App not installed, try next package
            }
        }
        return false
    }

    private fun buildSendIntent(text: String, imageUri: Uri?): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = if (imageUri != null) "image/*" else "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (imageUri != null) {
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
