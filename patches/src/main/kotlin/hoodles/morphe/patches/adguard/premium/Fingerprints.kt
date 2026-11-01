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

/* ---------- Lifetime / Duration lookup, from strict to permissive ---------- */

// Original target (mobile AdGuard). Kept first so nothing changes for existing builds.
object LifetimeDurationFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Lifetime")
)

// TV build may capitalise the display string differently.
object LifetimeDurationUpperFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("LIFETIME")
)

object LifetimeDurationSpacedFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Life time")
)

// Last resort: any method in a class that contains the "Lifetime" string.
// Ambiguity is handled in EnablePremiumPatch by picking the field name.
object LifetimeDurationFallbackFingerprint : Fingerprint(
    strings = listOf("Lifetime")
)