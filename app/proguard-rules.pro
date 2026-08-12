-keep class com.fuckbaiduinput.HookEntry { *; }
-keep class com.fuckbaiduinput.MainActivity { *; }
-keep class com.fuckbaiduinput.HookSettingsApplication { *; }
-keep class com.fuckbaiduinput.HookSettingsProvider { *; }
-keep class com.fuckbaiduinput.HookSettingsContract { *; }
-keep class com.fuckbaiduinput.TargetIdentityVerifier { *; }
-keep enum com.fuckbaiduinput.HookFeature { *; }
-keep class com.fuckbaiduinput.FeatureSnapshot { *; }

-keep class io.github.libxposed.service.SecureXposedProvider { *; }
-keep class io.github.libxposed.service.XposedServiceHelper { *; }

-dontwarn io.github.libxposed.**
-dontwarn io.github.libxposed.annotation.**
-dontwarn androidx.annotation.**
