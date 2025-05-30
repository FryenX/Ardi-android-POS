package com.example.ardimart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<Category> fullList = new ArrayList<>();
    private List<Category> filteredList = new ArrayList<>();
    private List<Category> pagedList = new ArrayList<>();
    private OnCategoryActionListener listener;
    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;


    public interface OnCategoryActionListener {
        void onEdit(Category category);
        void onDelete(Category category);
    }

    public void setListener(OnCategoryActionListener listener) {
        this.listener = listener;
    }

    public void setCategories(List<Category> categories) {
        this.fullList = new ArrayList<>(categories);
        this.filteredList = new ArrayList<>(categories);
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

    public void filter(String keyword) {
        if (keyword.isEmpty()) {
            filteredList = new ArrayList<>(fullList);
        } else {
            List<Category> result = new ArrayList<>();
            for (Category cat : fullList) {
                if (cat.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    result.add(cat);
                }
            }
            filteredList = result;
        }
        setPage(0);
    }

    @NonNull
    @Override
    public CategoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryAdapter.ViewHolder holder, int position) {
        Category category = pagedList.get(position);
        holder.txtCategoryNumber.setText((position + 1 + (currentPage * PAGE_SIZE)) + ".");
        holder.txtName.setText(category.getName());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(category);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(category);
        });
    }

    @Override
    public int getItemCount() {
        return pagedList.size();
    }

    public Category getItem(int position) {
        return filteredList.get(position);
    }

    public List<Category> getFullList() {
        return new ArrayList<>(fullList);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategoryNumber, txtName;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategoryNumber = itemView.findViewById(R.id.txtCategoryNumber);
            txtName = itemView.findViewById(R.id.txtCategoryName);
            btnEdit = itemView.findViewById(R.id.btnEditCategory);
            btnDelete = itemView.findViewById(R.id.btnDeleteCategory);
        }
    }
}

