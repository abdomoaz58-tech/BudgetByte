package com.example.budgetbyte.ui.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddExpenseViewModel extends ViewModel {
    public static final class SaveResult {
        public final boolean success;
        public final boolean editing;
        public final boolean addAnother;

        private SaveResult(boolean success, boolean editing, boolean addAnother) {
            this.success = success;
            this.editing = editing;
            this.addAnother = addAnother;
        }
    }

    public static final String KEY_TRANSACTION_TYPE = "transactionType";
    public static final String KEY_SELECTED_DATE = "selectedDate";
    public static final String KEY_TITLE = "title";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_CATEGORY = "category";
    public static final String KEY_EXPENSE_ID = "expenseId";
    private static final String KEY_EDIT_LOADED = "editLoaded";
    public static final String TYPE_EXPENSE = "expense";
    public static final String TYPE_INCOME = "income";

    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();

    public AddExpenseViewModel(@NonNull SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
    }

    public String getTransactionType() {
        String type = savedStateHandle.get(KEY_TRANSACTION_TYPE);
        return type == null || type.trim().isEmpty() ? TYPE_EXPENSE : type;
    }

    public void setTransactionType(String transactionType) {
        String normalized = (transactionType == null || transactionType.trim().isEmpty())
                ? TYPE_EXPENSE
                : transactionType.trim();
        savedStateHandle.set(KEY_TRANSACTION_TYPE, normalized);
    }

    public String getSelectedDate() {
        String date = savedStateHandle.get(KEY_SELECTED_DATE);
        if (date != null && !date.trim().isEmpty()) {
            return date;
        }
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public void setSelectedDate(String date) {
        String normalized = date == null || date.trim().isEmpty()
                ? new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())
                : date.trim();
        savedStateHandle.set(KEY_SELECTED_DATE, normalized);
    }

    public String getTitle() {
        String title = savedStateHandle.get(KEY_TITLE);
        return title == null ? "" : title;
    }

    public void setTitle(String title) {
        savedStateHandle.set(KEY_TITLE, title == null ? "" : title);
    }

    public String getAmount() {
        String amount = savedStateHandle.get(KEY_AMOUNT);
        return amount == null ? "" : amount;
    }

    public void setAmount(String amount) {
        savedStateHandle.set(KEY_AMOUNT, amount == null ? "" : amount.trim());
    }

    public String getCategory() {
        String category = savedStateHandle.get(KEY_CATEGORY);
        return category == null ? "" : category;
    }

    public void setCategory(String category) {
        savedStateHandle.set(KEY_CATEGORY, category == null ? "" : category.trim());
    }

    public long getExpenseId() {
        Long value = savedStateHandle.get(KEY_EXPENSE_ID);
        return value == null ? -1L : value;
    }

    public void setExpenseId(long id) {
        savedStateHandle.set(KEY_EXPENSE_ID, id);
    }

    public boolean isEditLoaded() {
        Boolean loaded = savedStateHandle.get(KEY_EDIT_LOADED);
        return Boolean.TRUE.equals(loaded);
    }

    public void setEditLoaded(boolean loaded) {
        savedStateHandle.set(KEY_EDIT_LOADED, loaded);
    }

    public LiveData<Boolean> saving() {
        return saving;
    }

    public boolean beginSave() {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return false;
        }
        saving.setValue(true);
        return true;
    }

    public void finishSave() {
        saving.setValue(false);
    }

    public LiveData<SaveResult> saveResult() {
        return saveResult;
    }

    public void completeSave(boolean editing, boolean addAnother) {
        saving.setValue(false);
        saveResult.setValue(new SaveResult(true, editing, addAnother));
    }

    public void failSave() {
        saving.setValue(false);
        saveResult.setValue(new SaveResult(false, false, false));
    }

    public void clearSaveResult() {
        saveResult.setValue(null);
    }

    public void resetValuesKeepingType() {
        setTitle("");
        setAmount("");
        setCategory("");
        setSelectedDate(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
    }

    public void resetForm() {
        setTransactionType(TYPE_EXPENSE);
        setTitle("");
        setAmount("");
        setCategory("");
        setSelectedDate(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        setExpenseId(-1L);
    }
}
