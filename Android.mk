LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := BrahmaWallet
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
LOCAL_MODULE_TAGS := optional

#LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)

LOCAL_PROGUARD_FLAG_FILES := ../../../frameworks/support/design/proguard-rules.pro

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
    --extra-packages com.bumptech.glide.gifdecoder \
    --extra-packages butterknife \
    --extra-packages com.orhanobut.logger \
    --extra-packages rx.android \
    --extra-packages me.yokeyword.indexablerecyclerview \
    --extra-packages android.arch.lifecycle.extensions \
    --extra-packages android.arch.lifecycle.livedata \
    --extra-packages android.arch.lifecycle.viewmodel \
    --extra-packages android.arch.lifecycle.runtime \
    --extra-packages android.arch.paging.runtime \
    --extra-packages android.arch.persistence.room \
    --extra-packages android.arch.lifecycle.livedata.core \
    --extra-packages android.arch.persistence.db \
    --extra-packages android.arch.persistence.db.framework \
    --extra-packages com.hwangjr.rxbus \
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
        bitcoinj-core \
        glide-compile \
        glide-disklrucache \
        zxing-core \
	    lambda-scrypt \
        okhttp-logging \
        retrofit2-adapter \
        retrofit2-converter \
        retrofit \
        guava-18 \
        web3j-core \
	    web3j-crypto \
	    web3j-abi \
	    web3j-rlp \
	    web3j-tuples \
	    web3j-utils \
        reactivex-rxjava \
        jackson \
        lifecycle-compiler \
        room-compiler \
        room-common \
        jackson-databind \
        jackson-core \
        spongycastle-core \
        spongycastle-prov \
        glide-annotations \
        constraint-layout-solver \
        okhttp-3 \
        okio \
        httpclient \
        lifecycle-common \
        room-migration \
	    slf4j-jdk14 \
	    protobuf-java

LOCAL_STATIC_JAVA_LIBRARIES += android-support-annotations

#for aars
LOCAL_STATIC_JAVA_AAR_LIBRARIES := glide-aar rxbus indexablerecyclerview orhanobut-logger \
                                   glide-gifdecoder reactivex-rxandroid constraint-layout lifecycle-extensions \
                                   lifecycle-livedata lifecycle-viewmodel paging-runtime room-runtime \
                                   lifecycle-runtime lifecycle-livedata-core persistence-db persistence-db-framework

APP_ALLOW_MISSING_DEPS :=true

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := glide-aar:libs/glide-4.7.1.aar \
                                        glide-compile:libs/glide-compiler-4.7.1.jar \
                                        glide-gifdecoder:libs/glide-gifdecoder-4.7.1.aar \
                                        glide-disklrucache:libs/glide-disklrucache-4.7.1.jar \
                                        orhanobut-logger:libs/orhanobut-logger-1.15.aar \
                                        indexablerecyclerview:libs/yokeyword-indexablerecyclerview-1.3.0.aar \
                                        zxing-core:libs/zxing-core-3.3.2.jar \
                                        lifecycle-compiler:libs/lifecycle-compiler-1.1.1.jar \
                                        lifecycle-extensions:libs/lifecycle-extensions-1.1.1.aar \
                                        lifecycle-livedata:libs/lifecycle-livedata-1.1.1.aar \
                                        lifecycle-viewmodel:libs/lifecycle-viewmodel-1.1.1.aar \
                                        lifecycle-runtime:libs/lifecycle-runtime-1.1.1.aar \
                                        lifecycle-common:libs/lifecycle-common-1.1.1.jar \
                                        paging-runtime:libs/paging-runtime-1.0.0-beta1.aar \
                                        room-compiler:libs/room-compiler-1.0.0.jar \
                                        room-runtime:libs/room-runtime-1.0.0.aar \
                                        room-common:libs/room-common-1.0.0.jar \
                                        lifecycle-livedata-core:libs/lifecycle-livedata-core-1.1.1.aar \
                                        persistence-db:libs/persistence-db-1.0.0.aar \
                                        glide-annotations:libs/glide-annotations-4.7.1.jar \
                                        rxbus:libs/rxbus-1.0.6.aar \
                                        httpclient:libs/httpclient-4.4.1.2.jar \
                                        persistence-db-framework:libs/persistence-db-framework-1.0.0.aar \
                                        room-migration:libs/room-migration-1.0.0.jar

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_MULTI_PREBUILT)
