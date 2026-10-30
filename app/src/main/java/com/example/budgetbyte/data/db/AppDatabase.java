package com.example.budgetbyte.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.budgetbyte.data.dao.ExpenseDao;
import com.example.budgetbyte.data.entity.Expense;

/** Process-wide Room database. Schema changes require an explicit migration. */
@Database(entities = {Expense.class}, version = 1, exportSchema = true)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract ExpenseDao expenseDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "budgetbyte.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
