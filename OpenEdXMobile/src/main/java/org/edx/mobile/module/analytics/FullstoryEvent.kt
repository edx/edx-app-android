package org.edx.mobile.module.analytics

import android.content.res.Configuration
import org.edx.mobile.base.MainApplication
import org.edx.mobile.extenstion.isNotNullOrEmpty
import org.edx.mobile.module.analytics.Analytics.Keys
import org.edx.mobile.module.analytics.Analytics.Values
import org.edx.mobile.util.JavaUtil

class FullstoryEvent {
    val properties: MutableMap<String, Any?> = mutableMapOf()

    init {
        val loginPrefs = MainApplication.getEnvironment(MainApplication.instance()).loginPrefs

        properties[Keys.APP] = Values.APP_NAME
        properties.addDeviceOrientationProperty()

        // Include User ID in each event if the user is logged in
        if (loginPrefs.isUserLoggedIn)
            properties[Keys.USER_ID] = loginPrefs.userId
    }

    private fun MutableMap<String, Any?>.addDeviceOrientationProperty() {
        val configuration = MainApplication.instance().resources.configuration
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        this[Keys.DEVICE_ORIENTATION] = if (isPortrait) Values.PORTRAIT else Values.LANDSCAPE
    }
}

fun MutableMap<String, Any?>.addCourseContext(
    courseId: String?,
    unitUrl: String?,
    component: String?
) {
    this[Keys.CONTEXT] = buildMap {
        courseId?.let { this[Keys.COURSE_ID] = courseId }
        unitUrl?.let { this[Keys.OPEN_BROWSER] = unitUrl }
        component?.let { this[Keys.COMPONENT] = component }
    }
}

fun MutableMap<String, Any?>.addLabelAndCategory(
    label: String?,
    category: String?
) {
    if (label.isNotNullOrEmpty()) {
        this[Keys.LABEL] = label
    }
    if (category.isNotNullOrEmpty()) {
        this[Keys.CATEGORY] = category
    }
}

fun MutableMap<String, Any?>.addVideoProperties(
    videoId: String?,
    eventName: String,
    currentTime: Double? = null
) {
    this[Keys.NAME] = eventName
    this[Keys.CODE] = Values.MOBILE
    videoId?.let {
        this[Keys.MODULE_ID] = videoId
    }
    currentTime?.let {
        this[Keys.CURRENT_TIME] = JavaUtil.formatDoubleValue(currentTime, 3)
    }
}
