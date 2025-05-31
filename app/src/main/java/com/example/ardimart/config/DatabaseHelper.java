package com.example.ardimart.config;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.io.*;

public class DatabaseHelper {

    private static final String DB_NAME = "ardi-mart-lite.db";
    private static final String DB_PATH_SUFFIX = "/databases/";
    private Context context;

    public DatabaseHelper(Context context) {
        this.context = context;
    }

    @NonNull
    private String getDatabasePath() {
        return context.getApplicationInfo().dataDir + DB_PATH_SUFFIX + DB_NAME;
    }

    public void copyDatabaseIfNeeded() {
        String dbPath = getDatabasePath();
        File dbFile = new File(dbPath);

        if (!dbFile.exists()) {
            File dbDir = new File(dbFile.getParent());
            if (!dbDir.exists()) dbDir.mkdirs();

            try (InputStream is = context.getAssets().open("config/" + DB_NAME);
                 OutputStream os = new FileOutputStream(dbPath)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
                os.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public SQLiteDatabase getConnection() {
        return SQLiteDatabase.openDatabase(getDatabasePath(), null, SQLiteDatabase.OPEN_READWRITE);
    }
}