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
        require(ctorParams.size == 5) {
            "Expected PaidLicense.<init> with 5 params " +
                "(String, LicenseType, Duration, int, int). " +
                "Found ${ctorParams.size}. This patch targets AdGuard 4.7.179+."
        }

        val licenseClass  = PaidLicenseFingerprint.classDef.type   // Lj0/i$k;
        val licenseType   = ctorParams[1].type                     // Lj0/i$j;
        val durationType  = ctorParams[2].type                     // Lj0/i$i;

        // Duration is a sealed interface on 4.7.179:
        //   Lj0/i$i$a  = Lifetime             (singleton, INSTANCE field `a`)
        //   Lj0/i$i$b  = Unknown              (singleton, INSTANCE field `a`)
        //   Lj0/i$i$c  = WithExpirationDate(Date)
        // R8 orders member classes alphabetically by original source name,
        // so `$a` = Lifetime, `$b` = Unknown, `$c` = WithExpirationDate.
        //
        // On legacy builds (≤ 4.3.x) Duration is an enum and this branch
        // never runs (LifetimeDurationFingerprint matches first).
        val durationArg: String = runCatching {
            val cls = LifetimeDurationFingerprint.classDef
            val field = cls.staticFields.firstOrNull {
                it.name.equals("Lifetime", ignoreCase = true)
            } ?: error("Legacy Duration enum has no Lifetime field.")
            "sget-object v3, $field"
        }.getOrElse {
            val durationBare = durationType.removePrefix("L").removeSuffix(";")  // j0/i$i
            val lifetimeType = "L${durationBare}\$a;"                            // Lj0/i$i$a;
            "sget-object v3, $lifetimeType->a:$lifetimeType"
        }

        val ctorRef =
            "$licenseClass-><init>(Ljava/lang/String;$licenseType$durationType" +
                "II)V"

        val getPaidLicense = ImmutableMethod(
            GetPlusStateFingerprint.classDef.type,
            "getPaidLicense",
            null,
            GetPlusStateFingerprint.method.returnType,
            AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(6)
        ).toMutable().apply {
            addInstructions(0, """
                new-instance v0, $licenseClass
                const-string v1, ""
                sget-object v2, $licenseType->Personal:$licenseType
                $durationArg
                const/4 v4, 0x1
                const/4 v5, 0x3
                invoke-direct/range {v0 .. v5}, $ctorRef
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
