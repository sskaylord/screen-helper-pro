cmake_minimum_required(VERSION 3.22)
project(display_utils LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fvisibility=hidden -fno-rtti -fno-exceptions")

add_library(display_utils SHARED
    env_check.cpp
    sys_compat.cpp
    asset_meta_resolve.cpp
    cache_manager.cpp
    draw_utils.cpp
    native_bridge.cpp
)

find_library(LOG_LIB log)
find_library(GL_LIB GLESv1_CM)
find_library(ANDROID_LIB android)

target_link_libraries(display_utils
    ${LOG_LIB}
    ${GL_LIB}
    ${ANDROID_LIB}
)

target_include_directories(display_utils PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
