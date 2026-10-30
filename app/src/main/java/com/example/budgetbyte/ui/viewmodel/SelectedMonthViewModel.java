package com.example.budgetbyte.ui.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.Calendar;
import java.util.Locale;

public class SelectedMonthViewModel extends ViewModel {
    private static final String KEY_YEAR = "selected_year";
    private static final String KEY_MONTH = "selected_month";

    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<YearMonth> selectedMonth = new MutableLiveData<>();

    public SelectedMonthViewModel(@NonNull SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
        refreshSelectedMonth();
    }

    public LiveData<YearMonth> selectedMonth() {
        return selectedMonth;
    }

    public YearMonth getSelectedMonthValue() {
        return new YearMonth(getYear(), getMonth());
    }

    public int getYear() {
        Integer year = savedStateHandle.get(KEY_YEAR);
        if (year == null) {
            Calendar now = Calendar.getInstance();
            return now.get(Calendar.YEAR);
        }
        return year;
    }

    public int getMonth() {
        Integer month = savedStateHandle.get(KEY_MONTH);
        if (month == null) {
            Calendar now = Calendar.getInstance();
            return now.get(Calendar.MONTH) + 1;
        }
        return month;
    }

    public void setYearMonth(int year, int month) {
        int normalizedMonth = month < 1 ? 1 : Math.min(month, 12);
        savedStateHandle.set(KEY_YEAR, year);
        savedStateHandle.set(KEY_MONTH, normalizedMonth);
        refreshSelectedMonth();
    }

    public void setCurrentMonth() {
        Calendar now = Calendar.getInstance();
        setYearMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
    }

    public void shiftMonth(int monthDelta) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(getYear(), getMonth() - 1, 1);
        calendar.add(Calendar.MONTH, monthDelta);
        setYearMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
    }

    private void refreshSelectedMonth() {
        selectedMonth.setValue(new YearMonth(getYear(), getMonth()));
    }

    public static final class YearMonth {
        public final int year;
        public final int month;

        public YearMonth(int year, int month) {
            this.year = year;
            this.month = month;
        }

        public String toYearMonthString() {
            return String.format(Locale.US, "%04d-%02d", year, month);
        }

        public int yearMonthCode() {
            return year * 12 + month;
        }
    }
}
