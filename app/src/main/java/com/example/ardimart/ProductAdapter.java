package com.example.ardimart;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> fullList = new ArrayList<>();
    private List<Product> filteredList = new ArrayList<>();
    private List<Product> pagedList = new ArrayList<>();
    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;
    private OnProductActionListener listener;

    public interface OnProductActionListener {
        void onEdit(Product product);
        void onDetail(Product product);
        void onDelete(Product product);
    }

    public void setListener(OnProductActionListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.fullList = new ArrayList<>(products);
        this.filteredList = new ArrayList<>(products);
        setPage(0);
    }

    public void filter(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            filteredList = new ArrayList<>(fullList);
        } else {
            List<Product> filtered = new ArrayList<>();
            for (Product product : fullList) {
                if (product.getBarcode().toLowerCase().contains(keyword.toLowerCase()) ||
                        product.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                        product.getCategoryName().toLowerCase().contains(keyword.toLowerCase())) {
                    filtered.add(product);
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return pagedList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = pagedList.get(position);
        holder.txtProductNumber.setText((position + 1) + ".");
        holder.bind(product);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtProductNumber, txtName, txtBarcode, txtCategory;
        ImageButton btnEdit, btnDelete, btnDetail;

        ViewHolder(View itemView) {
            super(itemView);
            txtProductNumber = itemView.findViewById(R.id.txtProductNumber);
            txtName = itemView.findViewById(R.id.txtProductName);
            txtBarcode = itemView.findViewById(R.id.txtBarcode);
            txtCategory = itemView.findViewById(R.id.txtCategoryName);
            btnEdit = itemView.findViewById(R.id.btnEditProduct);
            btnDelete = itemView.findViewById(R.id.btnDeleteProduct);
            btnDetail = itemView.findViewById(R.id.btnDetailProduct);
        }

        void bind(Product product) {
            txtName.setText(product.getName());
            txtBarcode.setText(product.getBarcode());
            txtCategory.setText(product.getCategoryName());

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(product);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(product);
            });

            btnDetail.setOnClickListener(v -> {
                if (listener != null) listener.onDetail(product);
            });
        }
    }
}
