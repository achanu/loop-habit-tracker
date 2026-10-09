plugins {
    // Built-in Kotlin in AGP 9 falls back to its embedded KGP (2.2.10) unless a
    // newer one is declared here; this keeps the app module on Kotlin 2.4.20.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.agp) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint.plugin) apply false
    alias(libs.plugins.shadow) apply false
}

apply {
    from("gradle/translators.gradle.kts")
}
