package org.edx.mobile.extension

import org.json.JSONObject

fun JSONObject.toMapOfString(): Map<String, String> = keys().asSequence().associateWith {
    it?.let { key -> this[key] as String } ?: ""
}
