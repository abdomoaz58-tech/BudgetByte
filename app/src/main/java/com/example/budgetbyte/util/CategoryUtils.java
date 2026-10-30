package com.example.budgetbyte.util;

import android.content.Context;

import com.example.budgetbyte.R;

/**
 * Maps stored canonical category values (English keys written by AddExpenseFragment and OCR)
 * to localized display strings. Supports legacy stored values without any database migration.
 *
 * <p>Rules:
 * <ul>
 *   <li>Call {@link #getLocalizedName(Context, String)} whenever displaying a category to the user.
 *   <li>Never change the stored value — it stays in English in the database.
 *   <li>Unknown / empty values fall back to {@code @string/category_other}.
 * </ul>
 */
public final class CategoryUtils {

    private CategoryUtils() { }

    /**
     * Returns the localized display name for the given stored category key.
     * Unknown keys are displayed as the "Other" string so no data is lost.
     */
    public static String getLocalizedName(Context context, String storedCategory) {
        if (storedCategory == null || storedCategory.trim().isEmpty()) {
            return context.getString(R.string.category_other);
        }
        switch (storedCategory.trim()) {
            case "Food":           return context.getString(R.string.category_food);
            case "Transportation": return context.getString(R.string.category_transportation);
            case "Bills":          return context.getString(R.string.category_bills);
            case "Entertainment":  return context.getString(R.string.category_entertainment);
            case "Shopping":       return context.getString(R.string.category_shopping);
            case "Health":         return context.getString(R.string.category_health);
            case "Education":      return context.getString(R.string.category_education);
            case "Groceries":      return context.getString(R.string.category_groceries);
            case "Salary":         return context.getString(R.string.category_salary);
            case "Freelance":      return context.getString(R.string.category_freelance);
            case "Gift":           return context.getString(R.string.category_gift);
            case "Other":          return context.getString(R.string.category_other);
            // Legacy Arabic-stored values (from old builds that stored Arabic display names).
            case "طعام":           return context.getString(R.string.category_food);
            case "مواصلات":        return context.getString(R.string.category_transportation);
            case "فواتير":         return context.getString(R.string.category_bills);
            case "ترفيه":          return context.getString(R.string.category_entertainment);
            case "تسوق":           return context.getString(R.string.category_shopping);
            case "صحة":            return context.getString(R.string.category_health);
            case "تعليم":          return context.getString(R.string.category_education);
            case "بقالة":          return context.getString(R.string.category_groceries);
            case "راتب":           return context.getString(R.string.category_salary);
            case "عمل حر":         return context.getString(R.string.category_freelance);
            case "هدية":           return context.getString(R.string.category_gift);
            case "Uncategorized":
            default:
                return context.getString(R.string.category_other);
        }
    }
}

