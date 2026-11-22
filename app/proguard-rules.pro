# ProGuard rules for Service For Ever App

# Preserve Firebase Data Models for Firestore Serialization
-keepclassmembers class com.srr.myapplication.model.** {
    public <fields>;
    public <methods>;
}
-keep class com.srr.myapplication.model.** { *; }

# Preserve JavaMail classes for SMTP Email sending
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class javax.activation.** { *; }

# Line numbers for error log reporting
-keepattributes SourceFile,LineNumberTable
