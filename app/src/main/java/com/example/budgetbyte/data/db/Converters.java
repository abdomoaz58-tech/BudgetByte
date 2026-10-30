package com.example.budgetbyte.data.db;

import androidx.room.TypeConverter;

import java.util.Date;

/** Room converters retained for date-bearing entities introduced in future schema versions. */
public final class Converters {
    private Converters() { }

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
