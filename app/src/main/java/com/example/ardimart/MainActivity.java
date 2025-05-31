package com.example.ardimart;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.ardimart.config.SessionManager;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle drawerToggle;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        session.checkLogin();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        // Initialize views FIRST
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Now safe to access header view
        View headerView = navigationView.getHeaderView(0);
        TextView tvName = headerView.findViewById(R.id.tvName);
        TextView tvLevel = headerView.findViewById(R.id.tvLevel);

        String name = session.getName();
        String level = session.getLevel();
        tvName.setText(name);
        tvLevel.setText(level);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        if (savedInstanceState == null) {
            if ("Admin".equalsIgnoreCase(level)) {
                replaceFragment(new AdminHomeFragment(), "Home");
                navigationView.setCheckedItem(R.id.home);
            } else if ("Cashier".equalsIgnoreCase(level)) {
                replaceFragment(new TransactionsFragment(), "Home");
                navigationView.setCheckedItem(R.id.home);
            }
        }

        if ("Cashier".equalsIgnoreCase(level)) {
            navigationView.getMenu().findItem(R.id.categories).setVisible(false);
            navigationView.getMenu().findItem(R.id.users).setVisible(false);
            navigationView.getMenu().findItem(R.id.products).setVisible(false);
            navigationView.getMenu().findItem(R.id.transactions).setVisible(false);
        } else if ("Admin".equalsIgnoreCase(level)) {

        }

        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.home:
                        if ("Admin".equalsIgnoreCase(level)) {
                            replaceFragment(new AdminHomeFragment(), "Home");
                            navigationView.setCheckedItem(R.id.home);
                        } else if ("Cashier".equalsIgnoreCase(level)) {
                            replaceFragment(new TransactionsFragment(), "Home");
                            navigationView.setCheckedItem(R.id.home);
                        }
                    break;
                case R.id.transactions:
                        replaceFragment(new TransactionsFragment(), "Transactions");
                    break;
                case R.id.products:
                    if ("Admin".equalsIgnoreCase(level)) {
                        replaceFragment(new ProductsFragment(), "Products");
                    } else if ("Cashier".equalsIgnoreCase(level)) {
                        new AlertDialog.Builder(this)
                                .setTitle("Error")
                                .setMessage("You do not have permission to access this section.")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                    break;
                case R.id.users:
                    if ("Admin".equalsIgnoreCase(level)) {
                        replaceFragment(new UsersFragment(), "Users");
                    } else if ("Cashier".equalsIgnoreCase(level)) {
                        new AlertDialog.Builder(this)
                                .setTitle("Error")
                                .setMessage("You do not have permission to access this section.")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                    break;
                case R.id.categories:
                    if ("Admin".equalsIgnoreCase(level)) {
                        replaceFragment(new CategoriesFragment(), "Categories");
                    } else if ("Cashier".equalsIgnoreCase(level)) {
                        new AlertDialog.Builder(this)
                                .setTitle("Error")
                                .setMessage("You do not have permission to access this section.")
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                    break;
                case R.id.logoutButton:
                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Logout")
                            .setMessage("Are you sure you want to log out?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                session.logoutUser();
                                finish();
                            })
                            .setNegativeButton("No", null)
                            .show();
                    break;
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void replaceFragment(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }
}