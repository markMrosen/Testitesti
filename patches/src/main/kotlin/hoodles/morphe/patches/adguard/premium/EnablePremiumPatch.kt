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
        val ctorParams   = PaidLicenseFingerprint.method.parameters
        val licenseType  = ctorParams[1].type
        val durationType = ctorParams[2].type

        // 4.7.179+ : 3rd param is boolean lifetime  -> const/4 v3, 0x1
        // ≤ 4.3.x  : 3rd param is Duration enum     -> sget-object v3, Duration.Lifetime
        val durationArg: String = if (durationType == "Z") {
            "const/4 v3, 0x1"
        } else {
            val cls = LifetimeDurationFingerprint.classDef
            val field = cls.staticFields.firstOrNull {
                it.name.equals("Lifetime", ignoreCase = true)
            } ?: error(
                "Located Duration class ${cls.type}, but no `Lifetime` static field. " +
                    "Please report this on the Hoodles issue tracker."
            )
            "sget-object v3, $field"
        }

        val getPaidLicense = ImmutableMethod(
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
                sget-object v2, $licenseType->Personal:$licenseType
                $durationArg
                const/4 v4, 0x1
                const/4 v5, 0x3
                const-string v6, ""
                invoke-direct/range {v0 .. v6}, ${PaidLicenseFingerprint.method}
                return-object v0
            """.trimIndent())
        }

        GetPlusStateFingerprint.classDef.methods.add(getPaidLicense)

        val stub = """
            invoke-static {}, $getPaidLicense
            move-result-object v0
            return-object v0
        """.trimIndent()

        GetPlusStateFingerprint.method.addInstructions(0, stub)
        FetchPlusStateFingerprint.method.addInstructions(0, stub)
    }
}