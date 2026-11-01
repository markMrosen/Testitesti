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
        val ctorParams = PaidLicenseFingerprint.method.parameters
        require(ctorParams.size >= 3) {
            "PaidLicense constructor has only ${ctorParams.size} parameters. " +
                "The patch assumes ≥ 3 (licenseKey, licenseType, duration|boolean). " +
                "Please report this on the Hoodles issue tracker."
        }

        val licenseTypeClass = ctorParams[1].type
        val durationType     = ctorParams[2].type

        // Build the argument for the "duration / lifetime" slot.
        //   boolean (Z)          → const/4 v3, 0x1     (true)
        //   class type (L…;)      → sget-object v3, <enum-constant>
        val durationArg: String = if (durationType == "Z") {
            // 4.7.179+ shape: lifetime is a boolean flag, no enum to fetch.
            "const/4 v3, 0x1"
        } else {
            // Older shape: static enum constant.
            val durationClass = runCatching { LifetimeDurationFingerprint.classDef }
                .recoverCatching { LifetimeDurationLowerFingerprint.classDef }
                .getOrElse { t ->
                    error(
                        "Could not locate the Duration class. Tried " +
                            "toString+['Lifetime'] and any+[', lifetime=']. " +
                            "Original cause: ${t.message}"
                    )
                }

            val instance = durationClass.staticFields
                .firstOrNull { it.name.equals("Lifetime", true) }
                ?: durationClass.staticFields.first()

            "sget-object v3, $instance"
        }

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
                $durationArg
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