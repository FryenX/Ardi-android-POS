package com.example.ardimart;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ardimart.config.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UsersFragment extends Fragment {

    private RecyclerView recyclerUsers;
    private UserAdapter userAdapter;
    private View emptyView;

    public UsersFragment() {
    }

    private static final int ADD_USER_REQUEST_CODE = 101;

    public static UsersFragment newInstance() {
        return new UsersFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        emptyView = view.findViewById(R.id.emptyView);
        recyclerUsers = view.findViewById(R.id.recyclerUsers);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new UserAdapter();

        userAdapter.setListener(new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user) {
                Intent intent = new Intent(getContext(), EditUser.class);
                intent.putExtra("user", user); // Send user object
                startActivityForResult(intent, ADD_USER_REQUEST_CODE);
            }

            @Override
            public void onDelete(User user) {
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    SQLiteDatabase db = null;
                    try {
                        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                        dbHelper.copyDatabaseIfNeeded();
                        db = dbHelper.getConnection();

                        int rowsDeleted = db.delete("users", "id = ?", new String[]{String.valueOf(user.getId())});

                        requireActivity().runOnUiThread(() -> {
                            if (rowsDeleted > 0) {
                                Toast.makeText(getContext(), "User deleted", Toast.LENGTH_SHORT).show();
                                loadUsersFromDatabase();
                            } else {
                                Toast.makeText(getContext(), "Failed to delete user", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error deleting user: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    } finally {
                        if (db != null && db.isOpen()) db.close();
                    }
                });
            }
        });

        recyclerUsers.setAdapter(userAdapter);

        FloatingActionButton btnAddUser = view.findViewById(R.id.btnAddUser);
        btnAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddUser.class);
            startActivityForResult(intent, ADD_USER_REQUEST_CODE);
        });

        loadUsersFromDatabase();

        return view;
    }

    private void loadUsersFromDatabase() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<User> userList = new ArrayList<>();
            SQLiteDatabase db = null;
            Cursor cursor = null;

            try {
                DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                dbHelper.copyDatabaseIfNeeded();
                db = dbHelper.getConnection();

                String query = "SELECT id, name, username, levels FROM users ORDER BY id ASC";
                cursor = db.rawQuery(query, null);

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                        String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                        String level = cursor.getString(cursor.getColumnIndexOrThrow("levels"));

                        userList.add(new User(id, name, username, level));
                    } while (cursor.moveToNext());
                }

                List<User> finalUserList = new ArrayList<>(userList);
                requireActivity().runOnUiThread(() -> {
                    userAdapter.setUsers(finalUserList);
                    if (finalUserList.isEmpty()) {
                        showEmptyView("No users found");
                    } else {
                        hideEmptyView();
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error loading users: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            } finally {
                if (cursor != null) cursor.close();
                if (db != null && db.isOpen()) db.close();
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_USER_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            loadUsersFromDatabase();
        }
    }

    private void showEmptyView(String message) {
        LinearLayout emptyView = requireView().findViewById(R.id.emptyView);
        TextView tvEmptyText = requireView().findViewById(R.id.tvEmptyText);
        emptyView.setVisibility(View.VISIBLE);
        tvEmptyText.setText(message);
    }

    private void hideEmptyView() {
        LinearLayout emptyView = requireView().findViewById(R.id.emptyView);
        emptyView.setVisibility(View.GONE);
    }
}
