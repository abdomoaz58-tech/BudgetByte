package com.example.budgetbyte.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.example.budgetbyte.data.dao.ExpenseDao;
import com.example.budgetbyte.data.db.AppDatabase;
import com.example.budgetbyte.data.entity.CategoryStat;
import com.example.budgetbyte.data.entity.Expense;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates Room reads and writes without allowing database work on the main thread. */
public final class ExpenseRepository {
    private final ExpenseDao expenseDao;
    private final ExecutorService databaseExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ExpenseRepository(Context context) {
        this(AppDatabase.getInstance(context).expenseDao(), Executors.newSingleThreadExecutor());
    }

    ExpenseRepository(ExpenseDao expenseDao, ExecutorService databaseExecutor) {
        this.expenseDao = expenseDao;
        this.databaseExecutor = databaseExecutor;
    }

    public LiveData<List<Expense>> getAllExpenses() {
        return expenseDao.getAllExpenses();
    }

    public LiveData<List<Expense>> getRecentExpenses(int limit) {
        return expenseDao.getRecentExpenses(limit);
    }

    public LiveData<Long> getMonthlyIncome(String yearMonth) {
        return expenseDao.getMonthlyIncome(yearMonth);
    }

    public LiveData<Long> getMonthlyExpenses(String yearMonth) {
        return expenseDao.getMonthlyExpenses(yearMonth);
    }

    public LiveData<List<CategoryStat>> getCategoryStats(String yearMonth) {
        return expenseDao.getCategoryStats(yearMonth);
    }

    public void insert(Expense expense) {
        databaseExecutor.execute(() -> expenseDao.insert(expense));
    }

    public void insert(Expense expense, WriteCallback callback) {
        databaseExecutor.execute(() -> {
            try {
                expenseDao.insert(expense);
                mainHandler.post(callback::onSuccess);
            } catch (RuntimeException exception) {
                mainHandler.post(() -> callback.onError(exception));
            }
        });
    }

    public void update(Expense expense) {
        databaseExecutor.execute(() -> expenseDao.update(expense));
    }

    public void update(Expense expense, WriteCallback callback) {
        databaseExecutor.execute(() -> {
            try {
                if (expenseDao.update(expense) == 0) {
                    throw new IllegalStateException("Transaction no longer exists");
                }
                mainHandler.post(callback::onSuccess);
            } catch (RuntimeException exception) {
                mainHandler.post(() -> callback.onError(exception));
            }
        });
    }

    public void delete(Expense expense) {
        databaseExecutor.execute(() -> expenseDao.delete(expense));
    }

    public void getExpensesByMonth(String yearMonth, ResultCallback<List<Expense>> callback) {
        databaseExecutor.execute(() -> runQuery(() -> expenseDao.getExpensesByMonthSync(yearMonth), callback));
    }

    public void getMonthlyTotals(String type, int months, ResultCallback<List<CategoryStat>> callback) {
        databaseExecutor.execute(() -> runQuery(() -> expenseDao.getMonthlyTotals(type, months), callback));
    }

    public void close() {
        databaseExecutor.shutdown();
    }

    private <T> void runQuery(Query<T> query, ResultCallback<T> callback) {
        try {
            callback.onResult(query.run());
        } catch (RuntimeException exception) {
            callback.onError(exception);
        }
    }

    private interface Query<T> {
        T run();
    }

    public interface ResultCallback<T> {
        void onResult(T result);
        void onError(Exception exception);
    }

    public interface WriteCallback {
        void onSuccess();
        void onError(Exception exception);
    }
}
