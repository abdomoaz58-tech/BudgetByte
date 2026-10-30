package com.example.budgetbyte.data.entity;

/** Projection used by Room aggregate queries. Totals are minor currency units. */
public class CategoryStat {
    public String category;
    public long total;
}
