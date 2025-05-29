package com.example.ardimart;

import android.app.Dialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.ardimart.config.DatabaseHelper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AddCategoryDialog extends DialogFragment {

    public interface AddCategoryListener {
        void onCategoryAdded(Category category);
    }

    private EditText txtCategoryName;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Add Category");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_category, null);
        txtCategoryName = view.findViewById(R.id.txtCategoryName); // update ID if needed
        txtCategoryName.requestFocus();
        builder.setView(view);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = txtCategoryName.getText().toString().trim();
            if (!name.isEmpty()) {
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    SQLiteDatabase db = null;
                    try {
                        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                        dbHelper.copyDatabaseIfNeeded();
                        db = dbHelper.getConnection();

                        // Check if category name already exists
                        String query = "SELECT id FROM categories WHERE LOWER(name) = LOWER(?)";
                        try (Cursor cursor = db.rawQuery(query, new String[]{name})) {
                            if (cursor.moveToFirst()) {
                                // Name already exists
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Category name already exists", Toast.LENGTH_SHORT).show()
                                );
                                return;
                            }
                        }

                        // Insert if unique
                        ContentValues values = new ContentValues();
                        values.put("name", name);
                        long newId = db.insert("categories", null, values);

                        if (newId != -1) {
                            Category newCategory = new Category((int) newId, name);
                            requireActivity().runOnUiThread(() -> {
                                if (getParentFragment() instanceof AddCategoryListener) {
                                    ((AddCategoryListener) getParentFragment()).onCategoryAdded(newCategory);
                                }
                                Toast.makeText(getContext(), "Category added!", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Failed to add category", Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (Exception e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "DB Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    } finally {
                        if (db != null && db.isOpen()) {
                            db.close();
                        }
                    }
                });
            } else {
                Toast.makeText(getContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        return builder.create();
    }
}