// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    // Thêm plugin Gradle của dịch vụ Google (bản 4.4.2 hoặc 4.5.0)
    id("com.google.gms.google-services") version "4.4.2" apply false
}