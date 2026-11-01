/**
 * Copyright 2026 Hoo-dles
 * https://github.com/hoo-dles/morphe-patches
 */

package hoodles.morphe.patches.adguard.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import com.android.tools.smali.dexlib2.Opcode

object GetPlusStateFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        strings = listOf("Failed to get state from backend. Remaining retry count: ")
    ),
    parameters = listOf(),
    returnType = "L",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ
    )
)

object FetchPlusStateFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        strings = listOf("Failed to get state from backend. Remaining retry count: ")
    ),
    parameters = listOf("L", "L"),
    returnType = "L",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INSTANCE_OF,
        Opcode.INVOKE_VIRTUAL
    )
)

object PaidLicenseFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        strings = listOf("PaidLicense(licenseKey=")
    ),
    name = "<init>"
)

/**
 * Only used by pre-4.7 builds where the 3rd ctor param is a Duration enum.
 * Never evaluated on 4.7.179 (where the 3rd param is boolean `Z`).
 */
object LifetimeDurationFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Lifetime")
)