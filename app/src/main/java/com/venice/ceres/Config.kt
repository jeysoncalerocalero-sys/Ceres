package com.venice.ceres

data class FeatureState(
    val name: String,
    val desc: String,
    val enabled: Boolean
)

object Config {

    fun allMax() = listOf(
        FeatureState("No Recoil", "Retroceso a cero absoluto", true),
        FeatureState("No Spread", "Dispersion a cero, bala recta", true),
        FeatureState("Aim Assist MAX", "Iman de apuntado al 100%", true),
        FeatureState("Auto Headshot", "Empuja el impacto a la cabeza", true),
        FeatureState("Headshot Lock", "Fija el hueso cabeza como objetivo", true),
        FeatureState("Aim Lock descarado", "Bloqueo instantaneo del objetivo", true),
        FeatureState("Hitbox XL", "Caja de impacto ampliada", true),
        FeatureState("FPS Boost", "Sombras y anti-aliasing apagados", true)
    )

    fun buildIni(features: List<FeatureState>): String {
        fun on(name: String) =
            if (features.firstOrNull { it.name == name }?.enabled == true) 1 else 0

        return buildString {
            appendLine("; ================= CERES =================")
            appendLine("; Todo al maximo. Backup automatico: UserCustom.ini.bak")
            appendLine()
            appendLine("[UserCustom DeviceProfile]")
            appendLine()
            appendLine("; --- Retroceso y dispersion ---")
            appendLine("+CVars=r.NoRecoil=${on("No Recoil")}")
            appendLine("+CVars=r.RecoilScale=0.0")
            appendLine("+CVars=r.SpreadScale=0.0")
            appendLine()
            appendLine("; --- Cabeza ---")
            appendLine("+CVars=r.AutoHeadshot=${on("Auto Headshot")}")
            appendLine("+CVars=r.HeadshotScale=1.0")
            appendLine("+CVars=r.HeadshotLock=${on("Headshot Lock")}")
            appendLine("+CVars=r.AimLockBone=Head")
            appendLine()
            appendLine("; --- Apuntado ---")
            appendLine("+CVars=r.AimAssist=${on("Aim Assist MAX")}")
            appendLine("+CVars=r.AimAssistStrength=100")
            appendLine("+CVars=r.AimAssistRange=100")
            appendLine("+CVars=r.AimLock=${on("Aim Lock descarado")}")
            appendLine("+CVars=r.AimLockSmooth=0.05")
            appendLine("+CVars=r.AimLockFov=100")
            appendLine("+CVars=r.AimLockSpeed=100")
            appendLine()
            appendLine("; --- Extras ---")
            appendLine("+CVars=r.HitboxScale=${if (on("Hitbox XL") == 1) 1.25 else 1.0}")
            appendLine("+CVars=r.Shadows=${if (on("FPS Boost") == 1) 0 else 1}")
            appendLine("+CVars=r.AntiAliasing=0")
            appendLine("+CVars=r.MobileContentScaleFactor=1.0")
            appendLine("+CVars=r.FPS=90")
        }
    }
}
