package com.example.budgetbyte.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** A transaction whose amount is stored in minor currency units. */
@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    public long amount;
    public String category;
    public String type;
    public String date;

    public Expense(String title, long amount, String category, String type, String date) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.type = type;
        this.date = date;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public boolean isIncome() {
        return "income".equalsIgnoreCase(type);
    }
}
