package com.example.ardimart;

public class TransactionData {
    private String invoice;
    private String dateTime;
    private String customerName;
    private double discountPercent;
    private double discountIdr;
    private double grossTotal;
    private double netTotal;
    private double paymentAmount;
    private double paymentChange;
    public TransactionData(String invoice, String dateTime, String customerName, double discountPercent, double discountIdr, double grossTotal,double netTotal, double paymentAmount, double paymentChange) {
        this.invoice = invoice;
        this.dateTime = dateTime;
        this.customerName = customerName;
        this.discountPercent = discountPercent;
        this.discountIdr = discountIdr;
        this.grossTotal = grossTotal;
        this.netTotal = netTotal;
        this.paymentAmount = paymentAmount;
        this.paymentChange = paymentChange;
    }

    public String getInvoice() { return invoice; }
    public String getDateTime() { return dateTime; }
    public String getCustomerName() { return customerName; }
    public double getDiscPercent() { return discountPercent; }
    public double getDiscIdr() { return discountIdr; }
    public double getGrossTotal() { return grossTotal; }
    public double getNetTotal() { return netTotal; }
    public double getPaymentAmount() { return paymentAmount; }
    public double getPaymentChange() { return paymentChange; }
}