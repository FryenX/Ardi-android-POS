package com.example.ardimart;

import static java.security.AccessController.getContext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ardimart.config.DatabaseHelper;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Transaction> itemList;
    private OnTransactionChangeListener listener;
    private InputTransaction inputTransaction;
    private TransactionAdapter adapter;
    public TransactionAdapter(List<Transaction> itemList, OnTransactionChangeListener listener) {
        this.itemList = itemList;
        this.listener = listener;
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtItemNo, txtBarcode, txtQty, txtPrice, txtSubtotal;
        View btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            txtItemNo = itemView.findViewById(R.id.txtItemNo);
            txtBarcode = itemView.findViewById(R.id.txtBarcode);
            txtQty = itemView.findViewById(R.id.txtQty);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtSubtotal = itemView.findViewById(R.id.txtSubtotal);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction item = itemList.get(position);
        holder.txtItemNo.setText(String.valueOf(position + 1));
        holder.txtBarcode.setText(item.getBarcode());
        holder.txtQty.setText(String.valueOf(item.getQty()));
        holder.txtPrice.setText(String.valueOf(item.getPrice()));
        holder.txtSubtotal.setText(String.valueOf(item.getSubtotal()));

        holder.btnDelete.setOnClickListener(v -> {
            Context context = holder.itemView.getContext();
            DatabaseHelper dbHelper = new DatabaseHelper(context);
            SQLiteDatabase db = dbHelper.getConnection();


            String barcode = item.getBarcode();
            int qtyToRestore = (int) item.getQty();

            Cursor cursor = db.rawQuery("SELECT stocks FROM products WHERE barcode = ?", new String[]{barcode});
            if (cursor.moveToFirst()) {
                int currentStock = cursor.getInt(cursor.getColumnIndexOrThrow("stocks"));
                int newStock = currentStock + qtyToRestore;

                // 3. Update products stock
                ContentValues values = new ContentValues();
                values.put("stocks", newStock);
                db.update("products", values, "barcode = ?", new String[]{barcode});
            }
            cursor.close();

            db.delete("temp_transactions", "id = ?", new String[]{String.valueOf(item.getId())});
            db.close();

            // 5. Update UI
            itemList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, itemList.size());

            Toast.makeText(context, "Item deleted and stock restored", Toast.LENGTH_SHORT).show();

            if (listener != null) {
                listener.onTransactionChanged();
            }
        });
    }


    public interface OnTransactionChangeListener {
        void onTransactionChanged();
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}