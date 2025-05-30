package com.example.ardimart;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ardimart.config.DatabaseHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class ProductsFragment extends Fragment {

    private RecyclerView recyclerProducts;
    private ProductAdapter productAdapter;
    private View emptyView;
    private static final int ADD_PRODUCT_REQUEST_CODE = 101;

    public ProductsFragment() {
    }

    public static ProductsFragment newInstance() {
        return new ProductsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_products, container, false);

        emptyView = view.findViewById(R.id.emptyView);
        recyclerProducts = view.findViewById(R.id.recyclerProducts);
        recyclerProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        productAdapter = new ProductAdapter();
        recyclerProducts.setAdapter(productAdapter);

        TextView txtPageInfo = view.findViewById(R.id.txtPageInfo);
        Button btnPrevPage = view.findViewById(R.id.btnPrevPage);
        Button btnNextPage = view.findViewById(R.id.btnNextPage);
        EditText searchInput = view.findViewById(R.id.searchInput);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                productAdapter.filter(s.toString().trim());
                updatePageInfo(txtPageInfo);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        productAdapter.setListener(new ProductAdapter.OnProductActionListener() {
            @Override
            public void onEdit(Product product) {
                Intent intent = new Intent(getContext(), EditProduct.class);
                intent.putExtra("product", product);
                startActivityForResult(intent, ADD_PRODUCT_REQUEST_CODE);
            }

            @Override
            public void onDetail(Product product) {
                if (product == null) return;

                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.product_detail_dialog, null);
                ImageView imgProduct = dialogView.findViewById(R.id.imgProduct);
                TextView txtDetails = dialogView.findViewById(R.id.txtProductDetails);

                String imagePath = product.getImage();
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imgFile = new File(imagePath);
                    if (imgFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                        imgProduct.setImageBitmap(bitmap);
                    } else {
                        imgProduct.setImageResource(R.drawable.ic_image_placeholder);
                    }
                } else {
                    imgProduct.setImageResource(R.drawable.ic_image_placeholder);
                }

                // Build product detail text with category name
                String details = "Barcode: " + product.getBarcode() + "\n" +
                        "Name: " + product.getName() + "\n" +
                        "Units: " + product.getUnits() + "\n" +
                        "Category: " + product.getCategoryName() + "\n" +
                        "Stocks: " + product.getStocks() + "\n" +
                        "Purchase Price: " + product.getPurchasePrice() + "\n" +
                        "Sell Price: " + product.getSellPrice();

                txtDetails.setText(details);

                new AlertDialog.Builder(getContext())
                        .setTitle("Product Details")
                        .setView(dialogView)
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onDelete(Product product) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to delete this product: " + product.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                SQLiteDatabase db = null;
                                try {
                                    DatabaseHelper helper = new DatabaseHelper(getContext());
                                    helper.copyDatabaseIfNeeded();
                                    db = helper.getConnection();

                                    // Query image path for this product barcode
                                    String imagePath = null;
                                    Cursor cursor = db.query(
                                            "products",
                                            new String[]{"image"},
                                            "barcode = ?",
                                            new String[]{product.getBarcode()},
                                            null, null, null
                                    );

                                    if (cursor != null) {
                                        if (cursor.moveToFirst()) {
                                            imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image"));
                                        }
                                        cursor.close();
                                    }

                                    // Delete product from DB
                                    int rowsDeleted = db.delete("products", "barcode = ?", new String[]{product.getBarcode()});

                                    // Delete image file if exists and DB deletion succeeded
                                    if (rowsDeleted > 0 && imagePath != null && !imagePath.isEmpty()) {
                                        File imageFile = new File(imagePath);
                                        if (imageFile.exists()) {
                                            imageFile.delete(); // Optional: log if needed
                                        }
                                    }

                                    requireActivity().runOnUiThread(() -> {
                                        if (rowsDeleted > 0) {
                                            Toast.makeText(getContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                                            loadProductsFromDatabase();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to delete product", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } catch (Exception e) {
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(getContext(), "Error deleting product: " + e.getMessage(), Toast.LENGTH_LONG).show()
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

        btnPrevPage.setOnClickListener(v -> {
            int currentPage = productAdapter.getCurrentPage();
            if (currentPage > 0) {
                productAdapter.setPage(currentPage - 1);
                updatePageInfo(txtPageInfo);
            }
        });

        btnNextPage.setOnClickListener(v -> {
            int currentPage = productAdapter.getCurrentPage();
            if (currentPage < productAdapter.getTotalPages() - 1) {
                productAdapter.setPage(currentPage + 1);
                updatePageInfo(txtPageInfo);
            }
        });

        FloatingActionButton btnAddProduct = view.findViewById(R.id.btnAddProduct);
        btnAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddProduct.class);
            startActivityForResult(intent, ADD_PRODUCT_REQUEST_CODE);
        });

        loadProductsFromDatabase();
        return view;
    }

    private void updatePageInfo(TextView txtPageInfo) {
        int current = productAdapter.getCurrentPage() + 1;
        int total = productAdapter.getTotalPages();
        txtPageInfo.setText("Page " + current + "/" + total);
    }
    private void loadProductsFromDatabase() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Product> products = new ArrayList<>();
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(getContext());
                dbHelper.copyDatabaseIfNeeded();
                db = dbHelper.getConnection();

                String query = "SELECT p.barcode, p.name AS product_name, p.units, p.category_id, p.stocks,p.purchase_price, p.sell_price,c.name AS category_name, p.image " +
                        "FROM products p LEFT JOIN categories c ON p.category_id = c.id ORDER BY p.barcode ASC";
                cursor = db.rawQuery(query, null);
                while (cursor != null && cursor.moveToNext()) {
                    String barcode = cursor.getString(cursor.getColumnIndexOrThrow("barcode"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("product_name"));
                    String units = cursor.getString(cursor.getColumnIndexOrThrow("units"));
                    int categoryId = cursor.getInt(cursor.getColumnIndexOrThrow("category_id"));
                    double stocks = cursor.getDouble(cursor.getColumnIndexOrThrow("stocks"));
                    double purchasePrice = cursor.getDouble(cursor.getColumnIndexOrThrow("purchase_price"));
                    double sellPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("sell_price"));
                    String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
                    String image = cursor.getString(cursor.getColumnIndexOrThrow("image"));
                    if (image == null || image.isEmpty()) {
                        image = "default";
                    }
                    products.add(new Product(barcode, name, units, categoryId, categoryName, stocks, purchasePrice, sellPrice, image));
                }

                requireActivity().runOnUiThread(() -> {
                    productAdapter.setProducts(products);
                    if (products.isEmpty()) showEmptyView("No products found");
                    else hideEmptyView();
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error loading products: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if (cursor != null) cursor.close();
                if (db != null && db.isOpen()) db.close();
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_PRODUCT_REQUEST_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            loadProductsFromDatabase();
        }
    }

    private void showEmptyView(String message) {
        LinearLayout emptyLayout = requireView().findViewById(R.id.emptyView);
        TextView tvEmpty = requireView().findViewById(R.id.tvEmptyText);
        emptyLayout.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }

    private void hideEmptyView() {
        LinearLayout emptyLayout = requireView().findViewById(R.id.emptyView);
        emptyLayout.setVisibility(View.GONE);
    }
}
