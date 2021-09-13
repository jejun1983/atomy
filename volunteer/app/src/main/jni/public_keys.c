#include <jni.h>

JNIEXPORT jstring JNICALL
 Java_com_idevel_volunteer_onestore_AppSecurity_getPublicKey(JNIEnv *env, jobject instance)
 {
 return (*env)->NewStringUTF(env, "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCcW/cCfNG7SYFGR1EDOZhtLq4dYH3qBgUVjdYP/jMdvlaYm7wzH8QJxUp+iSXrt0BeQkdruYsz/OMdmO2AhWxYrF11lu30XhvgzHMubrtQI58i7n5x2SUJ8rquBoP6BLGEjFS1Gx/5AnB7UI2ZAFa+eJxYnQgAySCvmEJgSp83wwIDAQAB");
}

