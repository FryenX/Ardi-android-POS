package com.example.ardimart;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ardimart.config.DatabaseHelper;
import com.example.ardimart.config.SessionManager;
import com.example.ardimart.Product;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InputTransaction extends AppCompatActivity implements TransactionAdapter.OnTransactionChangeListener{
    private SessionManager sessionManager;
    private EditText txtInvoice, txtDate, txtCustomer, txtBarcode, txtProduct, txtQty, txtTotal;
    private ImageButton btnDelete, btnSave;
    private Button btnAddItem;
    private RecyclerView recyclerTransactionsDetail;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_transaction);
        sessionManager = new SessionManager(InputTransaction.this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Input Transaction");

        txtInvoice = findViewById(R.id.txtInvoice);
        txtDate = findViewById(R.id.txtDate);
        txtCustomer = findViewById(R.id.txtCustomer);
        txtBarcode = findViewById(R.id.txtBarcode);
        txtProduct = findViewById(R.id.txtProduct);
        txtQty = findViewById(R.id.txtQty);
        txtTotal = findViewById(R.id.txtTotal);

        btnDelete = findViewById(R.id.btnDelete);
        btnSave = findViewById(R.id.btnSave);
        btnAddItem = findViewById(R.id.btnAddItem);
        generateInvoice(txtInvoice);
        txtDate.setText(android.text.format.DateFormat.format("yyyy-MM-dd", new Date()));

        txtBarcode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                checkCode(txtBarcode.getText().toString().trim());
                return true;
            }
            return false;
        });

        btnAddItem.setOnClickListener(view -> addItem());

        btnDelete.setOnClickListener(view -> clearTemp());

        btnSave.setOnClickListener(view -> {
            showPaymentDialog();
        });
    }

    private void showPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment, null);

        EditText edtDiscountPercent = dialogView.findViewById(R.id.edtDiscountPercent);
        EditText edtDiscountIDR = dialogView.findViewById(R.id.edtDiscountIdr);
        EditText edtNetTotal = dialogView.findViewById(R.id.edtNetTotal);
        EditText edtPaymentAmount = dialogView.findViewById(R.id.edtPaymentAmount);
        EditText edtPaymentChange = dialogView.findViewById(R.id.edtPaymentChange);
        TextView tvPaymentAmount =  dialogView.findViewById(R.id.tvPaymentAmount);
        TextView tvChange =  dialogView.findViewById(R.id.tvChange);
        Button btnCash = dialogView.findViewById(R.id.btnCash);
        Button btnTransfer = dialogView.findViewById(R.id.btnTransfer);
        double grossTotal = getCurrentGrossTotal();
        edtNetTotal.setText(String.format("%.2f", grossTotal));
        btnCash.setOnClickListener(v -> {
            edtPaymentAmount.setVisibility(View.VISIBLE);
            tvPaymentAmount.setVisibility(View.VISIBLE);
            tvChange.setVisibility(View.VISIBLE);
        });

        btnTransfer.setOnClickListener(v -> {
            edtPaymentAmount.setVisibility(View.GONE);
            tvPaymentAmount.setVisibility(View.GONE);
            tvChange.setVisibility(View.GONE);

        });
        builder.setView(dialogView)
                .setTitle("Enter Transaction Details")
                .setPositiveButton("Save", (dialog, which) -> {
                    double discountPercent = edtDiscountPercent.getText().toString().trim().isEmpty()
                            ? 0 : Double.parseDouble(edtDiscountPercent.getText().toString().trim());

                    double discountIDR = edtDiscountIDR.getText().toString().trim().isEmpty()
                            ? 0 : Double.parseDouble(edtDiscountIDR.getText().toString().trim());
                    double netTotal = Double.parseDouble(edtNetTotal.getText().toString().trim());
                    double paymentAmount = Double.parseDouble(edtPaymentAmount.getText().toString().trim());
                    double paymentChange = paymentAmount - netTotal;

                    // Save transaction logic here, passing paymentChange as well
                    saveTransactionWithDetails(discountPercent, discountIDR, netTotal, paymentAmount, paymentChange);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            public void afterTextChanged(Editable s) {
                double net = 0, pay = 0;
                try {
                    net = Double.parseDouble(edtNetTotal.getText().toString());
                } catch (NumberFormatException ignored) {}

                try {
                    pay = Double.parseDouble(edtPaymentAmount.getText().toString());
                } catch (NumberFormatException ignored) {}

                double change = pay - net;
                edtPaymentChange.setText(String.format("%.2f", change >= 0 ? change : 0));
            }
        };

        edtNetTotal.addTextChangedListener(watcher);
        edtPaymentAmount.addTextChangedListener(watcher);

        dialog.show();
    }

    private void saveTransactionWithDetails(double discountPercent, double discountIdr, double netTotal, double paymentAmount, double paymentChange) {
        String invoice = txtInvoice.getText().toString().trim();
        String dateTime = getCurrentDateTime();
        int customerId = 0;
        double grossTotal = getCurrentGrossTotal();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();

        db.beginTransaction();
        try {
            ContentValues transValues = new ContentValues();
            transValues.put("invoice", invoice);
            transValues.put("date_time", dateTime);
            transValues.put("customer_id", customerId);
            transValues.put("discount_percent", discountPercent);
            transValues.put("discount_idr", discountIdr);
            transValues.put("gross_total", grossTotal);
            transValues.put("net_total", netTotal);
            transValues.put("payment_amount", paymentAmount);
            transValues.put("payment_change", paymentChange);

            long transResult = db.insert("transactions", null, transValues);
            if (transResult == -1) throw new Exception("Failed to insert transaction");

            Cursor cursor = db.rawQuery("SELECT barcode, qty, sell_price, subtotal FROM temp_transactions WHERE invoice = ?", new String[]{invoice});
            while (cursor.moveToNext()) {
                ContentValues detailValues = new ContentValues();
                detailValues.put("invoice", invoice);
                detailValues.put("barcode", cursor.getString(cursor.getColumnIndexOrThrow("barcode")));
                detailValues.put("qty", cursor.getInt(cursor.getColumnIndexOrThrow("qty")));
                detailValues.put("sell_price", cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price")));
                detailValues.put("sub_total", cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal")));

                long detailResult = db.insert("transactions_detail", null, detailValues);
                if (detailResult == -1) throw new Exception("Failed to insert transaction detail");
            }
            cursor.close();

            db.delete("temp_transactions", "invoice = ?", new String[]{invoice});

            db.setTransactionSuccessful();

            Toast.makeText(this, "Transaction saved successfully", Toast.LENGTH_SHORT).show();

            loadTransactionItems();
            calculateTotal();
            clearInputFields();

        } catch (Exception e) {
            Toast.makeText(this, "Error saving transaction: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private double getCurrentGrossTotal() {
        String invoice = txtInvoice.getText().toString().trim();
        double grossTotal = 0;
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();

        Cursor cursor = db.rawQuery("SELECT SUM(subtotal) AS total FROM temp_transactions WHERE invoice = ?", new String[]{invoice});
        if(cursor.moveToFirst()) {
            grossTotal = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
        }
        cursor.close();
        db.close();
        return grossTotal;
    }
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
    private void checkCode(String barcode) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.copyDatabaseIfNeeded();
        SQLiteDatabase db = dbHelper.getConnection();

        recyclerTransactionsDetail = findViewById(R.id.recyclerTransactionsDetail);
        recyclerTransactionsDetail.setLayoutManager(new LinearLayoutManager(this));

        Cursor cursor = db.rawQuery(
                "SELECT p.barcode, p.name, p.stocks, p.sell_price, c.name AS category_name " +
                        "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                        "WHERE p.barcode = ?", new String[]{barcode});

        if (cursor.moveToFirst()) {
            Product product = new Product();
            product.setBarcode(cursor.getString(cursor.getColumnIndexOrThrow("barcode")));
            product.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            product.setSellPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price")));
            product.setStocks(cursor.getInt(cursor.getColumnIndexOrThrow("stocks")));
            product.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow("category_name")));

            txtProduct.setText(product.getName());
            txtQty.setText("1");
            txtQty.requestFocus();
            txtQty.selectAll();

            cursor.close();
            db.close();
            return;
        }
        cursor.close();

        Cursor likeCursor = db.rawQuery(
                "SELECT p.barcode, p.name, p.stocks, p.sell_price, c.name AS category_name " +
                        "FROM products p LEFT JOIN categories c ON p.category_id = c.id " +
                        "WHERE p.barcode LIKE ?", new String[]{"%" + barcode + "%"});

        List<Product> filteredProducts = new ArrayList<>();
        while (likeCursor.moveToNext()) {
            Product product = new Product();
            product.setBarcode(likeCursor.getString(likeCursor.getColumnIndexOrThrow("barcode")));
            product.setName(likeCursor.getString(likeCursor.getColumnIndexOrThrow("name")));
            product.setSellPrice(likeCursor.getDouble(likeCursor.getColumnIndexOrThrow("sell_price")));
            product.setStocks(likeCursor.getInt(likeCursor.getColumnIndexOrThrow("stocks")));
            product.setCategoryName(likeCursor.getString(likeCursor.getColumnIndexOrThrow("category_name")));
            filteredProducts.add(product);
        }

        likeCursor.close();
        db.close();

        if (filteredProducts.size() == 1) {
            // Auto-fill if only one result
            Product product = filteredProducts.get(0);
            txtBarcode.setText(product.getBarcode());
            txtProduct.setText(product.getName());
            txtQty.setText("1");
            txtQty.requestFocus();
            txtQty.selectAll();
        } else if (!filteredProducts.isEmpty()) {
            // Show dialog if multiple matches
            showProductDialog(filteredProducts);
        } else {
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
        }
    }



    private List<Product> getAllProducts() {
        List<Product> productList = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();

        Cursor cursor = db.rawQuery(
                "SELECT p.barcode, p.name, p.stocks, p.sell_price, c.name AS category_name " +
                        "FROM products p LEFT JOIN categories c ON p.category_id = c.id", null
        );

        while (cursor.moveToNext()) {
            Product product = new Product();
            product.setBarcode(cursor.getString(cursor.getColumnIndexOrThrow("barcode")));
            product.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            product.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow("category_name")));
            product.setStocks(cursor.getDouble(cursor.getColumnIndexOrThrow("stocks")));
            product.setSellPrice(cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price")));
            productList.add(product);
        }

        cursor.close();
        db.close();
        return productList;
    }

    private void showProductDialog(List<Product> products) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Product");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        SearchView searchView = new SearchView(this);
        searchView.setIconifiedByDefault(false);

        ListView listView = new ListView(this);

        // Pagination controls layout
        LinearLayout paginationLayout = new LinearLayout(this);
        paginationLayout.setOrientation(LinearLayout.HORIZONTAL);
        paginationLayout.setGravity(Gravity.CENTER);

        Button btnPrev = new Button(this);
        btnPrev.setText("Previous");
        Button btnNext = new Button(this);
        btnNext.setText("Next");

        paginationLayout.addView(btnPrev);
        paginationLayout.addView(btnNext);

        layout.addView(searchView);
        layout.addView(listView);
        layout.addView(paginationLayout);

        builder.setView(layout);
        AlertDialog dialog = builder.create();

        // Constants
        final int PAGE_SIZE = 5;
        final List<Product> filteredList = new ArrayList<>(products);

        // Current page index (starting at 0)
        final int[] currentPage = {0};

        // Adapter data subset for current page
        List<Product> pageList = new ArrayList<>();

        ArrayAdapter<Product> adapter = new ArrayAdapter<Product>(this,
                android.R.layout.simple_list_item_2, android.R.id.text1, pageList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Product product = getItem(position);
                View view = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);

                String line1 = product.getBarcode() + " - " + product.getName();
                String line2 = "Category: " + product.getCategoryName() +
                        " | Stock: " + product.getStocks() +
                        " | Price: Rp" + product.getSellPrice();

                ((TextView) view.findViewById(android.R.id.text1)).setText(line1);
                ((TextView) view.findViewById(android.R.id.text2)).setText(line2);

                return view;
            }
        };

        listView.setAdapter(adapter);

        Runnable updatePage = () -> {
            int start = currentPage[0] * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, filteredList.size());
            pageList.clear();
            if (start < end) {
                pageList.addAll(filteredList.subList(start, end));
            }
            adapter.notifyDataSetChanged();

            // Enable/disable buttons based on page
            btnPrev.setEnabled(currentPage[0] > 0);
            btnNext.setEnabled(end < filteredList.size());
        };

        // Initial page update
        updatePage.run();

        // Search filter
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filteredList.clear();
                if (newText.isEmpty()) {
                    filteredList.addAll(products);
                } else {
                    String lower = newText.toLowerCase();
                    for (Product p : products) {
                        if (p.getBarcode().toLowerCase().contains(lower) ||
                                p.getName().toLowerCase().contains(lower) ||
                                p.getCategoryName().toLowerCase().contains(lower)) {
                            filteredList.add(p);
                        }
                    }
                }
                currentPage[0] = 0; // Reset to first page on search change
                updatePage.run();
                return true;
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentPage[0] > 0) {
                currentPage[0]--;
                updatePage.run();
            }
        });

        btnNext.setOnClickListener(v -> {
            int maxPage = (filteredList.size() - 1) / PAGE_SIZE;
            if (currentPage[0] < maxPage) {
                currentPage[0]++;
                updatePage.run();
            }
        });

        listView.setOnItemClickListener((parent1, view1, position, id) -> {
            Product product = pageList.get(position);
            txtBarcode.setText(product.getBarcode());
            txtProduct.setText(product.getName());
            txtQty.setText("1");

            dialog.dismiss();
            txtQty.requestFocus();
            txtQty.selectAll();
        });

        dialog.show();
    }

    private void addItem() {
        String invoice = txtInvoice.getText().toString().trim();
        String barcode = txtBarcode.getText().toString().trim();
        String qtyText = txtQty.getText().toString().trim();

        if (barcode.isEmpty() || qtyText.isEmpty()) {
            Toast.makeText(this, "Please scan/select a product and enter quantity", Toast.LENGTH_SHORT).show();
            return;
        }

        int qty = Integer.parseInt(qtyText);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();

        Cursor cursor = db.rawQuery("SELECT sell_price, stocks FROM products WHERE barcode = ?", new String[]{barcode});
        if (cursor.moveToFirst()) {
            double sell_price = cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price"));
            int currentStock = cursor.getInt(cursor.getColumnIndexOrThrow("stocks"));

            if (qty > currentStock) {
                cursor.close();
                db.close();
                Toast.makeText(this, "Not enough stock", Toast.LENGTH_SHORT).show();
                return;
            }

            double subtotal = sell_price * qty;

            ContentValues values = new ContentValues();
            values.put("invoice", invoice);
            values.put("barcode", barcode);
            values.put("qty", qty);
            values.put("sell_price", sell_price);
            values.put("subtotal", subtotal);
            db.insert("temp_transactions", null, values);

            // Reduce stock in products table
            int updatedStock = currentStock - qty;
            ContentValues stockUpdate = new ContentValues();
            stockUpdate.put("stocks", updatedStock);
            db.update("products", stockUpdate, "barcode = ?", new String[]{barcode});

            cursor.close();
            db.close();

            Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
            loadTransactionItems();
            calculateTotal();
            clearInputFields();
        } else {
            cursor.close();
            db.close();
            Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
        }
    }


    private void clearTemp() {
        String invoice = txtInvoice.getText().toString().trim();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();

        Cursor cursor = db.rawQuery("SELECT barcode, qty FROM temp_transactions WHERE invoice = ?", new String[]{invoice});

        while (cursor.moveToNext()) {
            String barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode"));
            int qtyToRestore = cursor.getInt(cursor.getColumnIndexOrThrow("qty"));

            Cursor stockCursor = db.rawQuery("SELECT stocks FROM products WHERE barcode = ?", new String[]{barcode});
            if (stockCursor.moveToFirst()) {
                int currentStock = stockCursor.getInt(stockCursor.getColumnIndexOrThrow("stocks"));
                int newStock = currentStock + qtyToRestore;

                ContentValues values = new ContentValues();
                values.put("stocks", newStock);
                db.update("products", values, "barcode = ?", new String[]{barcode});
            }
            stockCursor.close();
        }
        cursor.close();

        db.delete("temp_transactions", "invoice = ?", new String[]{invoice});
        db.close();

        Toast.makeText(this, "Transaction items cleared and stock restored", Toast.LENGTH_SHORT).show();

        loadTransactionItems();
        calculateTotal();
    }


    private void loadTransactionItems() {
        String invoice = txtInvoice.getText().toString().trim();
        List<Transaction> itemList = new ArrayList<>();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();

        Cursor cursor = db.rawQuery(
                "SELECT t.id, t.barcode, p.name, t.qty, t.sell_price, t.subtotal " +
                        "FROM temp_transactions t " +
                        "JOIN products p ON t.barcode = p.barcode " +
                        "WHERE t.invoice = ?", new String[]{invoice}
        );

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            int qty = cursor.getInt(cursor.getColumnIndexOrThrow("qty"));
            double sell_price = cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price"));
            double subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow("subtotal"));

            itemList.add(new Transaction(id, barcode, name, qty, sell_price, subtotal));
        }

        cursor.close();
        db.close();

        TransactionAdapter adapter = new TransactionAdapter(itemList, this);
        recyclerTransactionsDetail.setAdapter(adapter);
        calculateTotal();
    }

    public void onTransactionChanged() {
        calculateTotal();
    }
    private void clearInputFields() {
        txtBarcode.setText("");
        txtProduct.setText("");
        txtQty.setText("1");
        txtBarcode.requestFocus();
    }

    private void calculateTotal() {
        String invoice = txtInvoice.getText().toString().trim();
        double total = 0;

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(subtotal) AS total FROM temp_transactions WHERE invoice = ?",
                new String[]{invoice});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));
        }

        cursor.close();
        db.close();

        txtTotal.setText(String.format(Locale.US, "%.2f", total));
    }

    private void generateInvoice(EditText txtInvoice) {
        Date currentDate = new Date();
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = dateFormatter.format(currentDate);

        String query = "SELECT MAX(invoice) AS noInvoice FROM transactions WHERE strftime('%Y-%m-%d', date_time) = ?";
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.copyDatabaseIfNeeded();
        SQLiteDatabase db = dbHelper.getConnection();
        Cursor cursor = db.rawQuery(query, new String[]{formattedDate});

        String lastInvoice = null;
        if (cursor.moveToFirst()) {
            lastInvoice = cursor.getString(cursor.getColumnIndexOrThrow("noInvoice"));
        }
        cursor.close();

        String userUuid = sessionManager.getUuid();
        String uuidSuffix = userUuid.length() >= 3 ? userUuid.substring(userUuid.length() - 3) : userUuid;

        String newInvoice;
        if (lastInvoice != null && !lastInvoice.isEmpty()) {
            String lastNum = lastInvoice.substring(lastInvoice.length() - 4);
            int nextNum = Integer.parseInt(lastNum) + 1;
            String formattedNextNum = String.format("%04d", nextNum);
            newInvoice = "T" + formattedDate.replace("-", "") + uuidSuffix + formattedNextNum;
        } else {
            newInvoice = "T" + formattedDate.replace("-", "") + uuidSuffix + "0001";
        }

        txtInvoice.setText(newInvoice);
        txtInvoice.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
