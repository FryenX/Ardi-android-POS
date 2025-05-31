package com.example.ardimart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentTransactionAdapter extends RecyclerView.Adapter<RecentTransactionAdapter.ViewHolder> {

    private final List<RecentTransaction> transactions;

    public RecentTransactionAdapter(List<RecentTransaction> transactions) {
        this.transactions = transactions;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtTransaction;
        public ViewHolder(View view) {
            super(view);
            txtTransaction = view.findViewById(R.id.txtTransaction);
        }
    }

    @NonNull
    @Override
    public RecentTransactionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentTransactionAdapter.ViewHolder holder, int position) {
        RecentTransaction transaction = transactions.get(position);
        String text = transaction.invoice + " | " + transaction.date + " | " + String.format("%.2f", transaction.total);
        holder.txtTransaction.setText(text);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }
}
