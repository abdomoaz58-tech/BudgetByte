package com.example.budgetbyte.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.budgetbyte.data.entity.Expense;
import com.example.budgetbyte.data.repository.ExpenseRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeViewModel extends AndroidViewModel {
    private final ExpenseRepository repository;
    private final MutableLiveData<String> activeMonth = new MutableLiveData<>();
    private final MediatorLiveData<Long> balance = new MediatorLiveData<>();
    private final LiveData<Long> monthlyIncome;
    private final LiveData<Long> monthlyExpenses;
    private long income;
    private long expense;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new ExpenseRepository(application);
        activeMonth.setValue(defaultMonthKey());

        monthlyIncome = Transformations.switchMap(activeMonth, repository::getMonthlyIncome);
        monthlyExpenses = Transformations.switchMap(activeMonth, repository::getMonthlyExpenses);

        balance.addSource(monthlyIncome, value -> {
            income = value == null ? 0L : value;
            balance.setValue(income - expense);
        });
        balance.addSource(monthlyExpenses, value -> {
            expense = value == null ? 0L : value;
            balance.setValue(income - expense);
        });
    }

    public void loadForMonth(String yearMonth) {
        String normalizedMonth = normalizeMonthKey(yearMonth);
        if (!normalizedMonth.equals(activeMonth.getValue())) {
            activeMonth.setValue(normalizedMonth);
        }
    }

    public LiveData<List<Expense>> recentExpenses() {
        return repository.getRecentExpenses(5);
    }

    public LiveData<Long> balance() {
        return balance;
    }

    public LiveData<Long> monthlyIncome() {
        return monthlyIncome;
    }

    public LiveData<Long> monthlyExpenses() {
        return monthlyExpenses;
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

    public void insert(Expense expense) {
        repository.insert(expense);
    }

    public void delete(Expense expense) {
        repository.delete(expense);
    }

    @Override
    protected void onCleared() {
        repository.close();
    }
}
