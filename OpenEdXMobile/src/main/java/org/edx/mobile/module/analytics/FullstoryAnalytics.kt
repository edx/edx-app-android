package org.edx.mobile.module.analytics

import com.fullstory.FS
import com.fullstory.FSSessionData
import org.edx.mobile.deeplink.DeepLink
import org.edx.mobile.extenstion.isNotNullOrEmpty
import org.edx.mobile.logger.Logger
import org.edx.mobile.model.video.VideoQuality
import org.edx.mobile.module.analytics.Analytics.Events
import org.edx.mobile.module.analytics.Analytics.Keys
import org.edx.mobile.module.analytics.Analytics.Screens
import org.edx.mobile.module.analytics.Analytics.Values
import org.edx.mobile.util.AnalyticsUtils
import org.edx.mobile.util.Config
import org.edx.mobile.util.JavaUtil
import org.edx.mobile.util.images.ShareUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullstoryAnalytics @Inject constructor(
    config: Config
) : Analytics {

    private val logger: Logger = Logger(TAG)

    init {
        if (config.fullstoryConfig.isEnabled) {
            FS.setReadyListener { sessionData: FSSessionData ->
                val sessionUrl = sessionData.currentSessionURL
                logger.debug("FullStory Session URL is: $sessionUrl")
            }
        }
    }

    private fun trackFullstoryPage(name: String, properties: Map<String, Any?>) {
        logger.debug("Page : $name $properties")
        FS.page(name, properties).start()
    }

    private fun trackFullstoryEvent(name: String, properties: Map<String, Any?>) {
        logger.debug("Event: $name $properties")
        FS.event(name, properties)
    }

    override fun identifyUser(userID: String, email: String?, username: String) {
        FS.identify(
            userID, mapOf(
                "displayName" to userID
            )
        )
    }

    override fun resetIdentifyUser() {}

    override fun trackScreenView(
        screenName: String,
        courseId: String?,
        values: Map<String, String>?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            if (courseId.isNotNullOrEmpty()) {
                properties[DeepLink.Keys.COURSE_ID] = courseId
            }
            values?.let { properties.putAll(it) }
            properties.addLabelAndCategory(screenName, "")
        }
        trackFullstoryPage(screenName, fullstoryEvent.properties)
    }

    override fun trackEvent(eventName: String, biValue: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = biValue
        }
        trackFullstoryEvent(eventName, fullstoryEvent.properties)
    }

    override fun trackVideoPlaying(
        videoId: String?,
        currentTime: Double?,
        courseId: String?,
        unitUrl: String?,
        playMedium: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.VIDEO_PLAYED, currentTime)
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)
            if (playMedium.isNotNullOrEmpty()) {
                properties[Keys.PLAY_MEDIUM] = playMedium
            }
        }
        trackFullstoryEvent(Events.PLAYED_VIDEO, fullstoryEvent.properties)
    }

    override fun trackVideoPause(
        videoId: String,
        currentTime: Double?,
        courseId: String?,
        unitUrl: String?,
        playMedium: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.VIDEO_PAUSED, currentTime)
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)
            if (playMedium.isNotNullOrEmpty()) {
                properties[Keys.PLAY_MEDIUM] = playMedium
            }
        }
        trackFullstoryEvent(Events.PAUSED_VIDEO, fullstoryEvent.properties)
    }

    override fun trackVideoStop(
        videoId: String?,
        currentTime: Double?,
        courseId: String?,
        unitUrl: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.VIDEO_STOPPED, currentTime)
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)
        }
        trackFullstoryEvent(Events.STOPPED_VIDEO, fullstoryEvent.properties)
    }

    override fun trackShowTranscript(
        videoId: String?,
        currentTime: Double?,
        courseId: String?,
        unitUrl: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.TRANSCRIPT_SHOWN, currentTime)
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)
        }
        trackFullstoryEvent(Events.SHOW_TRANSCRIPT, fullstoryEvent.properties)
    }

    override fun trackHideTranscript(
        videoId: String?,
        currentTime: Double?,
        courseId: String?,
        unitUrl: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.TRANSCRIPT_HIDDEN, currentTime)
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)
        }
        trackFullstoryEvent(Events.HIDE_TRANSCRIPT, fullstoryEvent.properties)
    }

    override fun trackVideoSpeed(
        videoId: String?,
        currentTime: Double?,
        courseId: String?,
        unitUrl: String?,
        oldSpeed: Float,
        newSpeed: Float
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.VIDEO_PLAYBACK_SPEED_CHANGED, currentTime)
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)
            properties[Keys.NEW_SPEED] = newSpeed
            properties[Keys.OLD_SPEED] = oldSpeed
        }
        trackFullstoryEvent(Events.SPEED_CHANGE_VIDEO, fullstoryEvent.properties)
    }

    override fun trackVideoLoading(
        videoId: String?,
        courseId: String?,
        unitUrl: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.VIDEO_LOADED)
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)
        }
        trackFullstoryEvent(Events.LOADED_VIDEO, fullstoryEvent.properties)
    }

    override fun trackVideoSeek(
        videoId: String?,
        oldTime: Double,
        newTime: Double,
        courseId: String?,
        unitUrl: String?,
        skipSeek: Boolean
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.VIDEO_SEEKED)
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)

            val oldTimeFormatted = JavaUtil.formatDoubleValue(oldTime, 3)
            val newTimeFormatted = JavaUtil.formatDoubleValue(newTime, 3)
            val skipInterval = JavaUtil.formatDoubleValue(newTimeFormatted - oldTimeFormatted, 3)

            properties[Keys.OLD_TIME] = oldTimeFormatted
            properties[Keys.NEW_TIME] = newTimeFormatted
            properties[Keys.SEEK_TYPE] = if (skipSeek) Values.SKIP else Values.SLIDE
            properties[Keys.REQUESTED_SKIP_INTERVAL] = skipInterval
        }
        trackFullstoryEvent(Events.SEEK_VIDEO, fullstoryEvent.properties)
    }

    override fun trackDownloadComplete(videoId: String?, courseId: String?, unitUrl: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.VIDEO_DOWNLOADED)
            properties.addCourseContext(courseId, unitUrl, Values.DOWNLOAD_MODULE)
        }
        trackFullstoryEvent(Events.VIDEO_DOWNLOADED, fullstoryEvent.properties)
    }

    override fun trackCourseUpgradeSuccess(
        blockId: String,
        courseId: String,
        minifiedBlockId: String
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.USER_COURSE_UPGRADE_SUCCESS
            properties[Keys.BLOCK_ID] = blockId
            properties[Keys.COURSE_ID] = courseId
            properties.addLabelAndCategory(label = courseId, category = Values.CONVERSION)
        }
        trackFullstoryEvent(Events.COURSE_UPGRADE_SUCCESS, fullstoryEvent.properties)
    }

    override fun trackBrowserLaunched(url: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.BROWSER_LAUNCHED
            url?.let {
                properties[Keys.TARGET_URL] = url
            }
        }
        trackFullstoryEvent(Events.BROWSER_LAUNCHED, fullstoryEvent.properties)
    }

    override fun trackSubSectionBulkVideoDownload(
        section: String?,
        subSection: String?,
        enrollmentId: String?,
        videoCount: Long
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.BULK_DOWNLOAD_SUBSECTION
            properties[Keys.NO_OF_VIDEOS] = videoCount
            if (section != null && subSection != null) {
                properties[Keys.COURSE_SECTION] = section
                properties[Keys.COURSE_SUBSECTION] = subSection
            }
            properties.addCourseContext(enrollmentId, null, Values.DOWNLOAD_MODULE)
        }
        trackFullstoryEvent(Events.BULK_DOWNLOAD_SUBSECTION, fullstoryEvent.properties)
    }

    override fun trackUserLogin(method: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.USERLOGIN
            method?.let {
                properties[Keys.METHOD] = method
            }
        }
        trackFullstoryEvent(Events.USER_LOGIN, fullstoryEvent.properties)
    }

    override fun trackUserLogout() {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.USERLOGOUT
        }
        trackFullstoryEvent(Events.USER_LOGOUT, fullstoryEvent.properties)
    }

    override fun trackTranscriptLanguage(
        videoId: String?,
        currentTime: Double?,
        lang: String?,
        courseId: String?,
        unitUrl: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.LANGUAGE] = lang
            properties.addVideoProperties(videoId, Values.TRANSCRIPT_LANGUAGE, currentTime)
            properties.addCourseContext(
                courseId, unitUrl, Values.VIDEOPLAYER
            )
        }
        trackFullstoryEvent(Events.LANGUAGE_CLICKED, fullstoryEvent.properties)
    }

    override fun trackSingleVideoDownload(
        videoId: String?,
        courseId: String?,
        unitUrl: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.SINGLE_VIDEO_DOWNLOAD)
            properties.addCourseContext(courseId, unitUrl, Values.DOWNLOAD_MODULE)
        }
        trackFullstoryEvent(Events.SINGLE_VIDEO_DOWNLOAD, fullstoryEvent.properties)
    }

    override fun trackVideoOrientation(
        videoId: String?,
        currentTime: Double?,
        isLandscape: Boolean,
        courseId: String?,
        unitUrl: String?,
        playMedium: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties.addVideoProperties(videoId, Values.FULLSREEN_TOGGLED, currentTime)
            properties[Keys.FULLSCREEN] = isLandscape
            properties.addCourseContext(courseId, unitUrl, Values.VIDEOPLAYER)
            if (playMedium.isNotNullOrEmpty()) {
                properties[Keys.PLAY_MEDIUM] = playMedium
            }
        }
        trackFullstoryEvent(Events.SCREEN_TOGGLED, fullstoryEvent.properties)
    }

    override fun trackCoursesSearch(
        searchQuery: String?,
        isLoggedIn: Boolean,
        versionName: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.DISCOVERY_COURSES_SEARCH
            properties[Keys.LABEL] = searchQuery
            properties[Keys.APP_VERSION] = versionName
            properties[Keys.SCREEN_NAME] =
                if (isLoggedIn) Values.DISCOVERY_COURSES_SEARCH_TAB
                else Values.DISCOVERY_COURSES_SEARCH_LANDING
        }
        trackFullstoryEvent(Events.DISCOVERY_COURSES_SEARCH, fullstoryEvent.properties)
    }

    override fun trackUserSignUpForAccount() {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.USER_NO_ACCOUNT
        }
        trackFullstoryEvent(Events.SIGN_UP, fullstoryEvent.properties)
    }

    override fun trackUserFindsCourses(enrolledCoursesCount: Int) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.USER_FIND_COURSES
            properties[Keys.ENROLLED_COURSES_COUNT] = enrolledCoursesCount
            properties.addLabelAndCategory(
                label = Values.COURSE_DISCOVERY,
                category = Values.USER_ENGAGEMENT
            )
        }
        trackFullstoryEvent(Events.FIND_COURSES, fullstoryEvent.properties)
    }

    override fun trackCreateAccountClicked(appVersion: String, source: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.CREATE_ACCOUNT_CLICKED
            if (source.isNotNullOrEmpty()) {
                properties[Keys.PROVIDER] = source
            }
            properties.addLabelAndCategory(label = appVersion, category = Values.CONVERSION)
        }
        trackFullstoryEvent(Events.CREATE_ACCOUNT_CLICKED, fullstoryEvent.properties)
    }

    override fun trackRegistrationSuccess(appVersion: String, source: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.USER_REGISTRATION_SUCCESS
            if (source.isNotNullOrEmpty()) {
                properties[Keys.PROVIDER] = source
            }
            properties.addLabelAndCategory(label = appVersion, category = Values.CONVERSION)
        }
        trackFullstoryEvent(Events.REGISTRATION_SUCCESS, fullstoryEvent.properties)
    }

    override fun trackEnrollClicked(courseId: String, emailOptIn: Boolean) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.USER_COURSE_ENROLL_CLICKED
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.EMAIL_OPT_IN] = emailOptIn
            properties.addLabelAndCategory(label = courseId, category = Values.CONVERSION)
        }
        trackFullstoryEvent(Events.COURSE_ENROLL_CLICKED, fullstoryEvent.properties)
    }

    override fun trackEnrolmentSuccess(courseId: String, emailOptIn: Boolean) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.USER_COURSE_ENROLL_SUCCESS
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.EMAIL_OPT_IN] = emailOptIn
            properties.addLabelAndCategory(label = courseId, category = Values.CONVERSION)
        }
        trackFullstoryEvent(Events.COURSE_ENROLL_SUCCESS, fullstoryEvent.properties)
    }

    override fun trackNotificationReceived(courseId: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.NOTIFICATION_RECEIVED
            properties.addLabelAndCategory(label = courseId, category = Values.PUSH_NOTIFICATION)
        }
        trackFullstoryEvent(Events.PUSH_NOTIFICATION_RECEIVED, fullstoryEvent.properties)
    }

    override fun trackNotificationTapped(courseId: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.NOTIFICATION_TAPPED
            properties.addLabelAndCategory(label = courseId, category = Values.PUSH_NOTIFICATION)
        }
        trackFullstoryEvent(Events.PUSH_NOTIFICATION_TAPPED, fullstoryEvent.properties)
    }

    override fun trackUserConnectionSpeed(connectionType: String?, connectionSpeed: Float) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.CONNECTION_SPEED
            properties[Keys.CONNECTION_TYPE] = connectionType
            properties[Keys.CONNECTION_SPEED] = connectionSpeed
        }
        trackFullstoryEvent(Events.SPEED, fullstoryEvent.properties)
    }

    override fun certificateShared(
        courseId: String?,
        certificateUrl: String?,
        method: ShareUtils.ShareType?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.SOCIAL_CERTIFICATE_SHARED
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.CATEGORY] = Values.SOCIAL_SHARING
            properties[Keys.URL] = certificateUrl
            method?.let { properties[Keys.TYPE] = AnalyticsUtils.getShareTypeValue(method) }
        }
        trackFullstoryEvent(Events.SOCIAL_CERTIFICATE_SHARED, fullstoryEvent.properties)
    }

    override fun courseDetailShared(
        courseId: String?,
        aboutUrl: String?,
        method: ShareUtils.ShareType?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = courseId
            properties[Keys.CATEGORY] = Values.SOCIAL_SHARING
            properties[Keys.URL] = aboutUrl
            method?.let { properties[Keys.TYPE] = AnalyticsUtils.getShareTypeValue(method) }
        }
        trackFullstoryEvent(Events.SOCIAL_COURSE_DETAIL_SHARED, fullstoryEvent.properties)
    }

    override fun trackCourseComponentViewed(
        blockId: String?,
        courseId: String?,
        minifiedBlockId: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.COMPONENT_VIEWED
            properties[Keys.BLOCK_ID] = blockId
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.MINIFIED_BLOCK_ID] = minifiedBlockId
            properties.addLabelAndCategory(
                label = Keys.COMPONENT_VIEWED,
                category = Values.NAVIGATION
            )
        }
        trackFullstoryEvent(Events.COMPONENT_VIEWED, fullstoryEvent.properties)
    }

    override fun trackOpenInBrowser(
        blockId: String?,
        courseId: String?,
        isSupported: Boolean,
        minifiedBlockId: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.OPEN_IN_BROWSER
            properties[Keys.BLOCK_ID] = blockId
            properties[Keys.MINIFIED_BLOCK_ID] = minifiedBlockId
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.SUPPORTED] = isSupported
            properties.addLabelAndCategory(
                label = if (isSupported) Values.OPEN_IN_WEB_SUPPORTED else Values.OPEN_IN_WEB_NOT_SUPPORTED,
                category = Values.NAVIGATION
            )
        }
        trackFullstoryEvent(Events.OPEN_IN_BROWSER, fullstoryEvent.properties)
    }

    override fun trackProfileViewed(username: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.PROFILE_VIEWED
            properties.addLabelAndCategory(label = username, category = Values.PROFILE)
        }
        trackFullstoryEvent(Events.PROFILE_VIEWED, fullstoryEvent.properties)
    }

    override fun trackProfilePhotoSet(fromCamera: Boolean) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.PROFILE_PHOTO_SET
            properties.addLabelAndCategory(
                label = Values.LIBRARY,
                category = if (fromCamera) Values.CAMERA else Values.LIBRARY
            )
        }
        trackFullstoryEvent(Events.PROFILE_PHOTO_SET, fullstoryEvent.properties)
    }

    override fun trackAppRatingDialogViewed(versionName: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.APP_REVIEWS_VIEW_RATING
            properties[Keys.CATEGORY] = Values.APP_REVIEWS_CATEGORY
            properties[Keys.APP_VERSION] = versionName
        }
        trackFullstoryEvent(Events.APP_REVIEWS_VIEW_RATING, fullstoryEvent.properties)
    }

    override fun trackAppRatingDialogCancelled(versionName: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.APP_REVIEWS_DISMISS_RATING
            properties[Keys.CATEGORY] = Values.APP_REVIEWS_CATEGORY
            properties[Keys.APP_VERSION] = versionName
        }
        trackFullstoryEvent(Events.APP_REVIEWS_DISMISS_RATING, fullstoryEvent.properties)
    }

    override fun trackUserSubmitRating(versionName: String?, rating: Int) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.APP_REVIEWS_SUBMIT_RATING
            properties[Keys.CATEGORY] = Values.APP_REVIEWS_CATEGORY
            properties[Keys.APP_VERSION] = versionName
            properties[Keys.RATING] = rating
        }
        trackFullstoryEvent(Events.APP_REVIEWS_SUBMIT_RATING, fullstoryEvent.properties)
    }

    override fun trackUserSendFeedback(versionName: String?, rating: Int) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.APP_REVIEWS_SEND_FEEDBACK
            properties[Keys.CATEGORY] = Values.APP_REVIEWS_CATEGORY
            properties[Keys.APP_VERSION] = versionName
            properties[Keys.RATING] = rating
        }
        trackFullstoryEvent(Events.APP_REVIEWS_SEND_FEEDBACK, fullstoryEvent.properties)
    }

    override fun trackUserMayReviewLater(versionName: String?, rating: Int) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.APP_REVIEWS_MAYBE_LATER
            properties[Keys.CATEGORY] = Values.APP_REVIEWS_CATEGORY
            properties[Keys.APP_VERSION] = versionName
            properties[Keys.RATING] = rating
        }
        trackFullstoryEvent(Events.APP_REVIEWS_MAYBE_LATER, fullstoryEvent.properties)
    }

    override fun trackRateTheAppClicked(versionName: String?, rating: Int) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.APP_REVIEWS_RATE_THE_APP
            properties[Keys.CATEGORY] = Values.APP_REVIEWS_CATEGORY
            properties[Keys.APP_VERSION] = versionName
            properties[Keys.RATING] = rating
        }
        trackFullstoryEvent(Events.APP_REVIEWS_RATE_THE_APP, fullstoryEvent.properties)
    }

    override fun trackWhatsNewClosed(
        versionName: String,
        totalViewed: Int,
        currentlyViewed: Int,
        totalScreens: Int
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.WHATS_NEW_CLOSE
            properties[Keys.CATEGORY] = Values.WHATS_NEW_CATEGORY
            properties[Keys.TOTAL_VIEWED] = totalViewed
            properties[Keys.CURRENTLY_VIEWED] = currentlyViewed
            properties[Keys.TOTAL_SCREENS] = totalScreens
            properties[Keys.APP_VERSION] = versionName
        }
        trackFullstoryEvent(Events.WHATS_NEW_CLOSE, fullstoryEvent.properties)
    }

    override fun trackWhatsNewSeen(versionName: String, totalScreens: Int) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.WHATS_NEW_DONE
            properties[Keys.CATEGORY] = Values.WHATS_NEW_CATEGORY
            properties[Keys.TOTAL_SCREENS] = totalScreens
            properties[Keys.APP_VERSION] = versionName
        }
        trackFullstoryEvent(Events.WHATS_NEW_DONE, fullstoryEvent.properties)
    }

    override fun trackSubsectionVideosDelete(courseId: String, subsectionId: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.VIDEOS_SUBSECTION_DELETE
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.SUBSECTION_ID] = subsectionId
        }
        trackFullstoryEvent(Events.VIDEOS_SUBSECTION_DELETE, fullstoryEvent.properties)
    }

    override fun trackUndoingSubsectionVideosDelete(courseId: String, subsectionId: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.VIDEOS_UNDO_SUBSECTION_DELETE
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.SUBSECTION_ID] = subsectionId
        }
        trackFullstoryEvent(Events.VIDEOS_UNDO_SUBSECTION_DELETE, fullstoryEvent.properties)
    }

    override fun trackUnitVideoDelete(courseId: String, unitId: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.VIDEOS_UNIT_DELETE
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.UNIT_ID] = unitId
        }
        trackFullstoryEvent(Events.VIDEOS_UNIT_DELETE, fullstoryEvent.properties)
    }

    override fun trackUndoingUnitVideoDelete(courseId: String, unitId: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.VIDEOS_UNDO_UNIT_DELETE
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.UNIT_ID] = unitId
        }
        trackFullstoryEvent(Events.VIDEOS_UNDO_UNIT_DELETE, fullstoryEvent.properties)
    }

    override fun trackBulkDownloadSwitchOn(
        courseId: String,
        totalDownloadableVideos: Int,
        remainingDownloadableVideos: Int
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.BULK_DOWNLOAD_SWITCH_ON
            properties[Keys.COMPONENT] = Values.DOWNLOAD_MODULE
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.TOTAL_DOWNLOADABLE_VIDEOS] = totalDownloadableVideos
            properties[Keys.REMAINING_DOWNLOADABLE_VIDEOS] = remainingDownloadableVideos
        }
        trackFullstoryEvent(Events.BULK_DOWNLOAD_TOGGLE_ON, fullstoryEvent.properties)
    }

    override fun trackBulkDownloadSwitchOff(courseId: String, totalDownloadableVideos: Int) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.BULK_DOWNLOAD_SWITCH_OFF
            properties[Keys.COMPONENT] = Values.DOWNLOAD_MODULE
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.TOTAL_DOWNLOADABLE_VIDEOS] = totalDownloadableVideos
        }
        trackFullstoryEvent(Events.BULK_DOWNLOAD_TOGGLE_OFF, fullstoryEvent.properties)
    }

    override fun trackExperimentParams(
        experimentName: String,
        values: MutableMap<String, String>?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            values?.let { properties.putAll(it) }
        }
        trackFullstoryEvent(experimentName, fullstoryEvent.properties)
    }

    override fun trackCastDeviceConnectionChanged(
        eventName: String,
        connectionState: String,
        playMedium: String
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = connectionState
            if (playMedium.isNotNullOrEmpty()) {
                properties[Keys.PLAY_MEDIUM] = playMedium
            }
        }
        trackFullstoryEvent(eventName, fullstoryEvent.properties)
    }

    override fun trackPLSCourseDatesBanner(
        biValue: String,
        courseId: String,
        enrollmentMode: String,
        screenName: String,
        bannerType: String
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = biValue
            properties[Keys.CATEGORY] = Values.COURSE_DATES
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.MODE] = enrollmentMode
            properties[Keys.SCREEN_NAME] = screenName
            properties[Keys.BANNER_TYPE] = bannerType
        }
        trackFullstoryEvent(Events.PLS_BANNER_VIEWED, fullstoryEvent.properties)
    }

    override fun trackPLSShiftButtonTapped(
        courseId: String,
        enrollmentMode: String,
        screenName: String
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.CATEGORY] = Values.COURSE_DATES
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.MODE] = enrollmentMode
            properties[Keys.SCREEN_NAME] = screenName
        }
        trackFullstoryEvent(Events.PLS_SHIFT_DATES_BUTTON_TAPPED, fullstoryEvent.properties)
    }

    override fun trackPLSCourseDatesShift(
        courseId: String,
        enrollmentMode: String,
        screenName: String,
        isSuccess: Boolean
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.CATEGORY] = Values.COURSE_DATES
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.MODE] = enrollmentMode
            properties[Keys.SCREEN_NAME] = screenName
            properties[Keys.SUCCESS] = isSuccess
        }
        trackFullstoryEvent(Events.PLS_SHIFT_DATES, fullstoryEvent.properties)
    }

    override fun trackValuePropMessageViewed(
        courseId: String,
        screenName: String,
        paymentEnabled: Boolean,
        experimentGroup: String?,
        componentId: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.VALUE_PROP_MESSAGE_VIEWED
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.SCREEN_NAME] = screenName
            properties[Keys.PAYMENT_ENABLED] = paymentEnabled
            if (experimentGroup.isNotNullOrEmpty()) {
                properties[Keys.IAP_EXPERIMENT_GROUP] = experimentGroup
            }
            if (componentId.isNotNullOrEmpty()) {
                properties[Keys.COMPONENT_ID] = componentId
            }
        }
        trackFullstoryEvent(Events.VALUE_PROP_MESSAGE_VIEWED, fullstoryEvent.properties)
    }

    override fun trackValuePropLearnMoreTapped(courseId: String, screenName: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.VALUE_PROP_LEARN_MORE_CLICKED
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.SCREEN_NAME] = screenName
        }
        trackFullstoryEvent(Events.VALUE_PROP_LEARN_MORE_CLICKED, fullstoryEvent.properties)
    }

    override fun trackLockedContentTapped(courseId: String, assignmentId: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.ASSIGNMENT_ID] = assignmentId
            properties[Keys.SCREEN_NAME] = Screens.COURSE_UNIT
        }
        trackFullstoryEvent(Events.COURSE_UNIT_LOCKED_CONTENT, fullstoryEvent.properties)
    }

    override fun trackValuePropShowMoreLessClicked(
        courseId: String,
        componentId: String?,
        price: String?,
        isSelfPaced: Boolean,
        showMore: Boolean
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = if (showMore) Values.VALUE_PROP_SHOW_MORE_CLICKED
            else Values.VALUE_PROP_SHOW_LESS_CLICKED
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.PACING] = if (isSelfPaced) Keys.SELF else Keys.INSTRUCTOR
            if (price.isNotNullOrEmpty()) {
                properties[Keys.PRICE] = price
            }
            if (componentId.isNotNullOrEmpty()) {
                properties[Keys.COMPONENT_ID] = componentId
            }
        }
        trackFullstoryEvent(
            if (showMore) Events.VALUE_PROP_SHOW_MORE_CLICKED else Events.VALUE_PROP_SHOW_LESS_CLICKED,
            fullstoryEvent.properties
        )
    }


    override fun trackExploreAllCoursesTapped(versionName: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.EXPLORE_ALL_COURSES
            properties[Keys.APP_VERSION] = versionName
            properties[Keys.SCREEN_NAME] = Values.DISCOVERY_COURSES_SEARCH_LANDING
            properties.addLabelAndCategory(
                label = Values.DISCOVERY,
                category = Values.USER_ENGAGEMENT
            )
        }
        trackFullstoryEvent(Events.EXPLORE_ALL_COURSES, fullstoryEvent.properties)
    }

    override fun trackDatesCourseComponentTapped(
        courseId: String,
        blockId: String,
        blockType: String,
        link: String
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.COURSE_DATES_COMPONENT_TAPPED
            properties[Keys.CATEGORY] = Values.COURSE_DATES
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.BLOCK_ID] = blockId
            properties[Keys.BLOCK_TYPE] = blockType
            properties[Keys.LINK] = link
        }
        trackFullstoryEvent(Events.DATES_COURSE_COMPONENT_TAPPED, fullstoryEvent.properties)
    }

    override fun trackUnsupportedComponentTapped(courseId: String, blockId: String, link: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.COURSE_DATES_UNSUPPORTED_COMPONENT_TAPPED
            properties[Keys.CATEGORY] = Values.COURSE_DATES
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.BLOCK_ID] = blockId
            properties[Keys.LINK] = link
        }
        trackFullstoryEvent(Events.DATES_UNSUPPORTED_COMPONENT_TAPPED, fullstoryEvent.properties)
    }

    override fun trackCourseSectionCelebration(courseId: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.COURSE_SECTION_COMPLETION_CELEBRATION
            properties[Keys.COURSE_ID] = courseId
        }
        trackFullstoryEvent(Events.COURSE_SECTION_COMPLETION_CELEBRATION, fullstoryEvent.properties)
    }

    override fun trackCourseCelebrationShareClicked(courseId: String, socialService: String?) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.COURSE_SECTION_CELEBRATION_SHARE_CLICKED
            properties[Keys.COURSE_ID] = courseId
            if (socialService.isNotNullOrEmpty()) {
                properties[Keys.SERVICE] = socialService
            }
        }
        trackFullstoryEvent(Events.CELEBRATION_SOCIAL_SHARE_CLICKED, fullstoryEvent.properties)
    }

    override fun trackResumeCourseBannerTapped(courseId: String, blockId: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.RESUME_COURSE_BANNER_TAPPED
            properties[Keys.CATEGORY] = Values.NAVIGATION
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.BLOCK_ID] = blockId
        }
        trackFullstoryEvent(Events.RESUME_COURSE_TAPPED, fullstoryEvent.properties)
    }

    override fun trackSubsectionViewOnWebTapped(
        courseId: String,
        subsectionId: String,
        isSpecialExamInfo: Boolean
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.SUBSECTION_VIEW_ON_WEB_TAPPED
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.SUBSECTION_ID] = subsectionId
            properties[Keys.SPECIAL_EXAM_INFO] = isSpecialExamInfo
        }
        trackFullstoryEvent(Events.SUBSECTION_VIEW_ON_WEB_TAPPED, fullstoryEvent.properties)
    }

    override fun trackCalendarEvent(
        eventName: String,
        biValue: String,
        courseId: String,
        userType: String,
        isSelfPaced: Boolean,
        elapsedTime: Long
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = biValue
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.USER_TYPE] = userType
            properties[Keys.PACING] = if (isSelfPaced) Keys.SELF else Keys.INSTRUCTOR
            if (elapsedTime > 0L) {
                properties[Keys.ELAPSED_TIME] = elapsedTime
            }
        }
        trackFullstoryEvent(eventName, fullstoryEvent.properties)
    }

    override fun trackOpenInBrowserBannerEvent(
        eventName: String,
        biValue: String,
        userType: String,
        courseId: String,
        componentId: String,
        componentType: String,
        openedUrl: String
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = biValue
            properties[Keys.USER_TYPE] = userType
            properties[Keys.COURSE_ID] = courseId
            properties[Keys.COMPONENT_ID] = componentId
            properties[Keys.COMPONENT_TYPE] = componentType
            properties[Keys.OPENED_URL] = openedUrl
        }
        trackFullstoryEvent(eventName, fullstoryEvent.properties)
    }

    override fun trackScreenViewEvent(eventName: String, screenName: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.SCREEN_NAVIGATION
            properties[Keys.SCREEN_NAME] = screenName
        }
        trackFullstoryEvent(Events.PROFILE_PAGE_VIEWED, fullstoryEvent.properties)
    }

    override fun trackVideoDownloadQualityChanged(
        selectedVideoQuality: VideoQuality,
        oldVideoQuality: VideoQuality
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.VIDEO_DOWNLOAD_QUALITY_CHANGED
            properties[Keys.VALUE] = selectedVideoQuality.value
            properties[Keys.OLD_VALUE] = oldVideoQuality.value
        }
        trackFullstoryEvent(Events.VIDEO_DOWNLOAD_QUALITY_CHANGED, fullstoryEvent.properties)
    }

    override fun trackOpenInBrowserAlertTriggerEvent(url: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.DISCOVERY_OPEN_IN_BROWSER_ALERT_TRIGGERED
            properties[Keys.CATEGORY] = Values.DISCOVERY
            properties[Keys.URL] = url
        }
        trackFullstoryEvent(
            Events.DISCOVERY_OPEN_IN_BROWSER_ALERT_TRIGGERED,
            fullstoryEvent.properties
        )
    }

    override fun trackOpenInBrowserAlertActionTaken(url: String, actionTaken: String) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = Values.DISCOVERY_OPEN_IN_BROWSER_ALERT_ACTION_TAKEN
            properties[Keys.CATEGORY] = Values.DISCOVERY
            properties[Keys.URL] = url
            properties[Keys.ALERT_ACTION] = actionTaken
        }
        trackFullstoryEvent(
            Events.DISCOVERY_OPEN_IN_BROWSER_ALERT_ACTION_TAKEN,
            fullstoryEvent.properties
        )
    }

    override fun trackInAppPurchasesEvent(
        eventName: String,
        biValue: String,
        courseId: String?,
        isSelfPaced: Boolean,
        flowType: String?,
        price: String?,
        componentId: String?,
        elapsedTime: Long,
        error: String?,
        actionTaken: String?,
        screenName: String?
    ) {
        val fullstoryEvent = FullstoryEvent().apply {
            properties[Keys.NAME] = biValue
            properties[Keys.CATEGORY] = Values.IN_APP_PURCHASES
            if (courseId.isNotNullOrEmpty()) {
                properties[Keys.COURSE_ID] = courseId
                properties[Keys.PACING] = if (isSelfPaced) Keys.SELF else Keys.INSTRUCTOR
            }
            if (flowType.isNotNullOrEmpty()) {
                properties[Keys.IAP_FLOW_TYPE] = flowType
            }
            if (price.isNotNullOrEmpty()) {
                properties[Keys.PRICE] = price
            }
            if (componentId.isNotNullOrEmpty()) {
                properties[Keys.COMPONENT_ID] = componentId
            }
            if (elapsedTime > 0L) {
                properties[Keys.ELAPSED_TIME] = elapsedTime
            }
            if (error.isNotNullOrEmpty()) {
                properties[Keys.ERROR] = error
            }
            if (actionTaken.isNotNullOrEmpty()) {
                val isErrorAlert = biValue.equals(Values.IAP_ERROR_ALERT_ACTION, ignoreCase = true)
                properties[if (isErrorAlert) Keys.ERROR_ACTION else Keys.ACTION] = actionTaken
            }
            if (screenName.isNotNullOrEmpty()) {
                properties[Keys.SCREEN_NAME] = screenName
            }
        }
        trackFullstoryEvent(eventName, fullstoryEvent.properties)
    }

    companion object {
        private const val TAG = "FullstoryAnalytics"
        const val CACHE_DIRECTORY_NAME = "fullstory"
    }
}
