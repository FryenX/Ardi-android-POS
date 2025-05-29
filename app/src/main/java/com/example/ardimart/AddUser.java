package com.example.ardimart;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ardimart.config.DatabaseHelper;

import org.mindrot.jbcrypt.BCrypt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddUser extends AppCompatActivity {

    private EditText txtName, txtUsername, txtPassword, txtPasswordConfirm;
    private Spinner txtLevel;
    private Button btnSaveUser;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Add User");

        txtName = findViewById(R.id.txtName);
        txtUsername = findViewById(R.id.txtUserName);
        txtPassword = findViewById(R.id.txtPassword);
        txtPasswordConfirm = findViewById(R.id.txtPasswordConfirm);
        txtLevel = findViewById(R.id.txtLevel);
        btnSaveUser = findViewById(R.id.btnSaveUser);

        ImageView passwordToggle = findViewById(R.id.passwordToggle);
        passwordToggle.setOnClickListener(v -> togglePasswordVisibility(txtPassword, passwordToggle));

        ImageView passwordConfirmToggle = findViewById(R.id.passwordConfirmToggle);
        passwordConfirmToggle.setOnClickListener(v -> togglePasswordVisibility(txtPasswordConfirm, passwordConfirmToggle));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Choose Level", "Admin", "Cashier"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        txtLevel.setAdapter(adapter);

        btnSaveUser.setOnClickListener(v -> addUser());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();  // or
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggle) {
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggle.setImageResource(R.drawable.ic_eye_open);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggle.setImageResource(R.drawable.ic_eye_closed);
        }
        editText.setSelection(editText.getText().length());
    }

    private void addUser() {
        String name = txtName.getText().toString().trim();
        String username = txtUsername.getText().toString().trim();
        String password = txtPassword.getText().toString().trim();
        String passwordConfirm = txtPasswordConfirm.getText().toString().trim();
        String level = txtLevel.getSelectedItem().toString();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() || level.equals("Pilih level")) {
            Toast.makeText(this, "Please fill in all fields and select a level", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(AddUser.this);
                dbHelper.copyDatabaseIfNeeded();
                SQLiteDatabase db = dbHelper.getConnection();

                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                String uuid = UUID.randomUUID().toString();
                String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                String sql = "INSERT INTO users (uuid, name, levels, username, password, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                SQLiteStatement stmt = db.compileStatement(sql);
                stmt.bindString(1, uuid);
                stmt.bindString(2, name);
                stmt.bindString(3, level);
                stmt.bindString(4, username);
                stmt.bindString(5, hashedPassword);
                stmt.bindString(6, datetime);
                stmt.bindString(7, datetime);
                stmt.executeInsert();

                runOnUiThread(() -> {
                    Toast.makeText(this, "User added successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}
