package com.example.budgetbyte;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.example.budgetbyte.util.MoneyUtils;

import org.junit.Test;

public final class MoneyUtilsTest {

    @Test
    public void parsesWesternDigits() {
        assertEquals(5000L, MoneyUtils.parseMinorUnits("50"));
        assertEquals(5050L, MoneyUtils.parseMinorUnits("50.50"));
        assertEquals(105L, MoneyUtils.parseMinorUnits("1.05"));
    }

    @Test
    public void parsesArabicIndicDigits() {
        assertEquals(5000L, MoneyUtils.parseMinorUnits("٥٠"));
        assertEquals(12345L, MoneyUtils.parseMinorUnits("١٢٣٫٤٥"));
    }

    @Test
    public void parsesEasternArabicIndicDigits() {
        assertEquals(5000L, MoneyUtils.parseMinorUnits("۵۰"));
        assertEquals(12345L, MoneyUtils.parseMinorUnits("۱۲۳٫۴۵"));
    }

    @Test
    public void parsesCommaDecimals() {
        assertEquals(12550L, MoneyUtils.parseMinorUnits("125,50"));
        assertEquals(50L, MoneyUtils.parseMinorUnits("0,50"));
    }

    @Test
    public void parsesThousandsSeparators() {
        assertEquals(125050L, MoneyUtils.parseMinorUnits("1,250.50"));
        assertEquals(125050L, MoneyUtils.parseMinorUnits("1.250,50"));
        assertEquals(100000L, MoneyUtils.parseMinorUnits("1,000"));
    }

    @Test
    public void parsesCurrencySymbols() {
        assertEquals(125000L, MoneyUtils.parseMinorUnits("1250 EGP"));
        assertEquals(125000L, MoneyUtils.parseMinorUnits("ج.م 1250"));
        assertEquals(5000L, MoneyUtils.parseMinorUnits("$50.00"));
    }

    @Test
    public void handlesEmptyAndNullInput() {
        assertEquals(0L, MoneyUtils.parseMinorUnits(""));
        assertEquals(0L, MoneyUtils.parseMinorUnits("   "));
        assertEquals(0L, MoneyUtils.parseMinorUnits(null));
    }

    @Test
    public void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtils.parseMinorUnits("abc"));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtils.parseMinorUnits("12.345"));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtils.parseMinorUnits("-50"));
    }
}

