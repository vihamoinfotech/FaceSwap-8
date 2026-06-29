package com.facechanger.faceswap.enhance.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Helper utility to persist and dynamically update application-wide locale.
 * Supports LTR/RTL layout switching automatically.
 */
public final class AppFaceLocaleHelper {

    private static final String PREF_NAME = "language_prefs";
    private static final String KEY_LANGUAGE = "selected_language";

    private AppFaceLocaleHelper() {
        // No instances
    }

    /**
     * Called from attachBaseContext in Application and Activities to wrap
     * context with the saved locale resources.
     */
    public static Context onAttach(@NonNull Context context) {
        String lang = getSavedLanguage(context);
        return setLocale(context, lang);
    }

    /**
     * Gets the saved language code, or auto-detects from the system if none is saved.
     */
    @NonNull
    public static String getSavedLanguage(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_LANGUAGE, null);
        if (saved == null) {
            // Auto detect from system language
            String systemLang = Locale.getDefault().getLanguage();
            if (isSupportedLanguage(systemLang)) {
                return systemLang;
            }
            return "en"; // Default to English
        }
        return saved;
    }

    /**
     * Checks if a language code is in our supported list.
     */
    public static boolean isSupportedLanguage(String lang) {
        if (lang == null) return false;
        return lang.equals("en") || lang.equals("in") || lang.equals("id") ||
                lang.equals("es") || lang.equals("ar") || lang.equals("fr") ||
                lang.equals("pt") || lang.equals("nl");
    }

    /**
     * Saves the language and updates resource configuration context.
     */
    public static Context setLocale(@NonNull Context context, @NonNull String language) {
        // Persist language code (use standard "in" for Indonesian)
        String langToPersist = language.equals("id") ? "in" : language;
        persistLanguage(context, langToPersist);

        Locale locale = new Locale(langToPersist);
        Locale.setDefault(locale);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResources(context, locale);
        }
        return updateResourcesLegacy(context, locale);
    }

    private static void persistLanguage(@NonNull Context context, @NonNull String language) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(@NonNull Context context, @NonNull Locale locale) {
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLayoutDirection(locale);
        
        // Also update Application Context configuration directly to prevent process-wide cache bugs
        Context appContext = context.getApplicationContext();
        if (appContext != null && appContext != context) {
            appContext.getResources().updateConfiguration(configuration, appContext.getResources().getDisplayMetrics());
        }
        
        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(@NonNull Context context, @NonNull Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale);
        }
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }
}
