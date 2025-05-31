package com.example.ardimart;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ardimart.config.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class UsersFragment extends Fragment {

    private RecyclerView recyclerUsers;
    private UserAdapter userAdapter;
    private Button btnPrev;
    private Button btnNext;
    private TextView txtPageIndicator;
    private boolean sortByNameAsc = true;
    private boolean sortByUserName = true;
    private boolean sortByLevel = true;
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
        EditText searchInput = view.findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                userAdapter.filter(s.toString());
                updatePageIndicator();
            }
            @Override public void afterTextChanged(Editable s) {}
        });


        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
        txtPageIndicator = view.findViewById(R.id.txtPageIndicator);

        btnPrev.setOnClickListener(v -> {
            int currentPage = userAdapter.getCurrentPage();
            if (currentPage > 0) {
                userAdapter.setPage(currentPage - 1);
                updatePageIndicator();
            }
        });

        btnNext.setOnClickListener(v -> {
            int currentPage = userAdapter.getCurrentPage();
            if (currentPage < userAdapter.getTotalPages() - 1) {
                userAdapter.setPage(currentPage + 1);
                updatePageIndicator();
            }
        });
        userAdapter.setListener(new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user) {
                Intent intent = new Intent(getContext(), EditUser.class);
                intent.putExtra("user", user);
                startActivityForResult(intent, ADD_USER_REQUEST_CODE);
            }

            @Override
            public void onDelete(User user) {
                new AlertDialog.Builder(getContext()).setTitle("Confirm Deletion").setMessage("Are you sure you want to delete user: " + user.getName() + "?").setPositiveButton("Delete", (dialog, which) -> {
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
                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error deleting user: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        } finally {
                            if (db != null && db.isOpen()) db.close();
                        }
                    });
                }).setNegativeButton("Cancel", null).show();
            }
        });

        recyclerUsers.setAdapter(userAdapter);

        TextView sortName = view.findViewById(R.id.sortName);
        TextView sortUserName = view.findViewById(R.id.sortUsername);
        TextView sortLevel = view.findViewById(R.id.sortLevel);

        sortName.setOnClickListener(v -> {
            sortByNameAsc = !sortByNameAsc;
            updateSortIndicators(SortField.NAME, sortByNameAsc);
            sortByName(sortByNameAsc);
        });

        sortUserName.setOnClickListener(v -> {
            sortByUserName = !sortByUserName;
            updateSortIndicators(SortField.USERNAME, sortByUserName);
            sortByUserName(sortByUserName);
        });

        sortLevel.setOnClickListener(v -> {
            sortByLevel = !sortByLevel;
            updateSortIndicators(SortField.LEVEL, sortByLevel);
            sortByLevel(sortByLevel);
        });

        FloatingActionButton btnAddUser = view.findViewById(R.id.btnAddUser);
        btnAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddUser.class);
            startActivityForResult(intent, ADD_USER_REQUEST_CODE);
        });

        loadUsersFromDatabase();

        return view;
    }

    private void updatePageIndicator() {
        int currentPage = userAdapter.getCurrentPage() + 1;
        int totalPages = userAdapter.getTotalPages();
        txtPageIndicator.setText("Page " + currentPage + " of " + totalPages);

        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
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
                    updatePageIndicator();
                    userAdapter.setUsers(finalUserList);
                    if (finalUserList.isEmpty()) {
                        showEmptyView("No users found");
                    } else {
                        hideEmptyView();
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error loading users: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if (cursor != null) cursor.close();
                if (db != null && db.isOpen()) db.close();
            }
        });
    }

    private void sortByName(boolean ascending) {
        List<User> currentList = userAdapter.getFullList();
        if (ascending) {
            Collections.sort(currentList, Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            Collections.sort(currentList, (c1, c2) -> c2.getName().compareToIgnoreCase(c1.getName()));
        }
        userAdapter.setUsers(currentList);
    }

    private void sortByUserName(boolean ascending) {
        List<User> currentList = userAdapter.getFullList();
        if (ascending) {
            Collections.sort(currentList, Comparator.comparing(User::getUserName, String.CASE_INSENSITIVE_ORDER));
        } else {
            Collections.sort(currentList, (c1, c2) -> c2.getUserName().compareToIgnoreCase(c1.getUserName()));
        }
        userAdapter.setUsers(currentList);
    }

    private void sortByLevel(boolean ascending) {
        List<User> currentList = userAdapter.getFullList();
        if (ascending) {
            Collections.sort(currentList, Comparator.comparing(User::getLevel, String.CASE_INSENSITIVE_ORDER));
        } else {
            Collections.sort(currentList, (c1, c2) -> c2.getLevel().compareToIgnoreCase(c1.getLevel()));
        }
        userAdapter.setUsers(currentList);
    }

    public enum SortField {
        NAME,
        USERNAME,
        LEVEL,
        NONE
    }
    private void updateSortIndicators(SortField sortField, boolean ascending) {
        TextView sortName = getView().findViewById(R.id.sortName);
        TextView sortUserName = getView().findViewById(R.id.sortUsername);
        TextView sortLevel = getView().findViewById(R.id.sortLevel);

        int upArrow = R.drawable.baseline_arrow_drop_up_24;
        int downArrow = R.drawable.baseline_arrow_drop_down_24;
        int none = R.drawable.ic_sort_none;

        sortName.setCompoundDrawablesWithIntrinsicBounds(0, 0, none, 0);
        sortUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, none, 0);
        sortLevel.setCompoundDrawablesWithIntrinsicBounds(0, 0, none, 0);

        switch (sortField) {
            case NAME:
                sortName.setCompoundDrawablesWithIntrinsicBounds(0, 0, ascending ? upArrow : downArrow, 0);
                break;
            case USERNAME:
                sortUserName.setCompoundDrawablesWithIntrinsicBounds(0, 0, ascending ? upArrow : downArrow, 0);
                break;
            case LEVEL:
                sortLevel.setCompoundDrawablesWithIntrinsicBounds(0, 0, ascending ? upArrow : downArrow, 0);
            case NONE:
            default:
                break;
        }
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
