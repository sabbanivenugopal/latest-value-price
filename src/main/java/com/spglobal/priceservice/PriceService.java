package com.spglobal.priceservice;

import java.util.HashMap;
import java.util.Map;

public class PriceService {
    private Map<String, Double> prices;

    public PriceService() {
        this.prices = new HashMap<>();
    }

    public void setPrice(String product, double price) {
        prices.put(product, price);
    }

    public Double getPrice(String product) {
        return prices.get(product);
    }

    public Map<String, Double> getAllPrices() {
        return new HashMap<>(prices);
    }

    public void clearPrices() {
        prices.clear();
    }
}