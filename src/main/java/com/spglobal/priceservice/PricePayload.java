package com.spglobal.priceservice;

public class PricePayload {
    // Price data
    private double price;
    private String currency;

    // Constructor
    public PricePayload(double price, String currency) {
        this.price = price;
        this.currency = currency;
    }

    // Getters
    public double getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}