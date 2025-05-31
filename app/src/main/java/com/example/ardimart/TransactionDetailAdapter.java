package com.example.ardimart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionDetailAdapter extends RecyclerView.Adapter<TransactionDetailAdapter.ViewHolder> {
    private List<TransactionDetail> list;

    public TransactionDetailAdapter(List<TransactionDetail> list) {
        this.list = list;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtQty, txtPrice, txtSubtotal;

        public ViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtQty = itemView.findViewById(R.id.txtQty);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtSubtotal = itemView.findViewById(R.id.txtSubtotal);
        }
    }

    @Override
    public TransactionDetailAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TransactionDetailAdapter.ViewHolder holder, int position) {
        TransactionDetail item = list.get(position);

        holder.txtName.setText(item.getProductName() + " (" + item.getBarcode() + ")");
        holder.txtQty.setText(String.valueOf(item.getQty()));
        holder.txtPrice.setText(String.format("%.2f", item.getSellPrice()));
        holder.txtSubtotal.setText(String.format("%.2f", item.getSubTotal()));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
