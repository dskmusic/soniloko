# Add project specific ProGuard rules here.

# NewPipeExtractor pulls in Mozilla Rhino (used to evaluate YouTube's player JS for signature
# decryption). Rhino has optional integration with desktop-only java.beans/javax.script classes
# that don't exist on Android and are never actually invoked at runtime — R8 in strict mode
# treats the missing references as a hard build error instead of a warning, so silence them.
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn org.mozilla.javascript.tools.**
