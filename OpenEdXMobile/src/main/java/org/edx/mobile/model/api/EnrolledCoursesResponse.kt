package org.edx.mobile.model.api

import androidx.recyclerview.widget.DiffUtil
import com.google.gson.annotations.SerializedName
import org.edx.mobile.interfaces.SectionItemInterface
import org.edx.mobile.model.course.EnrollmentMode
import org.edx.mobile.util.DateUtil
import java.io.Serializable
import java.util.Date

data class EnrolledCoursesResponse(

    @SerializedName("mode")
    var mode: String,

    @SerializedName("audit_access_expires")
    val auditAccessExpires: String?,

    @SerializedName("is_active")
    val isActive: Boolean = false,

    @SerializedName("course")
    val course: CourseEntry,

    @SerializedName("certificate")
    private val certificate: CertificateModel?,

    @SerializedName("course_modes")
    private val courseModes: List<CourseMode>?,
) : SectionItemInterface {

    var isDiscussionBlackedOut: Boolean = false

    val courseId: String
        get() = course.id

    val certificateURL: String?
        get() = certificate?.certificateURL

    val isCertificateEarned: Boolean
        get() = certificateURL.isNullOrEmpty().not()

    val isAuditMode: Boolean
        get() = EnrollmentMode.AUDIT.toString().equals(mode, ignoreCase = true)

    val isAuditAccessExpired: Boolean
        get() = auditAccessExpires.isNullOrEmpty().not() &&
                Date().after(DateUtil.convertToDate(auditAccessExpires))

    val isUpgradeable: Boolean
        get() = isAuditMode &&
                course.isStarted &&
                course.isUpgradeDeadlinePassed.not() &&
                courseModes?.find {
                    EnrollmentMode.VERIFIED.toString().equals(it.slug, ignoreCase = true)
                } != null

    val productInfo: ProductInfo?
        get() = courseSku?.let { courseSku ->
            storeSku?.let { storeSku ->
                ProductInfo(courseSku, storeSku, lmsUSDPrice)
            }
        }

    private val courseSku: String?
        get() = courseModes?.firstOrNull { item ->
            EnrollmentMode.VERIFIED.name.equals(item.slug, ignoreCase = true)
        }?.androidSku.takeUnless { it.isNullOrEmpty() }

    private val storeSku: String?
        get() = courseModes?.firstOrNull { item ->
            EnrollmentMode.VERIFIED.name.equals(item.slug, ignoreCase = true)
        }?.storeSku

    private val lmsUSDPrice: Double
        get() = courseModes?.firstOrNull { item ->
            EnrollmentMode.VERIFIED.name.equals(item.slug, ignoreCase = true)
        }?.price ?: 0.0

    fun setStoreSku(storeProductPrefix: String) {
        courseModes?.forEach {
            it.setStoreProductSku(storeProductPrefix)
        }
    }

    override fun isChapter(): Boolean {
        return false
    }

    override fun isSection(): Boolean {
        return false
    }

    override fun toString(): String {
        return course.name
    }

    override fun isCourse(): Boolean {
        return true
    }

    override fun isVideo(): Boolean {
        return false
    }

    override fun isDownload(): Boolean {
        return false
    }

    data class ProductInfo(
        val courseSku: String,
        val storeSku: String,
        val lmsUSDPrice: Double = 0.0,
    ) : Serializable
}

/**
 * The callback for calculating the difference between two non-null items in a list.
 */
object EnrolledCoursesComparator : DiffUtil.ItemCallback<EnrolledCoursesResponse>() {
    /**
     * To check whether two objects represent the same item
     */
    override fun areItemsTheSame(
        oldItem: EnrolledCoursesResponse,
        newItem: EnrolledCoursesResponse,
    ): Boolean {
        return oldItem == newItem
    }

    /**
     * To check whether two items have the same data. With a RecyclerView.Adapter, we should return
     * whether the items' visual representations are the same.
     */
    override fun areContentsTheSame(
        oldItem: EnrolledCoursesResponse,
        newItem: EnrolledCoursesResponse,
    ): Boolean {
        return oldItem.courseId == newItem.courseId
    }
}
