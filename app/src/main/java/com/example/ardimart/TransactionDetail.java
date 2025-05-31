package com.example.ardimart;

public class TransactionDetail {
    private String barcode;
    private String productName;
    private double purchasePrice;
    private double sellPrice;
    private double qty;
    private double subTotal;

    public TransactionDetail(String barcode, String productName, double purchasePrice, double sellPrice, double qty, double subTotal) {
        this.barcode = barcode;
        this.productName = productName;
        this.purchasePrice = purchasePrice;
        this.sellPrice = sellPrice;
        this.qty = qty;
        this.subTotal = subTotal;
    }

    public String getBarcode() { return barcode; }
    public String getProductName() { return productName; }

    public double getQty() { return qty; }

    public double getSellPrice() { return sellPrice; }
    public double getSubTotal() { return subTotal; }
}
