package com.example.budgetbyte.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.budgetbyte.data.entity.Expense;
import com.example.budgetbyte.data.repository.ExpenseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionsViewModel extends AndroidViewModel {
    private static final String TYPE_ALL = "all";

    private final ExpenseRepository repository;
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> typeFilter = new MutableLiveData<>(TYPE_ALL);
    private final MediatorLiveData<List<Expense>> filteredExpenses = new MediatorLiveData<>();
    private List<Expense> allExpenses = new ArrayList<>();

    public TransactionsViewModel(@NonNull Application application) {
        super(application);
        repository = new ExpenseRepository(application);
        filteredExpenses.addSource(repository.getAllExpenses(), value -> {
            allExpenses = value == null ? new ArrayList<>() : value;
            applyFilter();
        });
        filteredExpenses.addSource(searchQuery, value -> applyFilter());
        filteredExpenses.addSource(typeFilter, value -> applyFilter());
    }

    public LiveData<List<Expense>> expenses() {
        return filteredExpenses;
    }

    public LiveData<String> searchQuery() {
        return searchQuery;
    }

    public LiveData<String> typeFilter() {
        return typeFilter;
    }

    public void setFilter(String value) {
        searchQuery.setValue(value == null ? "" : value.trim());
    }

    public void setTypeFilter(String value) {
        String normalized = value == null || value.trim().isEmpty() ? TYPE_ALL : value.trim().toLowerCase(Locale.ROOT);
        typeFilter.setValue(normalized);
    }

    public void delete(Expense expense) {
        repository.delete(expense);
    }

    private void applyFilter() {
        String query = searchQuery.getValue() == null ? "" : searchQuery.getValue().trim().toLowerCase(Locale.ROOT);
        String type = typeFilter.getValue() == null ? TYPE_ALL : typeFilter.getValue().trim().toLowerCase(Locale.ROOT);

        List<Expense> result = new ArrayList<>();
        for (Expense item : allExpenses) {
            if (item == null) {
                continue;
            }

            boolean matchesType = TYPE_ALL.equals(type)
                    || type.equalsIgnoreCase(item.getType() == null ? "" : item.getType());
            boolean matchesQuery = query.isEmpty()
                    || containsSearchText(item, query);

            if (matchesType && matchesQuery) {
                result.add(item);
            }
        }

        filteredExpenses.setValue(result);
    }

    private boolean containsSearchText(Expense item, String query) {
        String title = item.getTitle() == null ? "" : item.getTitle().toLowerCase(Locale.ROOT);
        String category = item.getCategory() == null ? "" : item.getCategory().toLowerCase(Locale.ROOT);
        return title.contains(query) || category.contains(query);
    }

    @Override
    protected void onCleared() {
        repository.close();
    }
}
