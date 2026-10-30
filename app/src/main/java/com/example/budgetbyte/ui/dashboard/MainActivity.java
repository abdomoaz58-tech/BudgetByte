package com.example.budgetbyte.ui.dashboard;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.budgetbyte.R;
import com.example.budgetbyte.databinding.ActivityMainBinding;
import com.example.budgetbyte.ui.viewmodel.SettingsViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);

        AppCompatDelegate.setDefaultNightMode(SettingsViewModel.getSavedThemeMode(this));

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final int initialBottomNavPadding = binding.bottomNavigation.getPaddingBottom();
        final int initialNavHostPaddingTop = binding.navHost.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());

            binding.navHost.setPaddingRelative(
                    statusBars.left,
                    statusBars.top + initialNavHostPaddingTop,
                    statusBars.right,
                    0);

            binding.bottomNavigation.setPaddingRelative(
                    binding.bottomNavigation.getPaddingStart(),
                    binding.bottomNavigation.getPaddingTop(),
                    binding.bottomNavigation.getPaddingEnd(),
                    navBars.bottom + initialBottomNavPadding);

            return insets;
        });

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.navHost);
        if (host == null) {
            throw new IllegalStateException("Navigation host missing");
        }

        NavController controller = host.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, controller);

        controller.addOnDestinationChangedListener((navController, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.addExpenseFragment || id == R.id.ocrScannerFragment) {
                binding.bottomNavigation.setVisibility(android.view.View.GONE);
            } else {
                binding.bottomNavigation.setVisibility(android.view.View.VISIBLE);
            }
        });
    }
}
