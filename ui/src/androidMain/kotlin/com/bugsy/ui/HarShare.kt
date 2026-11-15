package com.bugsy.ui

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.bugsy.core.HarSerializer
import com.bugsy.core.model.DebugRequest
import java.io.File

private const val HAR_MIME_TYPE = "application/json"
private const val CACHE_DIR = "bugsy"

/**
 * Writes the HAR to the app's cache and opens the native share sheet with it attached. The file lives in the
 * cache (never in external storage) and is shared through a FileProvider, so only the chosen target can read it.
 */
internal fun shareHar(context: Context, requests: List<DebugRequest>) {
    if (requests.isEmpty()) return
    val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
    // Old exports may hold real values the user chose to share once: don't let them pile up.
    dir.listFiles { file -> file.extension == "har" }?.forEach { it.delete() }
    val file = File(dir, "bugsy-${System.currentTimeMillis()}.har")
    file.writeText(HarSerializer.serialize(requests, revealSecrets = true))

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.bugsy.fileprovider", file)
    val builder = ShareCompat.IntentBuilder(context)
        .setType(HAR_MIME_TYPE)
        .setStream(uri)
        .setChooserTitle("Bugsy HAR")
    // Explicit grant so the chosen app can read the file regardless of how the chooser propagates permissions.
    builder.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(builder.createChooserIntent())
}
