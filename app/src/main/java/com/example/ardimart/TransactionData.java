package com.example.ardimart;

public class TransactionData {
    private String invoice;
    private String dateTime;
    private double grossTotal;
    private double discountIdr;
    private double netTotal;

    public TransactionData(String invoice, String dateTime, double grossTotal, double discountIdr, double netTotal) {
        this.invoice = invoice;
        this.dateTime = dateTime;
        this.grossTotal = grossTotal;
        this.discountIdr = discountIdr;
        this.netTotal = netTotal;
    }

    public String getInvoice() { return invoice; }
    public String getDateTime() { return dateTime; }
    public String getGrossTotal() { return String.valueOf(grossTotal); }
    public String getDiscountIdr() { return String.valueOf(discountIdr); }
    public String getNetTotal() { return String.valueOf(netTotal); }
}