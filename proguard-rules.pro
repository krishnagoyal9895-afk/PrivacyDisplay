# Privacy Display ProGuard rules

# Keep overlay service
-keep class com.privacy.display.PrivacyOverlayService { *; }
-keep class com.privacy.display.PrivacyOverlayView { *; }
-keep class com.privacy.display.PrivacyTileService { *; }
-keep class com.privacy.display.BootReceiver { *; }
-keep class com.privacy.display.MainActivity { *; }

# AndroidX
-keep class androidx.** { *; }
