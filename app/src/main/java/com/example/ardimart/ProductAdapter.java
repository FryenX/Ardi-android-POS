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
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnProductActionListener {
        void onEdit(Product product);

        void onDetail(Product product);

        void onDelete(Product product);
    }

    private List<Product> productList;
    private OnProductActionListener listener;

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
    }

    public void setProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    public void setListener(OnProductActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.txtProductNumber.setText(String.valueOf(position + 1));
        holder.txtBarcode.setText(product.getBarcode());
        holder.txtProductName.setText(product.getName());
//        holder.txtCategory.setText(product.getCategoryName());
//        holder.txtStock.setText(String.valueOf(product.getStocks()));
        holder.txtSellPrice.setText(String.format("%.2f", product.getSellPrice()));
//        String imagePath = product.getImage();
//        if (imagePath != null && !imagePath.isEmpty()) {
//            File imgFile = new File(imagePath);
//            if (imgFile.exists()) {
//                holder.imgProduct.setImageURI(Uri.fromFile(imgFile));
//            } else {
//                holder.imgProduct.setImageResource(R.drawable.ic_image_placeholder); // fallback image
//            }
//        } else {
//            holder.imgProduct.setImageResource(R.drawable.ic_image_placeholder); // fallback image
//        }
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(product);
        });

        holder.btnDetail.setOnClickListener(v -> {
            if (listener != null) listener.onDetail(product);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(product);
        });
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtProductNumber, txtBarcode, txtProductName, txtSellPrice;
        ImageButton btnEdit, btnDetail, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtProductNumber = itemView.findViewById(R.id.txtProductNumber);
            txtBarcode = itemView.findViewById(R.id.txtBarcode);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtSellPrice = itemView.findViewById(R.id.txtSellPrice);
            btnEdit = itemView.findViewById(R.id.btnEditProduct);
            btnDetail = itemView.findViewById(R.id.btnDetailProduct);
            btnDelete = itemView.findViewById(R.id.btnDeleteProduct);
        }
    }
}
