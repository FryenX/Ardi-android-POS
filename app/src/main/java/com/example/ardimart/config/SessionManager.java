package com.example.ardimart.config;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ardimart.LoginActivity;

public class SessionManager extends AppCompatActivity {
    private static final String PREF_NAME = "LoginSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_ID = "id";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_NAME = "name";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_LEVEL = "level";

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(int id, String uuid, String name, String username, String level) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_ID, id);
        editor.putString(KEY_UUID, uuid);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_LEVEL, level);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    public int getId() {
        return pref.getInt(KEY_ID, -1);
    }
    public String getUuid() {
        return pref.getString(KEY_UUID, null);
    }

    public String getName() {
        return pref.getString(KEY_NAME, null);
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, null);
    }

    public String getLevel() {
        return pref.getString(KEY_LEVEL, null);
    }

    public void checkLogin() {
        if (!isLoggedIn()) {
            Intent i = new Intent(context, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);

            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).finish();
            }
        }
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();

        Intent i = new Intent(context, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
