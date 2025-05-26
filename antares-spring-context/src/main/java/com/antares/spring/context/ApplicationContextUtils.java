package com.antares.spring.context;

import java.util.Objects;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ApplicationContextUtils {
    private static ApplicationContext applicationContext = null;

    static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    @Nullable
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Nonnull
    public static ApplicationContext getRequiredApplicationContext() {
        return Objects.requireNonNull(getApplicationContext(), "ApplicationContext is not set.");
    }
}
