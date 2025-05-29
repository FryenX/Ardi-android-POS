package com.example.ardimart;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ardimart.config.DatabaseHelper;
import com.example.ardimart.config.SessionManager;


import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {
    private EditText txtName;
    private EditText txtUsername;
    private EditText txtPassword;
    private EditText txtPasswordConfirm;
    private Spinner txtLevel;
    private Button registerButton;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);            // must be inside onCreate
        EdgeToEdge.enable(this);
        setContentView(R.layout.register);

        txtName = findViewById(R.id.txtName);
        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        txtPasswordConfirm = findViewById(R.id.txtPasswordConfirm);
        txtLevel = findViewById(R.id.txtLevel);
        registerButton = findViewById(R.id.registerButton);

        EditText txtPassword = findViewById(R.id.txtPassword);
        ImageView passwordToggle = findViewById(R.id.passwordToggle);

        passwordToggle.setOnClickListener(v -> {
            if (txtPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                txtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordToggle.setImageResource(R.drawable.ic_eye_open);
            } else {
                txtPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordToggle.setImageResource(R.drawable.ic_eye_closed);
            }
            txtPassword.setSelection(txtPassword.getText().length());
        });

        EditText txtPasswordConfirm = findViewById(R.id.txtPasswordConfirm);
        ImageView passwordConfirmToggle = findViewById(R.id.passwordConfirmToggle);

        passwordConfirmToggle.setOnClickListener(v -> {
            if (txtPasswordConfirm.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                txtPasswordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordConfirmToggle.setImageResource(R.drawable.ic_eye_open);
            } else {
                txtPasswordConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordConfirmToggle.setImageResource(R.drawable.ic_eye_closed);
            }
            txtPasswordConfirm.setSelection(txtPasswordConfirm.getText().length());
        });

        Spinner spinner = findViewById(R.id.txtLevel);

        String[] levels = {"Pilih level", "Admin", "Cashier"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, levels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLevel = parent.getItemAtPosition(position).toString();
                if (position == 0) {
                    // User selected "Pilih level" — treat as null
                    selectedLevel = null;
                }

                // Use selectedLevel (could be null if no real selection)
                Log.d("Selected Level", String.valueOf(selectedLevel));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case if needed
            }
        });

        TextView signupText = findViewById(R.id.loginNav);
        signupText.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> register());
    }

    private void register()
    {
        String name = txtName.getText().toString();
        String username = txtUsername.getText().toString();
        String password = txtPassword.getText().toString();
        String passwordConfirm = txtPasswordConfirm.getText().toString();
        String level = txtLevel.getSelectedItem().toString();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() || level.equals("Pilih level")) {
            Toast.makeText(this, "Please fill in all fields and select a valid level", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(RegisterActivity.this);
                dbHelper.copyDatabaseIfNeeded();
                SQLiteDatabase db = dbHelper.getConnection();

                Cursor cursor = db.rawQuery("SELECT id FROM users WHERE username = ?", new String[]{username});
                if (cursor.moveToFirst()) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Username already exists", Toast.LENGTH_SHORT).show());
                    cursor.close();
                    return;
                }
                cursor.close();

                String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

                String uuid = UUID.randomUUID().toString();

                String datetime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

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
                    // Create session
                    SessionManager session = new SessionManager(RegisterActivity.this);
                    session.createLoginSession(uuid, name, username, level);

                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                });
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show());
            }catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}