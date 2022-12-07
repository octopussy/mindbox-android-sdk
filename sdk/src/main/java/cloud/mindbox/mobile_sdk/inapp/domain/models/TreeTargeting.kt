package cloud.mindbox.mobile_sdk.inapp.domain.models

import cloud.mindbox.mobile_sdk.inapp.data.InAppGeoRepositoryImpl
import cloud.mindbox.mobile_sdk.inapp.data.InAppRepositoryImpl
import cloud.mindbox.mobile_sdk.inapp.domain.InAppGeoRepository
import cloud.mindbox.mobile_sdk.inapp.domain.InAppRepository
import kotlinx.coroutines.flow.collect
import org.koin.java.KoinJavaComponent.inject

internal interface ITargeting {
    fun getCustomerIsInTargeting(csiaList: List<CustomerSegmentationInApp>): Boolean

    fun preCheckTargeting(): SegmentationCheckResult
}

internal enum class Kind {
    POSITIVE,
    NEGATIVE
}

internal sealed class TreeTargeting(open val type: String) : ITargeting {

    internal data class TrueNode(override val type: String) : TreeTargeting(type) {
        override fun getCustomerIsInTargeting(csiaList: List<CustomerSegmentationInApp>): Boolean {
            return true
        }

        override fun preCheckTargeting(): SegmentationCheckResult {
            return SegmentationCheckResult.IMMEDIATE
        }
    }

    internal data class CountryNode(
        override val type: String,
        val kind: Kind,
        val ids: List<String>,
        val countryId: String,
    ) : TreeTargeting(type) {
        private val inAppGeoRepositoryImpl: InAppGeoRepository by inject(InAppGeoRepositoryImpl::class.java)

        override fun getCustomerIsInTargeting(csiaList: List<CustomerSegmentationInApp>): Boolean {
            return if (kind == Kind.POSITIVE) ids.contains(countryId) else ids.contains(countryId)
                .not()
        }

        override fun preCheckTargeting(): SegmentationCheckResult {
            return SegmentationCheckResult.PENDING
        }
    }

    internal data class CityNode(
        override val type: String,
        val kind: Kind,
        val ids: List<String>,
        val cityId: String,
    ) : TreeTargeting(type) {
        override fun getCustomerIsInTargeting(csiaList: List<CustomerSegmentationInApp>): Boolean {
            return if (kind == Kind.POSITIVE) ids.contains(cityId) else ids.contains(cityId)
                .not()
        }

        override fun preCheckTargeting(): SegmentationCheckResult {
            return SegmentationCheckResult.PENDING
        }
    }

    internal data class RegionNode(
        override val type: String,
        val kind: Kind,
        val ids: List<String>,
        val regionId: String,
    ) : TreeTargeting(type) {
        override fun getCustomerIsInTargeting(csiaList: List<CustomerSegmentationInApp>): Boolean {
            return if (kind == Kind.POSITIVE) ids.contains(regionId) else ids.contains(regionId)
                .not()
        }

        override fun preCheckTargeting(): SegmentationCheckResult {
            return SegmentationCheckResult.PENDING
        }
    }

    internal data class IntersectionNode(
        override val type: String,
        val nodes: List<TreeTargeting>,
    ) : TreeTargeting(type) {
        override fun getCustomerIsInTargeting(csiaList: List<CustomerSegmentationInApp>): Boolean {
            var rez = true
            for (node in nodes) {
                for (csia in csiaList) {
                    if ((node is SegmentNode)) {
                        if (node.shouldProcessSegmentation(csia.segmentation?.ids?.externalId) && node.getCustomerIsInTargeting(
                                csiaList).not()
                        ) {
                            rez = false
                        }
                    } else {
                        if (node.getCustomerIsInTargeting(csiaList).not()) {
                            rez = false
                        }
                    }
                }
            }
            return rez
        }

        override fun preCheckTargeting(): SegmentationCheckResult {
            var rez: SegmentationCheckResult = SegmentationCheckResult.IMMEDIATE
            for (node in nodes) {
                if (node.preCheckTargeting() == SegmentationCheckResult.PENDING) {
                    rez = SegmentationCheckResult.PENDING
                }
            }
            return rez
        }
    }

    internal data class UnionNode(
        override val type: String,
        val nodes: List<TreeTargeting>,
    ) : TreeTargeting(type) {
        override fun getCustomerIsInTargeting(csiaList: List<CustomerSegmentationInApp>): Boolean {
            var rez = false
            for (node in nodes) {
                for (csia in csiaList) {
                    if (node is SegmentNode) {
                        if (node.shouldProcessSegmentation(csia.segmentation?.ids?.externalId) && node.getCustomerIsInTargeting(
                                csiaList)
                        ) {
                            rez = true
                        }
                    } else {
                        if (node.getCustomerIsInTargeting(csiaList)) {
                            rez = true
                        }
                    }
                }

            }
            return rez
        }

        override fun preCheckTargeting(): SegmentationCheckResult {
            var rez: SegmentationCheckResult = SegmentationCheckResult.IMMEDIATE
            for (node in nodes) {
                if (node.preCheckTargeting() == SegmentationCheckResult.PENDING) {
                    rez = SegmentationCheckResult.PENDING
                }
            }
            return rez
        }
    }

    internal data class SegmentNode(
        override val type: String,
        val kind: Kind,
        val segmentationExternalId: String,
        val segment_external_id: String,
    ) : TreeTargeting(type) {
        override fun getCustomerIsInTargeting(csiaList: List<CustomerSegmentationInApp>): Boolean {
            return when (kind) {
                Kind.POSITIVE -> csiaList.find { csia -> csia.segmentation?.ids?.externalId == segmentationExternalId }?.segment?.ids?.externalId == segment_external_id
                Kind.NEGATIVE -> csiaList.find { csia -> csia.segmentation?.ids?.externalId == segmentationExternalId }?.segment?.ids?.externalId == null
            }
        }

        fun shouldProcessSegmentation(segmentation: String?): Boolean {
            return (segmentation == segmentationExternalId)
        }

        override fun preCheckTargeting(): SegmentationCheckResult {
            return SegmentationCheckResult.PENDING
        }
    }
}

enum class SegmentationCheckResult {
    IMMEDIATE,
    PENDING
}