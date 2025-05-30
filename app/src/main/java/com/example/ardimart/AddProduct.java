package com.example.ardimart;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ardimart.config.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class AddProduct extends AppCompatActivity {
    private ImageView imgProduct;
    private Uri selectedImageUri;
    private EditText txtBarcode, txtName, txtStocks, txtPurchasePrice, txtSellPrice;
    private Spinner spinnerUnits, spinnerCategory;
    private Button btnSelectImage;
    private Button btnSaveProduct;
    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private static final int REQUEST_CODE_CAMERA = 102;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add Product");

        txtBarcode = findViewById(R.id.txtBarcode);
        txtName = findViewById(R.id.txtName);
        spinnerUnits = findViewById(R.id.spinnerUnits);
        ArrayAdapter<String> unitsAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Choose Unit", "pcs", "kg", "ltr", "box", "pack"});
        unitsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnits.setAdapter(unitsAdapter);
        spinnerUnits.setAdapter(unitsAdapter);
        txtStocks = findViewById(R.id.txtStocks);
        txtPurchasePrice = findViewById(R.id.txtPurchasePrice);
        txtSellPrice = findViewById(R.id.txtSellPrice);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        imgProduct = findViewById(R.id.imgProduct);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.copyDatabaseIfNeeded();

        SQLiteDatabase db = null;
        Cursor cursor = null;
        List<Category> categoryList = new ArrayList<>();
        categoryList.add(new Category(0, "Choose Category"));

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

        btnSelectImage.setOnClickListener(v -> {
            String[] options = {"Camera", "Gallery"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Image Source")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // Camera option
                            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                                startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA);
                            } else {
                                Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
                            }
                        } else if (which == 1) {
                            // Gallery option
                            Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                            galleryIntent.setType("image/*");
                            startActivityForResult(galleryIntent, REQUEST_CODE_PICK_IMAGE);
                        }
                    })
                    .show();
        });

        btnSaveProduct.setOnClickListener(v -> addProduct());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK_IMAGE && data != null) {
                selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    imgProduct.setImageURI(selectedImageUri);
                }
            } else if (requestCode == REQUEST_CODE_CAMERA && data != null) {
                // Camera returns a small Bitmap in extras under "data"
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null) {
                    imgProduct.setImageBitmap(photo);
                    selectedImageUri = saveImageAndGetUri(photo);
                }
            }
        }
    }
    private Uri saveImageAndGetUri(Bitmap bitmap) {
        // Create a unique file name
        String fileName = "image_" + System.currentTimeMillis() + ".jpg";

        // File in internal storage
        File file = new File(getFilesDir(), fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Compress bitmap to JPEG and save to file
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // Return the Uri from the saved file
        return Uri.fromFile(file);
    }
    private String getImagePath(Uri uri) {
        String fileName = "image_" + System.currentTimeMillis() + ".jpg";
        File tempFile = new File(getFilesDir(), fileName);

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(tempFile)) {

            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return tempFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addProduct() {
        String barcode = txtBarcode.getText().toString().trim();
        String name = txtName.getText().toString().trim();
        String units = spinnerUnits.getSelectedItem().toString();
        String stocksStr = txtStocks.getText().toString().trim();
        String purchasePriceStr = txtPurchasePrice.getText().toString().trim();
        String sellPriceStr = txtSellPrice.getText().toString().trim();
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        int categoryId = selectedCategory.getId();
        String imagePath = getImagePath(selectedImageUri);

        if (barcode.isEmpty() || name.isEmpty() || units.isEmpty() ||
                stocksStr.isEmpty() || purchasePriceStr.isEmpty() || sellPriceStr.isEmpty() ||
                selectedCategory.equals("Choose Category") || units.equals("Choose Unit")) {

            Toast.makeText(this, "Please fill in all fields and select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        int stocks;
        double purchasePrice, sellPrice;

        try {
            stocks = Integer.parseInt(stocksStr);
            purchasePrice = Double.parseDouble(purchasePriceStr);
            sellPrice = Double.parseDouble(sellPriceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for stocks and prices", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(AddProduct.this);
                dbHelper.copyDatabaseIfNeeded();
                SQLiteDatabase db = dbHelper.getConnection();

                String checkSql = "SELECT COUNT(*) FROM products WHERE barcode = ?";
                SQLiteStatement checkStmt = db.compileStatement(checkSql);
                checkStmt.bindString(1, barcode);
                long count = checkStmt.simpleQueryForLong();

                if (count > 0) {
                    // Barcode exists, show error on UI thread
                    runOnUiThread(() -> Toast.makeText(AddProduct.this, "Barcode already exists. Please use a unique barcode.", Toast.LENGTH_SHORT).show());
                    return;
                }

                String sql = "INSERT INTO products (barcode, name, units, category_id, stocks, purchase_price, sell_price, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                SQLiteStatement stmt = db.compileStatement(sql);
                stmt.bindString(1, barcode);
                stmt.bindString(2, name);
                stmt.bindString(3, units);
                stmt.bindString(4, String.valueOf(categoryId));
                stmt.bindLong(5, stocks);
                stmt.bindDouble(6, purchasePrice);
                stmt.bindDouble(7, sellPrice);
                stmt.bindString(8, imagePath);
                stmt.executeInsert();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}
