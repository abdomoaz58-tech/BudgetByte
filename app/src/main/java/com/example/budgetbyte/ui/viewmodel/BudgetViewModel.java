package com.example.budgetbyte.ui.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.budgetbyte.data.entity.CategoryStat;
import com.example.budgetbyte.data.repository.ExpenseRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BudgetViewModel extends AndroidViewModel {
    private static final String PREFS_NAME = "budgetbyte_budget_limits";
    private final ExpenseRepository repository;
    private final MutableLiveData<String> activeMonth = new MutableLiveData<>();
    private final LiveData<List<CategoryStat>> categories;
    private final LiveData<Long> monthlySpending;

    public BudgetViewModel(@NonNull Application app) {
        super(app);
        repository = new ExpenseRepository(app);
        activeMonth.setValue(defaultMonthKey());
        categories = Transformations.switchMap(activeMonth, repository::getCategoryStats);
        monthlySpending = Transformations.switchMap(activeMonth, repository::getMonthlyExpenses);
    }

    public LiveData<List<CategoryStat>> categories() {
        return categories;
    }

    public LiveData<Long> monthlySpending() {
        return monthlySpending;
    }

    public void loadForMonth(String yearMonth) {
        String monthKey = normalizeMonthKey(yearMonth);
        if (!monthKey.equals(activeMonth.getValue())) {
            activeMonth.setValue(monthKey);
        }
    }

    public long getBudgetForMonth(String yearMonth) {
        return getPreferences().getLong(normalizeMonthKey(yearMonth), 0L);
    }

    public void setBudgetForMonth(String yearMonth, long amount) {
        long safeAmount = Math.max(0L, amount);
        getPreferences().edit().putLong(normalizeMonthKey(yearMonth), safeAmount).apply();
    }

    public static String normalizeMonthKey(String yearMonth) {
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            return defaultMonthKey();
        }
        String normalized = yearMonth.trim();
        return normalized.length() == 7 && normalized.charAt(4) == '-' ? normalized : defaultMonthKey();
    }

    private static String defaultMonthKey() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    }

    private SharedPreferences getPreferences() {
        return getApplication().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    protected void onCleared() {
        repository.close();
    }
}
