package com.example.ardimart;

public class RecentTransaction {
    public String invoice;
    public String date;
    public double total;

    public RecentTransaction(String invoice, String date, double total) {
        this.invoice = invoice;
        this.date = date;
        this.total = total;
    }
}