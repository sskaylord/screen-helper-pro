#pragma once
#include <cstdint>
#include <cstddef>

namespace obf {
    constexpr uint8_t KEY = 0xA7;

    template<std::size_t N>
    struct ObfStr {
        char data[N];
        constexpr ObfStr(const char (&s)[N]) {
            for (std::size_t i = 0; i < N; i++)
                data[i] = s[i] ^ KEY;
        }
        const char* dec() const {
            static thread_local char buf[256];
            for (std::size_t i = 0; i < N; i++)
                buf[i] = data[i] ^ KEY;
            return buf;
        }
    };
}

#define OBF(s) (obf::ObfStr<sizeof(s)>(s).dec())
