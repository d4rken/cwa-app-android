package de.rki.coronawarnapp.coronatest.qrcode

import android.os.Parcelable
import de.rki.coronawarnapp.coronatest.TestRegistrationRequest
import de.rki.coronawarnapp.coronatest.type.BaseCoronaTest
import de.rki.coronawarnapp.qrcode.scanner.QrCode
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.joda.time.Instant
import org.joda.time.LocalDate

sealed class CoronaTestQRCode : Parcelable, TestRegistrationRequest, QrCode {

    abstract override val type: BaseCoronaTest.Type
    abstract val registrationIdentifier: String
    abstract val rawQrCode: String
    abstract val categoryType: CategoryType

    @Parcelize
    data class PCR(
        val qrCodeGUID: CoronaTestGUID,
        override val isDccConsentGiven: Boolean = false,
        override val dateOfBirth: LocalDate? = null,
        override val rawQrCode: String,
        override val categoryType: CategoryType = CategoryType.NOT_SELECTED
    ) : CoronaTestQRCode() {

        @IgnoredOnParcel
        override val isDccSupportedByPoc: Boolean = true

        @IgnoredOnParcel
        override val type: BaseCoronaTest.Type = BaseCoronaTest.Type.PCR

        @IgnoredOnParcel
        override val identifier: String
            get() = "qrcode-${type.raw}-$qrCodeGUID"

        @IgnoredOnParcel
        override val registrationIdentifier: String
            get() = qrCodeGUID
    }

    abstract class Rapid : CoronaTestQRCode() {
        abstract val hash: RapidHash
        abstract val createdAt: Instant
        abstract val firstName: String?
        abstract val lastName: String?
        abstract val testId: String?
        abstract val salt: String?

        override val identifier: String
            get() = "qrcode-${type.raw}-$hash"

        override val registrationIdentifier: String
            // We hash in the VerificationServer.retrieveRegistrationToken which was needed anyway for PCR
            get() = hash
    }

    @Parcelize
    data class RapidAntigen(
        override val dateOfBirth: LocalDate? = null,
        override val isDccConsentGiven: Boolean = false,
        override val isDccSupportedByPoc: Boolean = false,
        override val rawQrCode: String,
        override val hash: RapidAntigenHash,
        override val createdAt: Instant,
        override val firstName: String? = null,
        override val lastName: String? = null,
        override val testId: String? = null,
        override val salt: String? = null,
        override val categoryType: CategoryType = CategoryType.NOT_SELECTED
    ) : Rapid() {
        @IgnoredOnParcel
        override val type: BaseCoronaTest.Type = BaseCoronaTest.Type.RAPID_ANTIGEN
    }

    @Parcelize
    data class RapidPCR(
        override val dateOfBirth: LocalDate? = null,
        override val isDccConsentGiven: Boolean = false,
        override val isDccSupportedByPoc: Boolean = false,
        override val rawQrCode: String,
        override val hash: RapidPCRHash,
        override val createdAt: Instant,
        override val firstName: String? = null,
        override val lastName: String? = null,
        override val testId: String? = null,
        override val salt: String? = null,
        override val categoryType: CategoryType = CategoryType.NOT_SELECTED
    ) : Rapid() {
        @IgnoredOnParcel
        override val type: BaseCoronaTest.Type = BaseCoronaTest.Type.PCR
    }

    enum class CategoryType {
        FAMILY,
        PERSONAL,
        NOT_SELECTED,
    }
}

typealias CoronaTestGUID = String
typealias RapidHash = String
typealias RapidAntigenHash = String
typealias RapidPCRHash = String
