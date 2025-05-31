package com.example.ardimart;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionDataAdapter extends RecyclerView.Adapter<TransactionDataAdapter.ViewHolder> {
    private List<TransactionData> fullList = new ArrayList<>();
    private List<TransactionData> filteredList = new ArrayList<>();
    private List<TransactionData> pagedList = new ArrayList<>();
    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;
    private TransactionDataAdapter.OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onDetail(TransactionData transaction);
        void onDelete(TransactionData transaction);
    }

    public void setListener(TransactionDataAdapter.OnTransactionClickListener listener) {
        this.listener = listener;
    }
    public void setTransactions(List<TransactionData> transactions) {
        this.fullList = new ArrayList<>(transactions);
        this.filteredList = new ArrayList<>(transactions);
        setPage(0);
    }

    public void filter(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            filteredList = new ArrayList<>(fullList);
        } else {
            String lowerKeyword = keyword.toLowerCase();
            List<TransactionData> filtered = new ArrayList<>();
            for (TransactionData t : fullList) {
                if (t.getInvoice().toLowerCase().contains(lowerKeyword) ||
                        t.getDateTime().toLowerCase().contains(lowerKeyword)) {
                    filtered.add(t);
                }
            }
            filteredList = filtered;
        }
        setPage(0);
    }

    public void setPage(int page) {
        int fromIndex = page * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredList.size());
        if (fromIndex <= toIndex) {
            this.currentPage = page;
            this.pagedList = filteredList.subList(fromIndex, toIndex);
            notifyDataSetChanged();
        }
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) filteredList.size() / PAGE_SIZE);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void nextPage() {
        if (hasNextPage()) {
            setPage(currentPage + 1);
        }
    }

    public void previousPage() {
        if (hasPreviousPage()) {
            setPage(currentPage - 1);
        }
    }

    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }

    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    public int getFilteredItemCount() {
        return filteredList.size();
    }

    @NonNull
    @Override
    public TransactionDataAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_data, parent, false);
        return new TransactionDataAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionData t = pagedList.get(position);
        holder.txtNo.setText(String.valueOf((currentPage * PAGE_SIZE) + position + 1));
        holder.invoiceText.setText(t.getInvoice());
        holder.dateText.setText(t.getDateTime());
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        holder.netText.setText(currencyFormat.format(Double.parseDouble(String.valueOf(t.getNetTotal()))));

        holder.detailBtn.setOnClickListener(v -> listener.onDetail(t));
        holder.deleteBtn.setOnClickListener(v -> listener.onDelete(t));
    }

    @Override
    public int getItemCount() {
        return pagedList.size();
    }

    public List<TransactionData> getFullList() {
        return new ArrayList<>(fullList);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNo, invoiceText, dateText, netText;
        ImageButton detailBtn, deleteBtn;

        public ViewHolder(View itemView) {
            super(itemView);
            txtNo = itemView.findViewById(R.id.txtNo);
            invoiceText = itemView.findViewById(R.id.invoiceText);
            dateText = itemView.findViewById(R.id.dateText);
            netText = itemView.findViewById(R.id.netText);
            detailBtn = itemView.findViewById(R.id.detailButton);
            deleteBtn = itemView.findViewById(R.id.deleteButton);
        }
    }
}
