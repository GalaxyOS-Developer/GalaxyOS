package com.galaxyos.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final int white = Color.rgb(255, 255, 255);
    private final int pale = Color.rgb(220, 229, 247);
    private final int ink = Color.rgb(24, 34, 58);
    private LinearLayout root;
    private LinearLayout appGridContainer;
    private LinearLayout dockContainer;
    private android.content.SharedPreferences launcherPrefs;
    private EditText search;
    private final List<ResolveInfo> installedApps = new ArrayList<>();
    private final android.os.Handler clockHandler =
        new android.os.Handler(android.os.Looper.getMainLooper());
    private TextView clockView;
    private TextView dateView;

    private final Runnable clockUpdater = new Runnable() {
        @Override public void run() {
            if (clockView != null && dateView != null) {
                java.util.Locale locale = new java.util.Locale("it", "IT");
                clockView.setText(new SimpleDateFormat("HH:mm", locale).format(new Date()));

                String dateText = new SimpleDateFormat("EEEE d MMMM", locale)
                    .format(new Date());
                dateText = dateText.substring(0, 1).toUpperCase(locale) + dateText.substring(1);
                dateView.setText(dateText);
            }
            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        clockHandler.removeCallbacks(clockUpdater);
        clockUpdater.run();
    }

    @Override
    protected void onPause() {
        clockHandler.removeCallbacks(clockUpdater);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(20, 29, 49));
        getWindow().setNavigationBarColor(Color.rgb(20, 29, 49));
        buildHome();
        enableTwoFingerCustomizationGesture();
    }

    private void openHomeSettings() {
        Intent intent = new Intent("android.settings.HOME_SETTINGS");
        try {
            startActivity(intent);
        } catch (Exception firstError) {
            try {
                startActivity(new Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS"));
            } catch (Exception secondError) {
                Toast.makeText(this,
                    "Apri Impostazioni Android e cerca App predefinite > App Home",
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void buildHome() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 22, 24, 14);
        root.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(20, 31, 57), Color.rgb(38, 62, 105), Color.rgb(75, 104, 157)}
        ));

        TextView brand = text("GalaxyOS", 18, pale);
        brand.setTypeface(null, Typeface.BOLD);
        root.addView(brand);

        TextView homeButton = text(
            "Cambia schermata mettendo sulle impostazioni il launcher come schermata!",
            14, ink);
        homeButton.setGravity(Gravity.CENTER);
        homeButton.setTypeface(null, Typeface.BOLD);
        homeButton.setPadding(18, 14, 18, 14);
        homeButton.setBackground(rounded(Color.rgb(246, 248, 252), 24));
        LinearLayout.LayoutParams homeButtonParams =
            new LinearLayout.LayoutParams(-1, -2);
        homeButtonParams.topMargin = 14;
        root.addView(homeButton, homeButtonParams);
        homeButton.setOnClickListener(v -> openHomeSettings());

        clockView = text("--:--", 54, white);
        TextView clock = clockView;
        clock.setTypeface(null, Typeface.NORMAL);
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clockParams.topMargin = 20;
        root.addView(clock, clockParams);

        dateView = text("", 17, pale);
        TextView date = dateView;
        root.addView(date);

        TextView welcome = text("La tua giornata, a colpo d'occhio", 14, pale);
        LinearLayout.LayoutParams welcomeParams = new LinearLayout.LayoutParams(-2, -2);
        welcomeParams.topMargin = 18;
        welcomeParams.bottomMargin = 22;
        root.addView(welcome, welcomeParams);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setHint("Cerca un'app…");
        search.setHintTextColor(Color.rgb(104, 116, 139));
        search.setTextColor(ink);
        search.setPadding(18, 6, 18, 6);
        search.setBackground(rounded(Color.rgb(246, 248, 252), 30));
        search.setCompoundDrawablesWithIntrinsicBounds(
            android.R.drawable.ic_menu_search, 0, 0, 0);
        search.getCompoundDrawables()[0].setTint(Color.rgb(104, 116, 139));
        root.addView(search, new LinearLayout.LayoutParams(-1, 52));

        TextView heading = text("Le tue app", 22, white);
        heading.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-2, -2);
        headingParams.topMargin = 22;
        headingParams.bottomMargin = 12;
        root.addView(heading, headingParams);

        ScrollView scroll = new ScrollView(this);
        appGridContainer = new LinearLayout(this);
        appGridContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(appGridContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        dockContainer = new LinearLayout(this);
        dockContainer.setOrientation(LinearLayout.HORIZONTAL);
        dockContainer.setGravity(Gravity.CENTER);
        dockContainer.setPadding(8, 8, 8, 8);
        dockContainer.setBackground(rounded(0x38FFFFFF, 28));
        LinearLayout.LayoutParams dockParams =
            new LinearLayout.LayoutParams(-1, 76);
        dockParams.topMargin = 10;
        root.addView(dockContainer, dockParams);
        dockContainer.setOnDragListener((view, event) -> {
            if (event.getAction() == android.view.DragEvent.ACTION_DROP) {
                Object state = event.getLocalState();
                if (state instanceof String) {
                    addToDock((String) state);
                    return true;
                }
            }
            return true;
        });
        renderDock();

        TextView footer = text("GalaxyOS Launcher • Versione 1.0", 12, pale);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = 12;
        root.addView(footer, footerParams);

        setContentView(root);
        loadApps();

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                showApps(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });

        search.setOnEditorActionListener((v, actionId, event) -> {
            String q = search.getText().toString().trim();
            if (!q.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.putExtra(SearchManager.QUERY, q);
                try {
                    startActivity(intent);
                } catch (Exception ex) {
                    Toast.makeText(this, "Ricerca non disponibile", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        installedApps.clear();
        installedApps.addAll(pm.queryIntentActivities(intent, 0));
        Collections.sort(installedApps, (a, b) ->
            a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString()));
        showApps("");
    }


    private void addToDock(String packageName) {
        android.content.SharedPreferences prefs =
            getSharedPreferences("galaxyos_launcher", MODE_PRIVATE);
        java.util.LinkedHashSet<String> packages =
            new java.util.LinkedHashSet<>(prefs.getStringSet("dock_apps",
                new java.util.LinkedHashSet<>()));
        if (packages.add(packageName)) {
            prefs.edit().putStringSet("dock_apps", packages).apply();
            renderDock();
            Toast.makeText(this, "App aggiunta alla dock", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Questa app è già nella dock", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderDock() {
        if (dockContainer == null) return;
        dockContainer.removeAllViews();

        android.content.SharedPreferences prefs =
            getSharedPreferences("galaxyos_launcher", MODE_PRIVATE);
        java.util.Set<String> packages = prefs.getStringSet("dock_apps",
            new java.util.LinkedHashSet<>());

        PackageManager pm = getPackageManager();
        for (String packageName : packages) {
            try {
                Intent launch = pm.getLaunchIntentForPackage(packageName);
                if (launch == null) continue;

                ImageView icon = new ImageView(this);
                icon.setImageDrawable(pm.getApplicationIcon(packageName));
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                icon.setContentDescription(pm.getApplicationLabel(
                    pm.getApplicationInfo(packageName, 0)));
                icon.setPadding(8, 8, 8, 8);

                LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(56, 56);
                lp.setMargins(3, 0, 3, 0);
                dockContainer.addView(icon, lp);

                icon.setOnClickListener(v -> startActivity(launch));
                icon.setOnLongClickListener(v -> {
                    android.content.ClipData data =
                        android.content.ClipData.newPlainText("app", packageName);
                    v.startDragAndDrop(data, new View.DragShadowBuilder(v),
                        packageName, 0);
                    return true;
                });
            } catch (Exception ignored) {}
        }

        if (dockContainer.getChildCount() == 0) {
            TextView hint = text("Tieni premuta un'app per aggiungerla qui", 12, pale);
            hint.setGravity(Gravity.CENTER);
            dockContainer.addView(hint, new LinearLayout.LayoutParams(-1, -1));
        }
    }

    private final android.os.Handler gestureHandler =
        new android.os.Handler(android.os.Looper.getMainLooper());

    private boolean twoFingerGestureTriggered = false;

    private final Runnable openCustomization = () -> {
        twoFingerGestureTriggered = true;
        new AlertDialog.Builder(this)
            .setTitle("Personalizza Home")
            .setItems(new String[]{
                "Cambia sfondo",
                "Impostazioni launcher"
            }, (dialog, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(Intent.createChooser(intent, "Scegli immagine"), 7001);
                } else {
                    openHomeSettings();
                }
            })
            .setNegativeButton("Annulla", null)
            .show();
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != 7001 || resultCode != Activity.RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }

        try {
            Intent wallpaperIntent = new Intent(Intent.ACTION_ATTACH_DATA);
            wallpaperIntent.setDataAndType(data.getData(), "image/*");
            wallpaperIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            wallpaperIntent.setPackage("org.lineageos.glimpse");
            startActivity(wallpaperIntent);
        } catch (Exception e) {
            android.widget.Toast.makeText(
                    this,
                    "Impossibile aprire Glimpse: " + e.getMessage(),
                    android.widget.Toast.LENGTH_LONG
            ).show();
        }
    }

    private void enableTwoFingerCustomizationGesture() {
        // Gli eventi vengono rilevati a livello dell'intera finestra.
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    twoFingerGestureTriggered = false;
                    gestureHandler.removeCallbacks(openCustomization);
                    gestureHandler.postDelayed(openCustomization, 1000);
                } else {
                    gestureHandler.removeCallbacks(openCustomization);
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                gestureHandler.removeCallbacks(openCustomization);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

     private void showApps(String filter) {
        if (appGridContainer == null) return;
        appGridContainer.removeAllViews();

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(true);

        PackageManager pm = getPackageManager();
        int count = 0;
        for (ResolveInfo app : installedApps) {
            String label = app.loadLabel(pm).toString();
            if (!label.toLowerCase(Locale.getDefault()).contains(filter.toLowerCase(Locale.getDefault()))) {
                continue;
            }

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(4, 10, 4, 10);
            item.setBackground(rounded(0x14FFFFFF, 24));
            item.setClickable(true);
            item.setFocusable(true);

            ImageView icon = new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setAdjustViewBounds(true);
            try {
                icon.setImageDrawable(app.loadIcon(pm));
            } catch (Exception ignored) {}

            TextView name = text(label, 12, white);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setPadding(0, 6, 0, 0);

            item.addView(icon, new LinearLayout.LayoutParams(56, 56));
            item.addView(name, new LinearLayout.LayoutParams(-1, -2));

            item.setOnClickListener(v -> {
                Intent launch = pm.getLaunchIntentForPackage(app.activityInfo.packageName);
                if (launch != null) {
                    try {
                        startActivity(launch);
                    } catch (Exception e) {
                        Toast.makeText(this, "Impossibile aprire l'app", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            item.setOnLongClickListener(v -> {
                android.content.ClipData data =
                    android.content.ClipData.newPlainText(
                        "app", app.activityInfo.packageName);
                v.startDragAndDrop(data, new View.DragShadowBuilder(v),
                    app.activityInfo.packageName, 0);
                return true;
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(4, 4, 4, 4);
            grid.addView(item, params);
            count++;
        }

        if (count == 0) {
            TextView empty = text("Nessuna app trovata", 15, white);
            empty.setPadding(4, 20, 4, 20);
            appGridContainer.addView(empty);
        } else {
            appGridContainer.addView(grid);
        }
    }
}
