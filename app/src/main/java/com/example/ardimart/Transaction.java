package com.example.ardimart;

public class Transaction {
    private String barcode, name;
    private int id, qty;
    private double price, subtotal;

    public Transaction(int id, String barcode, String name, int qty, double price, double subtotal) {
        this.id = id;
        this.barcode = barcode;
        this.name = name;
        this.qty = qty;
        this.price = price;
        this.subtotal = subtotal;
    }

    public int getId() {
        return id;
    }
    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public double getQty() { return qty; }
    public double getPrice() { return price; }
    public double getSubtotal() { return subtotal; }
}
