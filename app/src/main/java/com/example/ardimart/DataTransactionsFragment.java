package com.example.ardimart;

import android.content.Context;
import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import com.example.ardimart.config.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class DataTransactionsFragment extends Fragment {

    private RecyclerView recyclerTransactionData;
    private TransactionDataAdapter transactionDataAdapter;
    private Button btnPrev;
    private Button btnNext;
    private TextView txtPageIndicator;
    private boolean sortByInvoice = true;
    private boolean sortByDate = true;
    private View emptyView;

    public DataTransactionsFragment() {

    }
    public static DataTransactionsFragment newInstance(String param1, String param2) {
        DataTransactionsFragment fragment = new DataTransactionsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data_transactions, container, false);

        emptyView = view.findViewById(R.id.emptyView);
        recyclerTransactionData = view.findViewById(R.id.recyclerTransactionsDetail);
        recyclerTransactionData.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionDataAdapter = new TransactionDataAdapter();
        EditText searchInput = view.findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                transactionDataAdapter.filter(s.toString());
                updatePageIndicator();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
        txtPageIndicator = view.findViewById(R.id.txtPageIndicator);

        btnPrev.setOnClickListener(v -> {
            int currentPage = transactionDataAdapter.getCurrentPage();
            if (currentPage > 0) {
                transactionDataAdapter.setPage(currentPage - 1);
                updatePageIndicator();
            }
        });

        btnNext.setOnClickListener(v -> {
            int currentPage = transactionDataAdapter.getCurrentPage();
            if (currentPage < transactionDataAdapter.getTotalPages() - 1) {
                transactionDataAdapter.setPage(currentPage + 1);
                updatePageIndicator();
            }
        });

        transactionDataAdapter.setListener(new TransactionDataAdapter.OnTransactionClickListener() {
            @Override
            public void onDetail(TransactionData transaction) {
                Toast.makeText(getContext(), "Detail", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(TransactionData transaction) {
                new AlertDialog.Builder(getContext()).setTitle("Confirm Deletion").setMessage("Are you sure you want to delete user: " + transaction.getInvoice() + "?").setPositiveButton("Delete", (dialog, which) -> {
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        SQLiteDatabase db = null;
                        try {
                            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                            dbHelper.copyDatabaseIfNeeded();
                            db = dbHelper.getConnection();

                            int rowsDeleted = db.delete("transactions", "id = ?", new String[]{String.valueOf(transaction.getInvoice())});

                            requireActivity().runOnUiThread(() -> {
                                if (rowsDeleted > 0) {
                                    Toast.makeText(getContext(), "User deleted", Toast.LENGTH_SHORT).show();
                                    loadTransactionDataFromDatabase();
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

        recyclerTransactionData.setAdapter(transactionDataAdapter);

        TextView sortInvoice = view.findViewById(R.id.txtInvoice);
        TextView sortDate = view.findViewById(R.id.txtDate);

        sortInvoice.setOnClickListener(v -> {
            sortByInvoice = !sortByInvoice;
            updateSortIndicators(DataTransactionsFragment.SortField.INVOICE, sortByInvoice);
            sortByInvoice(sortByInvoice);
        });

        sortDate.setOnClickListener(v -> {
            sortByDate = !sortByDate;
            updateSortIndicators(DataTransactionsFragment.SortField.USERNAME, sortByDate);
            sortByDate(sortByDate);
        });

        loadTransactionDataFromDatabase();

        return view;
    }

    private void updatePageIndicator() {
        int currentPage = transactionDataAdapter.getCurrentPage() + 1;
        int totalPages = transactionDataAdapter.getTotalPages();
        txtPageIndicator.setText("Page " + currentPage + " of " + totalPages);

        btnPrev.setEnabled(currentPage > 1);
        btnNext.setEnabled(currentPage < totalPages);
    }

    private List<TransactionData> getAllTransactions(Context context) {
        List<TransactionData> transactions = new ArrayList<>();

        DatabaseHelper dbHelper = new DatabaseHelper(context);
        dbHelper.copyDatabaseIfNeeded();
        SQLiteDatabase db = dbHelper.getConnection();

        Cursor cursor = db.rawQuery("SELECT * FROM transactions", null);
        if (cursor.moveToFirst()) {
            do {
                String invoice = cursor.getString(cursor.getColumnIndexOrThrow("invoice"));
                String dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date_time"));
                double gross = cursor.getDouble(cursor.getColumnIndexOrThrow("gross_total"));
                double disc = cursor.getDouble(cursor.getColumnIndexOrThrow("discount_idr"));
                double net = cursor.getDouble(cursor.getColumnIndexOrThrow("net_total"));

                transactions.add(new TransactionData(invoice, dateTime, gross, disc, net));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return transactions;
    }
}