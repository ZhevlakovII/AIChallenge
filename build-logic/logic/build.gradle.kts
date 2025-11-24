plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(libs.kotlin.gradle)
    implementation(libs.ksp.gradle)
    implementation(libs.android.gradle)
    implementation(libs.compose.gradle)

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
