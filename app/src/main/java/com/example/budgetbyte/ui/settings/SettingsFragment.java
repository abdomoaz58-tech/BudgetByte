package com.example.budgetbyte.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.budgetbyte.databinding.FragmentSettingsBinding;
import com.example.budgetbyte.ui.viewmodel.SettingsViewModel;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SettingsViewModel model = new ViewModelProvider(this).get(SettingsViewModel.class);
        model.biometricEnabled().observe(getViewLifecycleOwner(),
                enabled -> binding.switchBiometric.setChecked(Boolean.TRUE.equals(enabled)));
        model.themeMode().observe(getViewLifecycleOwner(), this::applyThemeSelection);

        binding.switchBiometric.setOnCheckedChangeListener((button, checked) -> model.setBiometricEnabled(checked));
        binding.btnThemeLight.setOnClickListener(v -> model.setThemeMode(AppCompatDelegate.MODE_NIGHT_NO));
        binding.btnThemeDark.setOnClickListener(v -> model.setThemeMode(AppCompatDelegate.MODE_NIGHT_YES));
        binding.btnThemeSystem.setOnClickListener(v -> model.setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));

        binding.btnNotifications.setEnabled(false);
        binding.btnEditProfile.setEnabled(false);
        binding.layoutCurrency.setEnabled(false);
        binding.layoutCurrency.setAlpha(0.6f);
    }

    private void applyThemeSelection(Integer mode) {
        int selectedMode = mode == null ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : mode;
        binding.btnThemeLight.setSelected(selectedMode == AppCompatDelegate.MODE_NIGHT_NO);
        binding.btnThemeDark.setSelected(selectedMode == AppCompatDelegate.MODE_NIGHT_YES);
        binding.btnThemeSystem.setSelected(selectedMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int activeColor = androidx.core.content.ContextCompat.getColor(requireContext(), com.example.budgetbyte.R.color.primary);
        int inactiveColor = androidx.core.content.ContextCompat.getColor(requireContext(), com.example.budgetbyte.R.color.surface_container_low);
        int activeTextColor = androidx.core.content.ContextCompat.getColor(requireContext(), com.example.budgetbyte.R.color.on_primary);
        int inactiveTextColor = androidx.core.content.ContextCompat.getColor(requireContext(), com.example.budgetbyte.R.color.on_surface_variant);

        binding.btnThemeLight.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selectedMode == AppCompatDelegate.MODE_NIGHT_NO ? activeColor : inactiveColor));
        binding.btnThemeDark.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selectedMode == AppCompatDelegate.MODE_NIGHT_YES ? activeColor : inactiveColor));
        binding.btnThemeSystem.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                selectedMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ? activeColor : inactiveColor));
        binding.btnThemeLight.setTextColor(selectedMode == AppCompatDelegate.MODE_NIGHT_NO ? activeTextColor : inactiveTextColor);
        binding.btnThemeDark.setTextColor(selectedMode == AppCompatDelegate.MODE_NIGHT_YES ? activeTextColor : inactiveTextColor);
        binding.btnThemeSystem.setTextColor(selectedMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ? activeTextColor : inactiveTextColor);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
