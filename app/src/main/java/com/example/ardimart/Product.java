package com.example.ardimart;

import java.io.Serializable;

public class Product implements Serializable {
    private String barcode;
    private String name;
    private String units;
    private int categoryId;
    private String categoryName;
    private double stocks;
    private double purchasePrice;
    private double sellPrice;
    private String image;

    public Product(String barcode, String name, String units, int categoryId, String categoryName,
                   double stocks, double purchasePrice, double sellPrice, String image) {
        this.barcode = barcode;
        this.name = name;
        this.units = units;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.stocks = stocks;
        this.purchasePrice = purchasePrice;
        this.sellPrice = sellPrice;
        this.image = image;
    }

    public Product() {}

    public Product(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getUnits() { return units; }
    public double getStocks() { return stocks; }
    public double getPurchasePrice() { return purchasePrice; }
    public double getSellPrice() { return sellPrice; }
    public String getImage() {
        return image;
    }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public void setName(String name) { this.name = name; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setUnits(String units) { this.units = units; }
    public void setStocks(double stocks) { this.stocks = stocks; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }
    public void setSellPrice(double sellPrice) { this.sellPrice = sellPrice; }
    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return barcode + " - " + name;
    }
}