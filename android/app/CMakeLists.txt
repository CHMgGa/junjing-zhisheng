cmake_minimum_required(VERSION 3.4.1)

#set(distribution_DIR ${CMAKE_SOURCE_DIR}/libs)            # 文件夹为libs的情况下
set(distribution_DIR ${CMAKE_SOURCE_DIR}/src/main/jniLibs) # 文件夹为jniLibs的情况下
message("distribution_DIR:" ${distribution_DIR})

add_library( libavcodec-57
        SHARED
        IMPORTED )

add_library( libavdevice-57
        SHARED
        IMPORTED )

add_library( libavfilter-6
        SHARED
        IMPORTED )

add_library( libavformat-57
        SHARED
        IMPORTED )

add_library( libavutil-55
        SHARED
        IMPORTED )

add_library( libpostproc-54
        SHARED
        IMPORTED )

add_library( libswresample-2
        SHARED
        IMPORTED )

add_library( libswscale-4
        SHARED
        IMPORTED )
set_target_properties( libavcodec-57
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavcodec-57.so)

set_target_properties( libavdevice-57
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavdevice-57.so)

set_target_properties( libavfilter-6
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavfilter-6.so)

set_target_properties( libavformat-57
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavformat-57.so)

set_target_properties( libavutil-55
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libavutil-55.so)

set_target_properties( libpostproc-54
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libpostproc-54.so)

set_target_properties( libswresample-2
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libswresample-2.so)

set_target_properties( libswscale-4
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libswscale-4.so)

add_library(
        native-lib
        SHARED
        ${CMAKE_SOURCE_DIR}/src/main/cpp/native-lib.c) #如果是C++则为native-lib.cpp

find_library(
        log-lib
        log)

include_directories(src/main/cpp/include)

target_link_libraries( # Specifies the target library.
        native-lib
       # 先链接avutil（基础库），再链接其他FFmpeg库
        libavutil-55       
        libswresample-2
        libavcodec-57
        libavformat-57
        libavfilter-6
        libavdevice-57
        libswscale-4
        libpostproc-54
        android        
        ${log-lib} )
