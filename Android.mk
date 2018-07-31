LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_DEX_PREOPT := false
LOCAL_PACKAGE_NAME := BrahmaWallet

LOCAL_MODULE_TAGS := optional

LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res \
                      frameworks/support/v7/appcompat/res \
                      frameworks/support/v7/cardview/res \
                      frameworks/support/v7/recyclerview/res \
                      frameworks/support/design/res \

#aar res need add packagename here too
LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages android.support.design \
    --extra-packages android.support.transition \
    --extra-packages android.support.v7.appcompat \
    --extra-packages android.support.v7.cardview \
    --extra-packages android.support.v7.recyclerview \
    --extra-packages com.bumptech.glide \
    --extra-packages butterknife \
    --extra-packages com.orhanobut.logger \
    --extra-packages rx.android \
    --extra-packages me.yokeyword.indexablerecyclerview \
    --extra-packages android.arch.lifecycle.extensions \
    --extra-packages android.arch.lifecycle.livedata \
    --extra-packages android.arch.lifecycle.viewmodel \
    --extra-packages android.arch.paging.runtime \
    --extra-packages android.arch.persistence.room \
    --extra-packages android.support.constraint \
    --extra-packages android.arch.lifecycle.livedata.core \
    --extra-packages android.arch.persistence.db \
    --extra-packages android.arch.persistence.db.framework \
    --extra-packages android.support.annotation

#for jars
LOCAL_STATIC_JAVA_LIBRARIES := \
    android-common \
    android-support-design \
    android-support-transition \
    android-support-v13 \
    android-support-v7-appcompat \
    android-support-v7-cardview \
    android-support-v7-recyclerview \
    android-support-v7-palette \
    android-support-v4 \
    junit \
    bitcoinj \
    butterknife-annotations \
    butterknife-compiler \
    glide-compile \
    zxing-core \
    lambda-scrypt \
    okhttp-logging \
    retrofit2-adapter \
    retrofit2-converter \
    retrofit \
    web3j-core \
    reactivex-rxjava \
    jackson \
    lifecycle-compiler \
    room-compiler \
    room-common \
    jackson-databind \
    jackson-core \
    spongycastle-core \
    glide-annotations \
    okhttp-3 \
    okio \
    room-migration


LOCAL_STATIC_JAVA_LIBRARIES += android-support-annotations

#for aars
LOCAL_STATIC_JAVA_AAR_LIBRARIES := glide-aar butterknife indexablerecyclerview orhanobut-logger \
                                   reactivex-rxandroid constraint-layout lifecycle-extensions \
                                   lifecycle-livedata lifecycle-viewmodel paging-runtime room-runtime \
                                   lifecycle-livedata-core persistence-db persistence-db-framework

APP_ALLOW_MISSING_DEPS :=true

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := bitcoinj:libs/bitcoinj-core-0.14.7.jar\
                                        butterknife:libs/butterknife-8.5.1.aar \
                                        butterknife-annotations:libs/butterknife-annotations-8.5.1.jar \
                                        butterknife-compiler:libs/butterknife-compiler-8.5.1.jar \
                                        glide-aar:libs/glide-4.7.1.aar \
                                        glide-compile:libs/glide-compiler-4.7.1.jar \
                                        lambda-scrypt:libs/lambda-scrypt-1.4.0.jar \
                                        okhttp-logging:libs/okhttp-logging-interceptor-3.8.1.jar \
                                        orhanobut-logger:libs/orhanobut-logger-1.15.aar \
                                        reactivex-rxandroid:libs/reactivex-rxandroid-1.2.1.aar \
                                        retrofit2-adapter:libs/retrofit2-adapter-rxjava-2.1.0.jar \
                                        retrofit2-converter:libs/retrofit2-converter-jackson-2.3.0.jar \
                                        retrofit:libs/retrofit-2.4.0.jar \
                                        web3j-core:libs/web3j-core-android-2.2.1.jar \
                                        indexablerecyclerview:libs/yokeyword-indexablerecyclerview-1.3.0.aar \
                                        zxing-core:libs/zxing-core-3.3.2.jar \
                                        constraint-layout:libs/constraint-layout-1.1.2.aar \
                                        reactivex-rxjava:libs/reactivex-rxjava-1.2.2.jar \
                                        jackson:libs/jackson-annotations-2.8.0.jar \
                                        lifecycle-compiler:libs/lifecycle-compiler-1.1.1.jar \
                                        lifecycle-extensions:libs/lifecycle-extensions-1.1.1.aar \
                                        lifecycle-livedata:libs/lifecycle-livedata-1.1.1.aar \
                                        lifecycle-viewmodel:libs/lifecycle-viewmodel-1.1.1.aar \
                                        paging-runtime:libs/paging-runtime-1.0.0-beta1.aar \
                                        room-compiler:libs/room-compiler-1.0.0.jar \
                                        room-runtime:libs/room-runtime-1.0.0.aar \
                                        room-common:libs/room-common-1.0.0.jar \
                                        lifecycle-livedata-core:libs/lifecycle-livedata-core-1.1.1.aar \
                                        jackson-databind:libs/jackson-databind-2.8.5.jar \
                                        jackson-core:libs/jackson-core-2.8.5.jar \
                                        spongycastle-core:libs/spongycastle-core-1.54.0.0.jar \
                                        persistence-db:libs/persistence-db-1.0.0.aar \
                                        glide-annotations:libs/glide-annotations-4.7.1.jar \
                                        okhttp-3:libs/okhttp-3.10.0.jar \
                                        okio:libs/okio-1.14.0.jar \
                                        persistence-db-framework:libs/persistence-db-framework-1.0.0.aar \
                                        room-migration:libs/room-migration-1.0.0.jar

include $(BUILD_MULTI_PREBUILT)
