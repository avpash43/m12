package com.epam.bd201.entity;

public enum Category {
    ERRONEOUS_DATE("Erroneous data"),
    SHORT_STAY("Short stay"),
    STANDARD_STAY("Standard stay"),
    STANDARD_EXTENDED_STAY("Standard extended stay"),
    LONG_STAY("Long stay");

    private String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
