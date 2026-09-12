cmake_minimum_required(VERSION 3.22)
project(renderkit_support LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fvisibility=hidden -fno-rtti -fno-exceptions")

add_library(render_support SHARED
    compat_guard.cpp
    rt_patch_entry.cpp
    asset_meta_resolve.cpp
    cache_manager.cpp
    ui_overlay_helper.cpp
)

find_library(LOG_LIB log)
find_library(GL_LIB GLESv2)
find_library(ANDROID_LIB android)

target_link_libraries(render_support
    ${LOG_LIB}
    ${GL_LIB}
    ${ANDROID_LIB}
)

target_include_directories(render_support PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
