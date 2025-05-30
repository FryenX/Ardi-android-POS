package com.example.ardimart;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ardimart.config.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<User> fullList = new ArrayList<>();
    private List<User> filteredList = new ArrayList<>();
    private List<User> pagedList = new ArrayList<>();
    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onEdit(User user);
        void onDelete(User user);
    }

    public void setListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        this.fullList = new ArrayList<>(users);
        this.filteredList = new ArrayList<>(users);
        setPage(0);
    }

    public void filter(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            filteredList = new ArrayList<>(fullList);
        } else {
            String lowerKeyword = keyword.toLowerCase();
            List<User> filtered = new ArrayList<>();
            for (User user : fullList) {
                if (user.getUserName().toLowerCase().contains(lowerKeyword) ||
                        user.getName().toLowerCase().contains(lowerKeyword) ||
                        user.getLevel().toLowerCase().contains(lowerKeyword)) {
                    filtered.add(user);
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
    public UserAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return pagedList.size();
    }

    public List<User> getFullList() {
        return new ArrayList<>(fullList);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.ViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        SessionManager session = new SessionManager(context);
        int sessionId = session.getId();
        User user = pagedList.get(position);
        holder.txtUserNumber.setText((position + 1) + ".");
        holder.txtName.setText(user.getName());
        holder.txtUserName.setText(user.getUserName());
        holder.txtLevel.setText(user.getLevel());

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(user);
        });

        if (user.getId() == sessionId) {
            holder.btnDelete.setEnabled(false);
            holder.btnDelete.setAlpha(0.5f);
        } else {
            holder.btnDelete.setEnabled(true);
            holder.btnDelete.setAlpha(1f);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(user);
        });
    }
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUserNumber, txtName, txtUserName, txtLevel;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserNumber = itemView.findViewById(R.id.txtUserNumber);
            txtName = itemView.findViewById(R.id.txtName);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtLevel = itemView.findViewById(R.id.txtUserLevel);
            btnEdit = itemView.findViewById(R.id.btnEditUser);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}