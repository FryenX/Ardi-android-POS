package com.example.ardimart;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.ardimart.config.DatabaseHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EditProduct extends AppCompatActivity {
    private Product product;
    private static final int PICK_IMAGE_REQUEST = 1;
    ImageView imgPreview;
    Uri selectedImageUri;
    private final Executor executor = Executors.newSingleThreadExecutor();

    EditText txtBarcode, txtName, txtStock, txtPurchasePrice, txtSellPrice;
    Spinner spinnerCategory, spinnerUnits;
    Button btnSaveProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit Product");

        product = (Product) getIntent().getSerializableExtra("product");

        txtBarcode = findViewById(R.id.txtBarcode);
        txtName = findViewById(R.id.txtName);
        spinnerUnits = findViewById(R.id.spinnerUnits);
        String productUnit = product.getUnits();
        String[] unitOptions = {"Choose Unit", "pcs", "kg", "ltr", "box", "pack"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, unitOptions);
        spinnerUnits.setAdapter(unitAdapter);
        for (int i = 0; i < unitOptions.length; i++) {
            if (unitOptions[i].equalsIgnoreCase(productUnit)) {
                spinnerUnits.setSelection(i);
                break;
            }
        }
        txtStock = findViewById(R.id.txtStock);
        txtPurchasePrice = findViewById(R.id.txtPurchasePrice);
        txtSellPrice = findViewById(R.id.txtSellPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        imgPreview = findViewById(R.id.imgPreview);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);


        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.copyDatabaseIfNeeded();

        SQLiteDatabase db = null;
        Cursor cursor = null;
        List<Category> categoryList = new ArrayList<>();
        categoryList.add(new Category(0, "Choose Category")); // Default item

        try {
            db = dbHelper.getConnection();
            cursor = db.rawQuery("SELECT id, name FROM categories", null);

            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(0);
                    String name = cursor.getString(1);
                    categoryList.add(new Category(id, name));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
            if (db != null && db.isOpen()) db.close();
        }
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (product != null) {
            txtBarcode.setText(product.getBarcode());
            txtBarcode.setEnabled(false);
            txtName.setText(product.getName());
            if (product != null) {
                for (int i = 0; i < categoryList.size(); i++) {
                    if (categoryList.get(i).getId() == product.getCategoryId()) {
                        spinnerCategory.setSelection(i);
                        break;
                    }
                }
            }
            txtStock.setText(String.valueOf(product.getStocks()));
            txtPurchasePrice.setText(String.valueOf(product.getPurchasePrice()));
            txtSellPrice.setText(String.valueOf(product.getSellPrice()));
            String imagePath = product.getImage();
            if (imagePath != null && !imagePath.isEmpty()) {
                File imgFile = new File(imagePath);
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    imgPreview.setImageBitmap(bitmap);
                } else {
                    imgPreview.setImageResource(R.drawable.ic_image_placeholder);
                }
            } else {
                imgPreview.setImageResource(R.drawable.ic_image_placeholder);
            }
        }

        btnSaveProduct.setOnClickListener(v -> updateProduct());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateProduct() {
        String name = txtName.getText().toString().trim();
        String units = spinnerUnits.getSelectedItem().toString();
        String stockStr = txtStock.getText().toString().trim();
        String purchasePriceStr = txtPurchasePrice.getText().toString().trim();
        String sellPriceStr = txtSellPrice.getText().toString().trim();


        int categoryId = product.getCategoryId();

        if (name.isEmpty() || units.isEmpty() || stockStr.isEmpty() || purchasePriceStr.isEmpty() || sellPriceStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double stocks = Double.parseDouble(stockStr);
        double purchasePrice = Double.parseDouble(purchasePriceStr);
        double sellPrice = Double.parseDouble(sellPriceStr);

        executor.execute(() -> {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(EditProduct.this);
                dbHelper.copyDatabaseIfNeeded();
                SQLiteDatabase db = dbHelper.getConnection();

                String sql = "UPDATE products SET name = ?, units = ?, category_id = ?, stocks = ?, purchase_price = ?, sell_price = ? WHERE barcode = ?";
                SQLiteStatement stmt = db.compileStatement(sql);
                stmt.bindString(1, name);
                stmt.bindString(2, units);
                stmt.bindLong(3, categoryId);
                stmt.bindDouble(4, stocks);
                stmt.bindDouble(5, purchasePrice);
                stmt.bindDouble(6, sellPrice);
                stmt.bindString(7, product.getBarcode());

                int rowsAffected = stmt.executeUpdateDelete();

                runOnUiThread(() -> {
                    if (rowsAffected > 0) {
                        Toast.makeText(EditProduct.this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(EditProduct.this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(EditProduct.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}