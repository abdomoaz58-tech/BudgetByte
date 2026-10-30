package com.example.budgetbyte.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.budgetbyte.data.entity.CategoryStat;
import com.example.budgetbyte.data.entity.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Expense expense);

    @Update
    int update(Expense expense);

    @Delete
    void delete(Expense expense);

    /** All transactions, newest first. */
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    LiveData<List<Expense>> getAllExpenses();

    /** Recent N transactions for home dashboard. */
    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC LIMIT :limit")
    LiveData<List<Expense>> getRecentExpenses(int limit);

    /** Monthly income total (minor units). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE type = 'income' AND strftime('%Y-%m', date) = :yearMonth")
    LiveData<Long> getMonthlyIncome(String yearMonth);

    /** Monthly expense total (minor units). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE type = 'expense' AND strftime('%Y-%m', date) = :yearMonth")
    LiveData<Long> getMonthlyExpenses(String yearMonth);

    /** Per-category totals for expenses in a given month — for stats/budget. */
    @Query("SELECT category, SUM(amount) AS total FROM expenses WHERE type = 'expense' AND strftime('%Y-%m', date) = :yearMonth GROUP BY category ORDER BY total DESC")
    LiveData<List<CategoryStat>> getCategoryStats(String yearMonth);

    /** Monthly totals for last N months — for bar chart. */
    @Query("SELECT strftime('%Y-%m', date) AS category, SUM(amount) AS total FROM expenses WHERE type = :type GROUP BY strftime('%Y-%m', date) ORDER BY strftime('%Y-%m', date) DESC LIMIT :months")
    List<CategoryStat> getMonthlyTotals(String type, int months);

    /** All expenses in a month (for export). */
    @Query("SELECT * FROM expenses WHERE strftime('%Y-%m', date) = :yearMonth ORDER BY date DESC")
    List<Expense> getExpensesByMonthSync(String yearMonth);

    /** All transactions (for export). */
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    List<Expense> getAllExpensesSync();
}
