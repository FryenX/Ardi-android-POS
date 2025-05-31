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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ardimart.config.DatabaseHelper;
import com.example.ardimart.config.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        recyclerTransactionData = view.findViewById(R.id.transactionsRecyclerView);
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
                String invoice = transaction.getInvoice();
                showTransactionDetailDialog(invoice);
            }

            @Override
            public void onDelete(TransactionData transaction) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to delete transaction: " + transaction.getInvoice() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            Executor executor = Executors.newSingleThreadExecutor();
                            executor.execute(() -> {
                                SQLiteDatabase db = null;
                                try {
                                    DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                                    dbHelper.copyDatabaseIfNeeded();
                                    db = dbHelper.getConnection();

                                    String invoice = transaction.getInvoice();

                                    db.beginTransaction();

                                    int detailsDeleted = db.delete("transactions_detail", "invoice = ?", new String[]{invoice});

                                    int transactionsDeleted = db.delete("transactions", "invoice = ?", new String[]{invoice});

                                    db.setTransactionSuccessful();
                                    db.endTransaction();

                                    requireActivity().runOnUiThread(() -> {
                                        if (transactionsDeleted > 0) {
                                            Toast.makeText(getContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                                            loadTransactionDataFromDatabase();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to delete transaction", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } catch (Exception e) {
                                    if (db != null) db.endTransaction();
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), "Error deleting transaction: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                    );
                                } finally {
                                    if (db != null && db.isOpen()) db.close();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        recyclerTransactionData.setAdapter(transactionDataAdapter);

        TextView sortInvoice = view.findViewById(R.id.txtInvoice);
        TextView sortDate = view.findViewById(R.id.txtDate);
        SessionManager session = new SessionManager(requireContext());
        String level = session.getLevel();
        transactionDataAdapter.setUserLevel(level);
        sortInvoice.setOnClickListener(v -> {
            sortByInvoice = !sortByInvoice;
            updateSortIndicators(DataTransactionsFragment.SortField.INVOICE, sortByInvoice);
            sortByInvoice(sortByInvoice);
        });

        sortDate.setOnClickListener(v -> {
            sortByDate = !sortByDate;
            updateSortIndicators(DataTransactionsFragment.SortField.DATE, sortByDate);
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

    private void loadTransactionDataFromDatabase() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<TransactionData> transactionList = new ArrayList<>();
            SQLiteDatabase db = null;
            Cursor cursor = null;

            try {
                DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                dbHelper.copyDatabaseIfNeeded();
                db = dbHelper.getConnection();

                String query = "SELECT t.invoice, t.date_time, c.name AS customerName, " +
                        "t.discount_percent, t.discount_idr, t.gross_total, t.net_total, " +
                        "t.payment_amount, t.payment_change " +
                        "FROM transactions t " +
                        "LEFT JOIN customers c ON t.customer_id = c.id " +
                        "ORDER BY t.date_time DESC";
                cursor = db.rawQuery(query, null);

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String invoice = cursor.getString(cursor.getColumnIndexOrThrow("invoice"));
                        String dateTime = cursor.getString(cursor.getColumnIndexOrThrow("date_time"));
                        String customerName = cursor.getString(cursor.getColumnIndexOrThrow("customerName"));
                        double disc_percent = cursor.getDouble(cursor.getColumnIndexOrThrow("discount_percent"));
                        double disc_idr = cursor.getDouble(cursor.getColumnIndexOrThrow("discount_idr"));
                        double gross_total = cursor.getDouble(cursor.getColumnIndexOrThrow("gross_total"));
                        double net_total = cursor.getDouble(cursor.getColumnIndexOrThrow("net_total"));
                        double payment_amount = cursor.getDouble(cursor.getColumnIndexOrThrow("payment_amount"));
                        double payment_change = cursor.getDouble(cursor.getColumnIndexOrThrow("payment_change"));

                        transactionList.add(new TransactionData(invoice, dateTime, customerName, disc_percent, disc_idr, gross_total,net_total, payment_amount, payment_change));
                    } while (cursor.moveToNext());
                }

                List<TransactionData> finalTransactionList = new ArrayList<>(transactionList);
                requireActivity().runOnUiThread(() -> {
                    transactionDataAdapter.setTransactions(finalTransactionList);
                    updatePageIndicator();
                    if (finalTransactionList.isEmpty()) {
                        showEmptyView("No transactions found");
                    } else {
                        hideEmptyView();
                    }
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error loading transactions: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            } finally {
                if (cursor != null) cursor.close();
                if (db != null && db.isOpen()) db.close();
            }
        });
    }

    private void sortByInvoice(boolean ascending) {
        List<TransactionData> currentList = transactionDataAdapter.getFullList();
        if (ascending) {
            Collections.sort(currentList, Comparator.comparing(TransactionData::getInvoice, String.CASE_INSENSITIVE_ORDER));
        } else {
            Collections.sort(currentList, (c1, c2) -> c2.getInvoice().compareToIgnoreCase(c1.getInvoice()));
        }
        transactionDataAdapter.setTransactions(currentList);
    }

    private void sortByDate(boolean ascending) {
        List<TransactionData> currentList = transactionDataAdapter.getFullList();
        if (ascending) {
            Collections.sort(currentList, Comparator.comparing(TransactionData::getDateTime, String.CASE_INSENSITIVE_ORDER));
        } else {
            Collections.sort(currentList, (c1, c2) -> c2.getDateTime().compareToIgnoreCase(c1.getDateTime()));
        }
        transactionDataAdapter.setTransactions(currentList);
    }
    public enum SortField {
        INVOICE,
        DATE,
        NONE,
    }
    private void updateSortIndicators(DataTransactionsFragment.SortField sortField, boolean ascending) {
        TextView sortInvoice = getView().findViewById(R.id.txtInvoice);
        TextView sortDate = getView().findViewById(R.id.txtDate);

        int upArrow = R.drawable.baseline_arrow_drop_up_24;
        int downArrow = R.drawable.baseline_arrow_drop_down_24;
        int none = R.drawable.ic_sort_none;

        sortInvoice.setCompoundDrawablesWithIntrinsicBounds(0, 0, none, 0);
        sortDate.setCompoundDrawablesWithIntrinsicBounds(0, 0, none, 0);

        switch (sortField) {
            case INVOICE:
                sortInvoice.setCompoundDrawablesWithIntrinsicBounds(0, 0, ascending ? upArrow : downArrow, 0);
                break;
            case DATE:
                sortDate.setCompoundDrawablesWithIntrinsicBounds(0, 0, ascending ? upArrow : downArrow, 0);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void showTransactionDetailDialog(String invoice) {
        Executors.newSingleThreadExecutor().execute(() -> {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                DatabaseHelper helper = new DatabaseHelper(getContext());
                helper.copyDatabaseIfNeeded();
                db = helper.getConnection();

                TransactionData trans = null;
                List<TransactionDetail> details = new ArrayList<>();

                cursor = db.rawQuery("SELECT t.*, c.name as customer_name FROM transactions t " +
                                "LEFT JOIN customers c ON t.customer_id = c.id WHERE t.invoice = ?",
                        new String[]{invoice});
                if (cursor.moveToFirst()) {
                    trans = new TransactionData(
                            cursor.getString(cursor.getColumnIndexOrThrow("invoice")),
                            cursor.getString(cursor.getColumnIndexOrThrow("date_time")),
                            cursor.getString(cursor.getColumnIndexOrThrow("customer_name")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("discount_percent")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("discount_idr")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("gross_total")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("net_total")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("payment_amount")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("payment_change"))
                    );
                }
                cursor.close();

                // Get transaction details
                cursor = db.rawQuery("SELECT td.*, p.name as product_name FROM transactions_detail td " +
                                "LEFT JOIN products p ON td.barcode = p.barcode WHERE td.invoice = ?",
                        new String[]{invoice});
                while (cursor.moveToNext()) {
                    details.add(new TransactionDetail(
                            cursor.getString(cursor.getColumnIndexOrThrow("barcode")),
                            cursor.getString(cursor.getColumnIndexOrThrow("product_name")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("purchase_price")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("qty")),
                            cursor.getDouble(cursor.getColumnIndexOrThrow("sub_total"))
                    ));
                }

                TransactionData finalTrans = trans;
                List<TransactionDetail> finalDetails = details;

                requireActivity().runOnUiThread(() -> {
                    View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_transaction_detail, null);

                    ((TextView) view.findViewById(R.id.txtInvoice)).setText("Invoice: " + finalTrans.getInvoice());
                    ((TextView) view.findViewById(R.id.txtDateTime)).setText("Date: " + finalTrans.getDateTime());
                    ((TextView) view.findViewById(R.id.txtCustomer)).setText("Customer: " + finalTrans.getCustomerName());
                    ((TextView) view.findViewById(R.id.txtDiscount)).setText(String.format("Discount: %.2f%% (%.2f)", finalTrans.getDiscPercent(), finalTrans.getDiscIdr()));
                    ((TextView) view.findViewById(R.id.txtGrossTotal)).setText(String.format("Gross: %.2f", finalTrans.getGrossTotal()));
                    ((TextView) view.findViewById(R.id.txtNetTotal)).setText(String.format("Net: %.2f", finalTrans.getNetTotal()));
                    ((TextView) view.findViewById(R.id.txtPaymentAmount)).setText(String.format("Paid: %.2f", finalTrans.getPaymentAmount()));
                    ((TextView) view.findViewById(R.id.txtPaymentChange)).setText(String.format("Change: %.2f", finalTrans.getPaymentChange()));

                    RecyclerView recycler = view.findViewById(R.id.recyclerDetails);
                    recycler.setLayoutManager(new LinearLayoutManager(getContext()));
                    recycler.setAdapter(new TransactionDetailAdapter(finalDetails));

                    new AlertDialog.Builder(getContext())
                            .setTitle("Transaction Detail")
                            .setView(view)
                            .setPositiveButton("OK", null)
                            .show();
                });

            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
}