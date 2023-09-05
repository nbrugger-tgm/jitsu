package com.niton.parser.internal;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class Lazy<T> {
    private T value;
    private final Supplier<T> supplier;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }
    public Lazy(T constant) {
        this.value = constant;
        this.supplier = null;
    }

    @NotNull
    public T get() {
        if (value == null && supplier != null) {
            value = supplier.get();
        }
        return value;
    }
}
