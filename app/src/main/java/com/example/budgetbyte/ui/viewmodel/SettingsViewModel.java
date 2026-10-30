package com.example.budgetbyte.ui.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.budgetbyte.util.SecurityHelper;

public class SettingsViewModel extends AndroidViewModel {
    private static final String BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String THEME_MODE = "theme_mode";
    private static final String PREFS_NAME = "budgetbyte_settings";

    private final MutableLiveData<Boolean> biometricEnabled;
    private final MutableLiveData<Integer> themeMode;

    public SettingsViewModel(@NonNull Application app) {
        super(app);
        biometricEnabled = new MutableLiveData<>(SecurityHelper.getBoolean(app, BIOMETRIC_ENABLED, false));
        themeMode = new MutableLiveData<>(getSavedThemeMode(app));
    }

    public LiveData<Boolean> biometricEnabled() {
        return biometricEnabled;
    }

    public LiveData<Integer> themeMode() {
        return themeMode;
    }

    public void setBiometricEnabled(boolean enabled) {
        SecurityHelper.putBoolean(getApplication(), BIOMETRIC_ENABLED, enabled);
        biometricEnabled.setValue(enabled);
    }

    public void setThemeMode(int mode) {
        int normalized = mode == AppCompatDelegate.MODE_NIGHT_NO
                || mode == AppCompatDelegate.MODE_NIGHT_YES
                || mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ? mode : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

        AppCompatDelegate.setDefaultNightMode(normalized);
        setSavedThemeMode(getApplication(), normalized);
        themeMode.setValue(normalized);
    }

    public static int getSavedThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int mode = prefs.getInt(THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        return mode == AppCompatDelegate.MODE_NIGHT_NO
                || mode == AppCompatDelegate.MODE_NIGHT_YES
                || mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ? mode : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public static void setSavedThemeMode(Context context, int mode) {
        int normalized = mode == AppCompatDelegate.MODE_NIGHT_NO
                || mode == AppCompatDelegate.MODE_NIGHT_YES
                || mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ? mode : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(THEME_MODE, normalized)
                .apply();
    }
}
