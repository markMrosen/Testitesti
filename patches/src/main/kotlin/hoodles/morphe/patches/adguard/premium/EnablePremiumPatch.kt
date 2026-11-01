/**
 * Copyright 2026 Hoo-dles
 * https://github.com/hoo-dles/morphe-patches
 */

package hoodles.morphe.patches.adguard.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import hoodles.morphe.compatibility.Compat

val enablePremiumPatch = bytecodePatch(
    name = "Enable Premium",
    description = "Enables app features locked behind the subscription paywall."
) {
    compatibleWith(Compat.ADGUARD)

    execute {
        val licenseTypeClass = PaidLicenseFingerprint.method.parameters[1].type

        // Walk through the fingerprints from strict to permissive.
        val lifetimeDurationClass = runCatching { LifetimeDurationFingerprint.classDef }
            .recoverCatching { LifetimeDurationUpperFingerprint.classDef }
            .recoverCatching { LifetimeDurationSpacedFingerprint.classDef }
            .recoverCatching { LifetimeDurationFallbackFingerprint.classDef }
            .getOrElse { throwable ->
                error(
                    "Could not locate the Lifetime duration enum in this APK. " +
                        "Tried: toString+['Lifetime'], toString+['LIFETIME'], " +
                        "toString+['Life time'], any+['Lifetime']. " +
                        "Original cause: ${throwable.message}"
                )
            }

        // Do NOT blindly take staticFields.first() — that's what broke on TV.
        // Prefer a field literally called "Lifetime"; fall back to whatever is first.
        val lifetimeDurationInstance = lifetimeDurationClass.staticFields
            .firstOrNull { it.name.equals("Lifetime", ignoreCase = true) }
            ?: lifetimeDurationClass.staticFields.firstOrNull { !it.name.startsWith("$") }
            ?: lifetimeDurationClass.staticFields.firstOrNull()
            ?: error(
                "Class ${lifetimeDurationClass.type} was located, but it has no " +
                    "static fields to use as the Lifetime enum entry. " +
                    "Please report this on the Hoodles issue tracker."
            )

        val getPaidLicenseMethod = ImmutableMethod(
            GetPlusStateFingerprint.classDef.type,
            "getPaidLicense",
            null,
            GetPlusStateFingerprint.method.returnType,
            AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(7)
        ).toMutable().apply {
            addInstructions(0, """
                new-instance v0, ${PaidLicenseFingerprint.classDef.type}
                const-string v1, ""
                sget-object v2, $licenseTypeClass->Personal:$licenseTypeClass
                sget-object v3, $lifetimeDurationInstance
                const/4 v4, 0x1
                const/4 v5, 0x3
                const-string v6, ""
                invoke-direct/range {v0 .. v6}, ${PaidLicenseFingerprint.method}
                return-object v0
            """.trimIndent())
        }

        GetPlusStateFingerprint.classDef.methods.add(getPaidLicenseMethod)

        val patchInstructions = """
            invoke-static {}, $getPaidLicenseMethod
            move-result-object v0
            return-object v0
        """.trimIndent()

        GetPlusStateFingerprint.method.addInstructions(0, patchInstructions)
        FetchPlusStateFingerprint.method.addInstructions(0, patchInstructions)
    }
}