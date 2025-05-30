package com.example.ardimart;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
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

public class CategoriesFragment extends Fragment implements AddCategoryDialog.AddCategoryListener {


    // TODO: Rename and change types of parameters


    public CategoriesFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static CategoriesFragment newInstance(String param1, String param2) {
        CategoriesFragment fragment = new CategoriesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private RecyclerView recyclerCategories;
    private CategoryAdapter categoryAdapter;
    private View emptyView;
    private boolean sortByNameAsc = true;

    private Button btnPrev;
    private Button btnNext;
    private TextView txtPageIndicator;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);
        emptyView = view.findViewById(R.id.emptyView);
        recyclerCategories = view.findViewById(R.id.recyclerCategories);
        recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapter = new CategoryAdapter();
        EditText searchInput = view.findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                categoryAdapter.filter(s.toString());
                updatePaginationText();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        TextView headerName = view.findViewById(R.id.headerName);

        headerName.setOnClickListener(v -> {
            sortByNameAsc = !sortByNameAsc;
            updateSortIndicators(false, sortByNameAsc);
            sortByName(sortByNameAsc);
        });

        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
        txtPageIndicator = view.findViewById(R.id.txtPageIndicator);

        btnPrev.setOnClickListener(v -> {
            int page = categoryAdapter.getCurrentPage();
            if (page > 0) {
                categoryAdapter.setPage(page - 1);
                updatePageIndicator();
            }
        });

        btnNext.setOnClickListener(v -> {
            int page = categoryAdapter.getCurrentPage();
            if (page < categoryAdapter.getTotalPages() - 1) {
                categoryAdapter.setPage(page + 1);
                updatePageIndicator();
            }
        });

        categoryAdapter.setListener(new CategoryAdapter.OnCategoryActionListener() {
            public void onEdit(Category category) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                View dialogView = inflater.inflate(R.layout.dialog_edit_category, null);

                EditText txtCategoryName = dialogView.findViewById(R.id.txtCategoryName);
                txtCategoryName.setText(category.getName());
                txtCategoryName.requestFocus();
                txtCategoryName.selectAll();

                new AlertDialog.Builder(getContext()).setTitle("Edit Category").setView(dialogView).setPositiveButton("Save", (dialog, which) -> {
                    String newName = txtCategoryName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateCategory(category.getId(), newName);
                    } else {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Cancel", null).show();
            }

            public void onDelete(Category category) {
                new AlertDialog.Builder(getContext()).setTitle("Confirm Deletion").setMessage("Are you sure you want to delete category: " + category.getName() + "?").setPositiveButton("Delete", (dialog, which) -> {
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        SQLiteDatabase db = null;
                        try {
                            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                            dbHelper.copyDatabaseIfNeeded();
                            db = dbHelper.getConnection();

                            int rowsDeleted = db.delete("categories", "id = ?", new String[]{String.valueOf(category.getId())});

                            requireActivity().runOnUiThread(() -> {
                                if (rowsDeleted > 0) {
                                    Toast.makeText(getContext(), "Category deleted", Toast.LENGTH_SHORT).show();
                                    loadCategoriesFromDatabase();  // Refresh list
                                } else {
                                    Toast.makeText(getContext(), "Failed to delete category", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error deleting category: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        } finally {
                            if (db != null && db.isOpen()) {
                                db.close();
                            }
                        }
                    });
                }).setNegativeButton("Cancel", null).show();
            }
        });

        recyclerCategories.setAdapter(categoryAdapter);

        FloatingActionButton btnAddCategory = view.findViewById(R.id.btnAddCategory);
        btnAddCategory.setOnClickListener(v -> {
            AddCategoryDialog dialog = new AddCategoryDialog();
            dialog.show(getChildFragmentManager(), "AddCategoryDialog");
        });

        loadCategoriesFromDatabase();

        return view;
    }
    private void updatePaginationText() {
        int current = categoryAdapter.getCurrentPage() + 1;
        int total = categoryAdapter.getTotalPages();
        txtPageIndicator.setText("Page " + current + " of " + (total == 0 ? 1 : total));
    }
    private void updatePageIndicator() {
        TextView txtPageIndicator = getView().findViewById(R.id.txtPageIndicator);
        int current = categoryAdapter.getCurrentPage() + 1;
        int total = categoryAdapter.getTotalPages();
        txtPageIndicator.setText("Page " + current + " of " + total);
    }
    private void sortByName(boolean ascending) {
        List<Category> currentList = categoryAdapter.getFullList();
        if (ascending) {
            Collections.sort(currentList, Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            Collections.sort(currentList, (c1, c2) -> c2.getName().compareToIgnoreCase(c1.getName()));
        }
        categoryAdapter.setCategories(currentList);
    }

    private void updateSortIndicators(boolean sortByName, boolean ascending) {
        TextView headerName = getView().findViewById(R.id.headerName);

        int upArrow = R.drawable.baseline_arrow_drop_up_24;
        int downArrow = R.drawable.baseline_arrow_drop_down_24;
        int none = R.drawable.ic_sort_none;

        if (sortByName) {
            headerName.setCompoundDrawablesWithIntrinsicBounds(0, 0, none, 0);
        } else {
            headerName.setCompoundDrawablesWithIntrinsicBounds(0, 0, ascending ? upArrow : downArrow, 0);
        }
    }

    private void loadCategoriesFromDatabase() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Category> categoryList = new ArrayList<>();
            SQLiteDatabase db = null;
            Cursor cursor = null;

            try {
                DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                dbHelper.copyDatabaseIfNeeded();
                db = dbHelper.getConnection();

                cursor = db.rawQuery("SELECT id, name FROM categories ORDER BY id ASC", null);
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                    categoryList.add(new Category(id, name));
                }

                requireActivity().runOnUiThread(() -> {
                    categoryAdapter.setCategories(categoryList);

                    // Show or hide empty view based on the result
                    if (categoryList.isEmpty()) {
                        showEmptyView("No categories available");
                    } else {
                        hideEmptyView();
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error loading categories: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if (cursor != null) cursor.close();
                if (db != null && db.isOpen()) db.close();
            }
        });
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

    @Override
    public void onCategoryAdded(Category category) {
        loadCategoriesFromDatabase();
    }

    private void updateCategory(int categoryId, String newName) {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            SQLiteDatabase db = null;
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                dbHelper.copyDatabaseIfNeeded();
                db = dbHelper.getConnection();

                ContentValues values = new ContentValues();
                values.put("name", newName);

                int rowsUpdated = db.update("categories", values, "id = ?", new String[]{String.valueOf(categoryId)});

                requireActivity().runOnUiThread(() -> {
                    if (rowsUpdated > 0) {
                        Toast.makeText(getContext(), "Category updated", Toast.LENGTH_SHORT).show();
                        loadCategoriesFromDatabase();  // Refresh list
                    } else {
                        Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error updating category: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if (db != null && db.isOpen()) db.close();
            }
        });
    }

}