package com.example.ardimart;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dantsu.escposprinter.EscPosCharsetEncoding;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection;
import com.example.ardimart.config.DatabaseHelper;
import com.example.ardimart.config.SessionManager;
import com.example.ardimart.Product;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class InputTransaction extends AppCompatActivity implements TransactionAdapter.OnTransactionChangeListener{
    private SessionManager sessionManager;
    private EditText txtInvoice, txtDate, txtCustomer, txtBarcode, txtProduct, txtQty, txtTotal;
    private String lastInvoice;
    private ImageButton btnDelete, btnSave;
    private Button btnAddItem;
    private RecyclerView recyclerTransactionsDetail;
    private AlertDialog dialog;
    private BluetoothDevice selectedPrinterDevice;

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
            if (getTempTransactionCount() > 0) {
                showPaymentDialog();
            } else {
                Toast.makeText(this, "No items to save!", Toast.LENGTH_SHORT).show();
            }
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
        TextView tvPaymentAmount = dialogView.findViewById(R.id.tvPaymentAmount);
        TextView tvChange = dialogView.findViewById(R.id.tvChange);
        Button btnCash = dialogView.findViewById(R.id.btnCash);
        Button btnTransfer = dialogView.findViewById(R.id.btnTransfer);

        double grossTotal = getCurrentGrossTotal();
        edtNetTotal.setText(String.format("%.2f", grossTotal));

        Runnable updateNetTotal = () -> {
            double discountPercent = 0;
            double discountIDR = 0;

            try {
                discountPercent = Double.parseDouble(edtDiscountPercent.getText().toString());
            } catch (NumberFormatException ignored) {}

            try {
                discountIDR = Double.parseDouble(edtDiscountIDR.getText().toString());
            } catch (NumberFormatException ignored) {}

            double calculatedDiscount = (grossTotal * discountPercent / 100.0) + discountIDR;
            double net = grossTotal - calculatedDiscount;

            runOnUiThread(() -> edtNetTotal.setText(String.format("%.2f", net >= 0 ? net : 0)));
        };

        TextWatcher discountWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                updateNetTotal.run();
            }
        };

        edtDiscountPercent.addTextChangedListener(discountWatcher);
        edtDiscountIDR.addTextChangedListener(discountWatcher);

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
                    try {
                        double discountPercent = edtDiscountPercent.getText().toString().trim().isEmpty()
                                ? 0 : Double.parseDouble(edtDiscountPercent.getText().toString().trim());

                        double discountIDR = edtDiscountIDR.getText().toString().trim().isEmpty()
                                ? 0 : Double.parseDouble(edtDiscountIDR.getText().toString().trim());

                        double netTotal = Double.parseDouble(edtNetTotal.getText().toString().trim());
                        double paymentAmount = Double.parseDouble(edtPaymentAmount.getText().toString().trim());
                        double paymentChange = paymentAmount - netTotal;

                        // Save transaction in background thread to avoid blocking UI
                        new Thread(() -> {
                            boolean success = saveTransactionWithDetailsThreadSafe(discountPercent, discountIDR, netTotal, paymentAmount, paymentChange);
                            runOnUiThread(() -> {
                                if (success) {
                                    lastInvoice = txtInvoice.getText().toString();
                                    showPrintConfirmationDialog(lastInvoice);
                                    loadTransactionItems();
                                    generateInvoice(txtInvoice);
                                    calculateTotal();
                                } else {
                                    Toast.makeText(this, "Failed to save transaction. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).start();

                        dialog.dismiss();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                    }
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
    private boolean saveTransactionWithDetailsThreadSafe(double discountPercent, double discountIDR,
                                                         double netTotal, double paymentAmount, double paymentChange) {
        String invoice = txtInvoice.getText().toString().trim();
        String dateTime = getCurrentDateTime();
        int customerId = 0;
        double grossTotal = getCurrentGrossTotal();

        SQLiteDatabase db = null;
        int maxRetries = 3;
        int retryDelay = 100;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                db = dbHelper.getConnection();
                db.beginTransaction();

                ContentValues values = new ContentValues();
                values.put("invoice", invoice);
                values.put("date_time", dateTime);
                values.put("customer_id", customerId);
                values.put("discount_percent", discountPercent);
                values.put("discount_idr", discountIDR);
                values.put("gross_total", grossTotal);
                values.put("net_total", netTotal);
                values.put("payment_amount", paymentAmount);
                values.put("payment_change", paymentChange);

                long transactionResult  = db.insert("transactions", null, values);

                if (transactionResult != -1) {
                    Cursor cursor = db.rawQuery("SELECT barcode, purchase_price, sell_price, qty, subtotal FROM temp_transactions", null);

                    boolean detailsInserted = true;
                    while (cursor.moveToNext()) {
                        ContentValues detailValues = new ContentValues();
                        detailValues.put("invoice", invoice);
                        detailValues.put("barcode", cursor.getString(0));
                        detailValues.put("purchase_price", cursor.getDouble(1));
                        detailValues.put("sell_price", cursor.getDouble(2));
                        detailValues.put("qty", cursor.getDouble(3));
                        detailValues.put("sub_total", cursor.getDouble(4));

                        long detailResult = db.insert("transactions_detail", null, detailValues);
                        if (detailResult == -1) {
                            detailsInserted = false;
                            break;
                        }
                    }
                    cursor.close();

                    if (detailsInserted) {
                        db.delete("temp_transactions", null, null);

                        db.setTransactionSuccessful();
                        return true;
                    }
                }

            } catch (SQLiteDatabaseLockedException e) {
                Log.w("Database", "Database locked, attempt " + (attempt + 1) + "/" + maxRetries);
                try {
                    Thread.sleep(retryDelay * (attempt + 1)); // Progressive delay
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                Log.e("Database", "Error saving transaction", e);
                break;
            } finally {
                if (db != null) {
                    try {
                        if (db.inTransaction()) {
                            db.endTransaction();
                        }
                    } catch (Exception e) {
                        Log.e("Database", "Error ending transaction", e);
                    }
                }
            }
        }

        return false;
    }

    private void showPrintConfirmationDialog(String lastInvoice) {
        new AlertDialog.Builder(this)
                .setTitle("Print Invoice")
                .setMessage("Do you want to print the invoice?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Call your printing function here
                    printInvoice(lastInvoice);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showPairedDevicesDialog() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "No paired Bluetooth devices found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] deviceNames = new String[pairedDevices.size()];
        BluetoothDevice[] devices = new BluetoothDevice[pairedDevices.size()];
        int i = 0;
        for (BluetoothDevice device : pairedDevices) {
            deviceNames[i] = device.getName() + " (" + device.getAddress() + ")";
            devices[i] = device;
            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Bluetooth Printer")
                .setItems(deviceNames, (dialog, which) -> {
                    selectedPrinterDevice = devices[which];
                    Toast.makeText(this, "Selected printer: " + selectedPrinterDevice.getName(), Toast.LENGTH_SHORT).show();
                    // Now call printInvoice with the selected printer
                    printInvoice(lastInvoice);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void printInvoice(String lastInvoice) {
        if (selectedPrinterDevice == null) {
            showPairedDevicesDialog();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> permissions = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[0]), 101);
                return;
            }
        }

        // Perform DB and print in background thread
        new Thread(() -> {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(this);
                SQLiteDatabase db = dbHelper.getConnection();

                String storeName = "Pande Ardi";
                String storeAddress = "Jl. Arjuna, Darma Kaja";

                String dateTime = "";
                String customer = "-";
                List<String> items = new ArrayList<>();
                double discountPercent = 0;
                double discountIDR = 0;
                double grossTotal = 0;
                double netTotal = 0;

                Cursor transCursor = db.rawQuery(
                        "SELECT date_time, customer_id, discount_percent, discount_idr, gross_total, net_total FROM transactions WHERE invoice = ?",
                        new String[]{lastInvoice}
                );

                if (transCursor.moveToFirst()) {
                    dateTime = transCursor.getString(transCursor.getColumnIndexOrThrow("date_time"));
                    int customerId = transCursor.getInt(transCursor.getColumnIndexOrThrow("customer_id"));
                    discountPercent = transCursor.getDouble(transCursor.getColumnIndexOrThrow("discount_percent"));
                    discountIDR = transCursor.getDouble(transCursor.getColumnIndexOrThrow("discount_idr"));
                    grossTotal = transCursor.getDouble(transCursor.getColumnIndexOrThrow("gross_total"));
                    netTotal = transCursor.getDouble(transCursor.getColumnIndexOrThrow("net_total"));

                    customer = customerId == 0 ? "-" : "Customer ID: " + customerId;
                }
                transCursor.close();

                Cursor detailCursor = db.rawQuery(
                        "SELECT barcode, qty, sell_price, sub_total FROM transactions_detail WHERE invoice = ?",
                        new String[]{lastInvoice}
                );

                while (detailCursor.moveToNext()) {
                    String barcode = detailCursor.getString(detailCursor.getColumnIndexOrThrow("barcode"));
                    double qty = detailCursor.getDouble(detailCursor.getColumnIndexOrThrow("qty"));
                    double sellPrice = detailCursor.getDouble(detailCursor.getColumnIndexOrThrow("sell_price"));
                    double subTotal = detailCursor.getDouble(detailCursor.getColumnIndexOrThrow("sub_total"));

                    String productName = getProductName(barcode);
                    String units = getProductUnit(barcode);
                    items.add(String.format("[L]%s x%.0f %s [R]%.2f [R]%.2f", productName, qty, units, sellPrice, subTotal));
                }
                detailCursor.close();

                StringBuilder invoiceText = new StringBuilder();
                invoiceText.append("[C]<u><font size='big'>").append(storeName).append("</font></u>\n");
                invoiceText.append("[C]").append(storeAddress).append("\n");
                invoiceText.append("[L]Invoice: ").append(lastInvoice).append("\n");
                invoiceText.append("[L]Date: ").append(dateTime).append("\n");
                invoiceText.append("[L]Customer: ").append(customer).append("\n");
                invoiceText.append("[C]-----------------------------\n");

                for (String line : items) {
                    invoiceText.append(line).append("\n");
                }

                invoiceText.append("[C]-----------------------------\n");
                invoiceText.append(String.format("[L]Gross Total [R]%.2f\n", grossTotal));
                invoiceText.append(String.format("[L]Disc %% [R]%.2f%%\n", discountPercent));
                invoiceText.append(String.format("[L]Disc IDR [R]%.2f\n", discountIDR));
                invoiceText.append(String.format("[L]Net Total [R]%.2f\n", netTotal));
                invoiceText.append("[C]Thank you!\n");

                // Attempt connection and print
                BluetoothConnection printerConnection = new BluetoothConnection(selectedPrinterDevice);
                EscPosPrinter printer = new EscPosPrinter(
                        printerConnection,
                        203,
                        48f,
                        32,
                        new EscPosCharsetEncoding("windows-1252", 16)
                );

                printer.printFormattedText(invoiceText.toString());

            } catch (final Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to print: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private String getProductName(String barcode) {
        String productName = barcode;
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();
        Cursor cursor = db.rawQuery("SELECT name FROM products WHERE barcode = ?", new String[]{barcode});
        if (cursor.moveToFirst()) {
            productName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        }
        cursor.close();

        return productName;
    }

    private String getProductUnit(String barcode) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getConnection();

        Cursor cursor = db.rawQuery("SELECT units FROM products WHERE barcode = ?", new String[]{barcode});
        if (cursor.moveToFirst()) {
            String unit = cursor.getString(cursor.getColumnIndexOrThrow("units"));
            cursor.close();
            return unit;
        } else {
            cursor.close();
            return "";
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

        Cursor cursor = db.rawQuery("SELECT purchase_price, sell_price, stocks FROM products WHERE barcode = ?", new String[]{barcode});
        if (cursor.moveToFirst()) {
            double purchase_price = cursor.getDouble(cursor.getColumnIndexOrThrow("purchase_price"));
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
            values.put("purchase_price", purchase_price);
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

    private int getTempTransactionCount() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.copyDatabaseIfNeeded();
        SQLiteDatabase db = dbHelper.getConnection();
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM temp_transactions", null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return count;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        String invoice = String.valueOf(txtInvoice.getText());
        if (requestCode == 101) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                printInvoice(lastInvoice);
            } else {
                Toast.makeText(this, "Bluetooth permissions are required to print", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
