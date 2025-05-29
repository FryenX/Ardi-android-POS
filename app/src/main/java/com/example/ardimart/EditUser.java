package com.example.ardimart;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.ardimart.config.DatabaseHelper;

import org.mindrot.jbcrypt.BCrypt;

public class EditUser extends AppCompatActivity {
    User user;
    private final Executor executor = Executors.newSingleThreadExecutor();
    EditText txtName, txtUserName, txtPassword, txtPasswordConfirm;
    Spinner txtLevel;
    Button btnSaveUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Edit User");

        user = (User) getIntent().getSerializableExtra("user");
        txtName = findViewById(R.id.txtName);
        txtUserName = findViewById(R.id.txtUserName);
        txtPassword = findViewById(R.id.txtPassword);
        txtPasswordConfirm = findViewById(R.id.txtPasswordConfirm);
        txtLevel = findViewById(R.id.txtLevel);
        btnSaveUser = findViewById(R.id.btnSaveUser);

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Admin", "Cashier"}
        );
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        txtLevel.setAdapter(levelAdapter);
        if (user != null) {
            txtName.setText(user.getName());
            txtUserName.setText(user.getUserName());
            txtPassword.setText("");
            txtPasswordConfirm.setText("");

            if (user.getLevel().equalsIgnoreCase("Admin")) {
                txtLevel.setSelection(0);
            } else {
                txtLevel.setSelection(1);
            }

            btnSaveUser.setText("Update User");

            btnSaveUser.setOnClickListener(v -> updateUser());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();  // or
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void updateUser() {
        String name = txtName.getText().toString().trim();
        String username = txtUserName.getText().toString().trim();
        String password = txtPassword.getText().toString().trim();
        String passwordConfirm = txtPasswordConfirm.getText().toString().trim();
        String level = txtLevel.getSelectedItem().toString();

        if (name.isEmpty() || username.isEmpty() || level.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields and select a level", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.isEmpty() && !password.equals(passwordConfirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(EditUser.this);
                dbHelper.copyDatabaseIfNeeded();
                SQLiteDatabase db = dbHelper.getConnection();

                Cursor cursor = db.rawQuery("SELECT id FROM users WHERE username = ?", new String[]{username});
                if (cursor.moveToFirst()) {
                    int foundUserId = cursor.getInt(0);
                    int currentUserId = user.getId();
                    if (foundUserId != currentUserId) {
                        runOnUiThread(() -> Toast.makeText(EditUser.this, "Username already exists", Toast.LENGTH_SHORT).show());
                        cursor.close();
                        return;
                    }
                }
                cursor.close();

                String sql;
                SQLiteStatement stmt;

                String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                if (password.isEmpty()) {
                    // No password change - update other fields only
                    sql = "UPDATE users SET name = ?, levels = ?, username = ?, updated_at = ? WHERE id = ?";
                    stmt = db.compileStatement(sql);
                    stmt.bindString(1, name);
                    stmt.bindString(2, level);
                    stmt.bindString(3, username);
                    stmt.bindString(4, currentTime);
                    stmt.bindLong(5, user.getId());
                } else {
                    // Password changed - hash it and update all fields
                    String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                    sql = "UPDATE users SET name = ?, levels = ?, username = ?, password = ?, updated_at = ? WHERE id = ?";
                    stmt = db.compileStatement(sql);
                    stmt.bindString(1, name);
                    stmt.bindString(2, level);
                    stmt.bindString(3, username);
                    stmt.bindString(4, hashedPassword);
                    stmt.bindString(5, currentTime);
                    stmt.bindLong(6, user.getId());
                }

                int rowsAffected = stmt.executeUpdateDelete();

                runOnUiThread(() -> {
                    if (rowsAffected > 0) {
                        Toast.makeText(EditUser.this, "User updated successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(EditUser.this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(EditUser.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}
