package com.bugsy.ui

import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import java.io.File

private const val MOCK_RULES_MIME_TYPE = "application/json"
private const val CACHE_DIR = "bugsy"

/** Writes [json] (an exported mock rule set) to the app's cache and opens the native share sheet with it. */
internal fun shareMockRules(context: Context, json: String) {
    val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
    dir.listFiles { file -> file.extension == "json" }?.forEach { it.delete() }
    val file = File(dir, "bugsy-mocks-${System.currentTimeMillis()}.json")
    file.writeText(json)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.bugsy.fileprovider", file)
    val builder = ShareCompat.IntentBuilder(context)
        .setType(MOCK_RULES_MIME_TYPE)
        .setStream(uri)
        .setChooserTitle("Bugsy mock rules")
    builder.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(builder.createChooserIntent())
}
