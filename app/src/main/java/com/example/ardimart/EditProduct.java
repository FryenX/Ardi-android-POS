package com.example.ardimart;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.ardimart.config.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EditProduct extends AppCompatActivity {
    private Product product;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
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
        Button btnChangeImage = findViewById(R.id.btnChangeImage);
        btnChangeImage.setOnClickListener(v -> showImagePickDialog());
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

    private void showImagePickDialog() {
        String[] options = {"Camera", "Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Select Image From")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                }).show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                imgPreview.setImageURI(selectedImageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                // Small bitmap from camera
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                if (photo != null) {
                    imgPreview.setImageBitmap(photo);
                    selectedImageUri = saveImageAndGetUri(photo);
                }
            }
        }
    }

    private Uri saveImageAndGetUri(Bitmap bitmap) {
        File file = new File(getFilesDir(), "image_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return Uri.fromFile(file);
    }

    private void updateProduct() {
        String name = txtName.getText().toString().trim();
        String units = spinnerUnits.getSelectedItem().toString();
        String stockStr = txtStock.getText().toString().trim();
        String purchasePriceStr = txtPurchasePrice.getText().toString().trim();
        String sellPriceStr = txtSellPrice.getText().toString().trim();
        Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
        int categoryId = selectedCategory.getId();

        if (name.isEmpty() || units.isEmpty() || stockStr.isEmpty() || purchasePriceStr.isEmpty() || sellPriceStr.isEmpty() || selectedCategory.getName().equalsIgnoreCase("Choose Category")) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double stocks = Double.parseDouble(stockStr);
        double purchasePrice = Double.parseDouble(purchasePriceStr);
        double sellPrice = Double.parseDouble(sellPriceStr);
        String imagePath = product.getImage();

        if (selectedImageUri != null) {
            if (imagePath != null && !imagePath.isEmpty()) {
                File oldImageFile = new File(imagePath);
                if (oldImageFile.exists()) {
                    oldImageFile.delete();
                }
            }

            String savedPath = saveImageToInternalStorage(selectedImageUri);
            if (savedPath != null) {
                imagePath = savedPath;
            }
        }
        String finalImagePath = imagePath;
        executor.execute(() -> {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(EditProduct.this);
                dbHelper.copyDatabaseIfNeeded();
                SQLiteDatabase db = dbHelper.getConnection();

                String sql = "UPDATE products SET name = ?, units = ?, category_id = ?, stocks = ?, purchase_price = ?, sell_price = ?, image = ? WHERE barcode = ?";
                SQLiteStatement stmt = db.compileStatement(sql);
                stmt.bindString(1, name);
                stmt.bindString(2, units);
                stmt.bindString(3, String.valueOf(categoryId));
                stmt.bindDouble(4, stocks);
                stmt.bindDouble(5, purchasePrice);
                stmt.bindDouble(6, sellPrice);
                stmt.bindString(7, finalImagePath);
                stmt.bindString(8, product.getBarcode());

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

    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File imageFile = new File(getFilesDir(), "product_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return imageFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}