package com.example.budgetbyte.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.budgetbyte.data.entity.CategoryStat;
import com.example.budgetbyte.data.repository.ExpenseRepository;
import com.example.budgetbyte.ui.stats.StatsAnalysisUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsViewModel extends AndroidViewModel {
    private final ExpenseRepository repository;
    private final MutableLiveData<String> activeMonth = new MutableLiveData<>();
    private final LiveData<Long> currentMonthSpending;
    private final LiveData<List<CategoryStat>> categoryBreakdown;
    private final MutableLiveData<List<CategoryStat>> monthlyTotals = new MutableLiveData<>(Collections.emptyList());

    public StatsViewModel(@NonNull Application app) {
        super(app);
        repository = new ExpenseRepository(app);
        activeMonth.setValue(defaultMonthKey());
        currentMonthSpending = Transformations.switchMap(activeMonth, repository::getMonthlyExpenses);
        categoryBreakdown = Transformations.switchMap(activeMonth, repository::getCategoryStats);
    }

    public LiveData<Long> currentMonthSpending() {
        return currentMonthSpending;
    }

    public LiveData<List<CategoryStat>> categoryBreakdown() {
        return categoryBreakdown;
    }

    public LiveData<List<CategoryStat>> monthlyTotals() {
        return monthlyTotals;
    }

    public void loadCurrentMonthSpending() {
        loadForMonth(defaultMonthKey());
    }

    public void loadCategoryBreakdown() {
        loadForMonth(defaultMonthKey());
    }

    public void loadMonthlyTotals() {
        loadForMonth(defaultMonthKey());
    }

    public void loadForMonth(String yearMonth) {
        String monthKey = normalizeMonthKey(yearMonth);
        if (!monthKey.equals(activeMonth.getValue())) {
            activeMonth.setValue(monthKey);
        }

        repository.getMonthlyTotals("expense", 6, new ExpenseRepository.ResultCallback<List<CategoryStat>>() {
            @Override
            public void onResult(List<CategoryStat> result) {
                monthlyTotals.postValue(StatsAnalysisUtils.limitLastSix(result));
            }

            @Override
            public void onError(Exception exception) {
                monthlyTotals.postValue(Collections.emptyList());
            }
        });
    }

    private String normalizeMonthKey(String yearMonth) {
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            return defaultMonthKey();
        }
        String normalized = yearMonth.trim();
        return normalized.length() == 7 && normalized.charAt(4) == '-' ? normalized : defaultMonthKey();
    }

    private String defaultMonthKey() {
        return new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
    }

    public void exportReport(android.content.Context context, String yearMonth, ExpenseRepository.ResultCallback<java.io.File> callback) {
        String monthKey = normalizeMonthKey(yearMonth);
        repository.getExpensesByMonth(monthKey, new ExpenseRepository.ResultCallback<List<com.example.budgetbyte.data.entity.Expense>>() {
            @Override
            public void onResult(List<com.example.budgetbyte.data.entity.Expense> expenses) {
                if (expenses == null || expenses.isEmpty()) {
                    callback.onResult(null);
                    return;
                }
                try {
                    java.io.File file = com.example.budgetbyte.util.PdfExporter.exportMonthlyReport(context, monthKey, expenses);
                    callback.onResult(file);
                } catch (java.io.IOException e) {
                    callback.onError(e);
                }
            }

            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }

    @Override
    protected void onCleared() {
        repository.close();
    }
}
