package background.work.around;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.Locale;

public class WhitelistActivity extends Activity {

    private SharedPreferences whitelistPrefs;
    private LinearLayout listContainer;

    private boolean isEn() {
        return !Locale.getDefault().getLanguage().equals("ru");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        whitelistPrefs = getApplicationContext().createDeviceProtectedStorageContext().getSharedPreferences("whitelist_prefs", Context.MODE_PRIVATE);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(32, 64, 32, 32);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        scrollView.setLayoutParams(scrollParams);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);
        root.addView(scrollView);
        
        Button backBtn = new Button(this);
        backBtn.setText(isEn() ? "Back" : "Назад");
        backBtn.setBackgroundColor(Color.parseColor("#34495e"));
        backBtn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 32, 0, 0);
        backBtn.setLayoutParams(btnParams);
        backBtn.setOnClickListener(v -> finish());
        root.addView(backBtn);

        setContentView(root);

        showIntroDialog();
        loadApps();
    }

    private void showIntroDialog() {
    String msgRu = "Привет, это настройки управления приложениями. Здесь вы можете выбрать приложения, exported сервисы которых, если они не имеют permission, будут запускаться данным приложением вне зависимости от их назначения. То есть это принудительный перезапуск приложений. Здесь будут показаны только те приложения, которые имеют такие сервисы. Вам не нужно включать здесь приложения, которые и так совместимы с BackgroundWorkAround, ведь они имеют специальные action, через которые они будут автозапущены в любом случае.";
    String msgEn = "Hello, these are the application management settings. Here you can select applications whose exported services, if they do not have a permission, will be launched by this application regardless of their purpose. This is essentially a forced restart of applications. Only applications that have such services will be shown here. You do not need to include apps here that are already compatible with BackgroundWorkAround, because they have special actions through which they will be auto-started anyway.";

    AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(isEn() ? "Alert" : "Внимание")
            .setMessage(isEn() ? msgEn : msgRu)
            .setPositiveButton("OK", null)
            .create();
    
    dialog.show();

    android.view.Window window = dialog.getWindow();
    if (window != null) {
        android.view.WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = android.view.Gravity.CENTER;
        lp.y = 0;
        window.setAttributes(lp);
    }
    }


    private void loadApps() {        
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            java.util.List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_SERVICES);
            
            runOnUiThread(() -> listContainer.removeAllViews());

            for (PackageInfo pi : packages) {
                
                if (pi.packageName.equals(getPackageName())) continue;

                boolean hasTargetService = false;
                if (pi.services != null) {
                    for (ServiceInfo si : pi.services) {
                        if (si.exported && (si.permission == null || si.permission.isEmpty())) {
                            hasTargetService = true;
                            break;
                        }
                    }
                }

                if (hasTargetService) {
                    ApplicationInfo ai = pi.applicationInfo;
                    String appName = ai.loadLabel(pm).toString();
                    android.graphics.drawable.Drawable icon = ai.loadIcon(pm);
                    boolean isChecked = whitelistPrefs.getBoolean(pi.packageName, false);

                    runOnUiThread(() -> addAppRow(appName, pi.packageName, icon, isChecked));
                }
            }
        }).start();
    }

    private void addAppRow(String name, String pkg, android.graphics.drawable.Drawable icon, boolean isChecked) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 16, 0, 16);

        ImageView img = new ImageView(this);
        img.setImageDrawable(icon);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(96, 96);
        img.setLayoutParams(imgParams);
        row.addView(img);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        textParams.setMargins(24, 0, 24, 0);
        textLayout.setLayoutParams(textParams);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextColor(Color.WHITE);
        nameView.setTextSize(16f);
        
        TextView pkgView = new TextView(this);
        pkgView.setText("[" + pkg + "]");
        pkgView.setTextColor(Color.LTGRAY);
        pkgView.setTextSize(12f);

        textLayout.addView(nameView);
        textLayout.addView(pkgView);
        row.addView(textLayout);

        Switch toggle = new Switch(this);
        toggle.setChecked(isChecked);
        toggle.setOnCheckedChangeListener((buttonView, isNowChecked) -> {
            if (isNowChecked) {
                whitelistPrefs.edit().putBoolean(pkg, true).apply();
            } else {
                whitelistPrefs.edit().remove(pkg).apply();
            }
        });
        row.addView(toggle);

        listContainer.addView(row);
    }
}
