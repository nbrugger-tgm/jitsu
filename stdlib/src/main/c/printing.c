#include "jitsu.h"
#include <stdio.h>

void print_i8(signed char value) {
    printf("%d", value);
}

void print_i16(signed int value) {
    printf("%d", value);
}

void print_i32(signed long value) {
    printf("%ld", value);
}

void print_i64(signed long long value) {
    printf("%lld", value);
}

void print_u8(unsigned char value){
    printf("%u", value);
}

void print_u16(unsigned int value) {
    printf("%u", value);
}

void print_u32(unsigned long value) {
    printf("%lu", value);
}

void print_u64(unsigned long long value) {
    printf("%llu", value);
}

void println() {
    printf("\n");
}