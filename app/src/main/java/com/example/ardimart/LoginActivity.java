package com.example.ardimart;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ardimart.config.DatabaseHelper;
import com.example.ardimart.config.SessionManager;

import org.mindrot.jbcrypt.BCrypt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText txtUsername;
    private EditText txtPassword;
    private Button loginButton;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.login);

        txtUsername = findViewById(R.id.txtUsername);
        txtPassword = findViewById(R.id.txtPassword);
        loginButton = findViewById(R.id.loginButton);

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

        TextView signupText = findViewById(R.id.signupText);
        signupText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        loginButton.setOnClickListener(v -> login());
    }

    private void login() {
        String username = txtUsername.getText().toString();
        String password = txtPassword.getText().toString();

        executor.execute(() -> {
            try {
                DatabaseHelper dbHelper = new DatabaseHelper(LoginActivity.this);
                dbHelper.copyDatabaseIfNeeded();
                SQLiteDatabase db = dbHelper.getConnection();

                Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = ?", new String[]{username});
                if(cursor.moveToFirst()) {
                    String dbPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
                    String level = cursor.getString(cursor.getColumnIndexOrThrow("level_id"));
                    String uuid = cursor.getString(cursor.getColumnIndexOrThrow("uuid"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));

                    if (BCrypt.checkpw(password, dbPassword)) {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Login Succesful!", Toast.LENGTH_SHORT).show();

                            SessionManager sessionManager = new SessionManager(LoginActivity.this);
                            sessionManager.createLoginSession(uuid, name, username, level);

                            Intent intent;
                            intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
                            txtPassword.setText("");
                            txtPassword.requestFocus();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Error User Not Found", Toast.LENGTH_SHORT).show();
                        txtUsername.setText("");
                        txtPassword.setText("");
                        txtUsername.requestFocus();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
}
