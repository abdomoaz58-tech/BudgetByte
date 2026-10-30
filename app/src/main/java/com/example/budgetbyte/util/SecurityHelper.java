package com.example.budgetbyte.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/** Accesses app settings without silently falling back to plaintext preferences. */
public final class SecurityHelper {
    private static final String PREFERENCES_NAME = "budgetbyte_secure_preferences";
    private static volatile SharedPreferences cachedPreferences;

    private SecurityHelper() { }

    public static SharedPreferences getSecurePreferences(Context context) {
        if (cachedPreferences == null) {
            synchronized (SecurityHelper.class) {
                if (cachedPreferences == null) {
                    try {
                        MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .build();
                        cachedPreferences = EncryptedSharedPreferences.create(
                                context.getApplicationContext(),
                                PREFERENCES_NAME,
                                masterKey,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                    } catch (GeneralSecurityException | IOException exception) {
                        throw new IllegalStateException("Unable to open encrypted preferences", exception);
                    }
                }
            }
        }
        return cachedPreferences;
    }

    public static void putBoolean(Context context, String key, boolean value) {
        getSecurePreferences(context).edit().putBoolean(key, value).apply();
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getSecurePreferences(context).getBoolean(key, defaultValue);
    }
}
