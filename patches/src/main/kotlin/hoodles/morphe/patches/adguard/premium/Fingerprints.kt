/**
 * Copyright 2026 Hoo-dles
 * https://github.com/hoo-dles/morphe-patches
 */

package hoodles.morphe.patches.adguard.premium

import app.morphe.patcher.Fingerprint

object GetPlusStateFingerprint : Fingerprint(
    classFingerprint = Fingerprint(type = "Lj0/j;"),
    name = "a",
    parameters = listOf(),
    returnType = "Lj0/i;"
)

object FetchPlusStateFingerprint : Fingerprint(
    classFingerprint = Fingerprint(type = "Lj0/b;"),
    name = "o",
    parameters = listOf("Li0/k;", "Ljava/lang/String;"),
    returnType = "Lj0/i;"
)

object PaidLicenseFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        strings = listOf("PaidLicense(licenseKey=")
    ),
    name = "<init>"
)

object LifetimeDurationFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Lifetime")
)
