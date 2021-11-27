package com.htetznaing.gdrivex;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.inputmethodservice.Keyboard;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.htetznaing.gdrivex.databinding.ActivityMainBinding;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ProgressDialog progressDialog;
    private ClipboardManager clipboardManager;
    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.please_wait));

        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.setWebChromeClient(new WebChromeClient());
        binding.webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                downloadMe();
            }
        });

        binding.webView.addJavascriptInterface(new MyInterface(),"HtetzNaing");
        binding.webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            progressDialog.dismiss();
            String cookie = CookieManager.getInstance().getCookie(url);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.done)
                    .setMessage(R.string.what_do_you_want)
                    .setPositiveButton(R.string.play, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            openWithMXPlayer(url,cookie);
                        }
                    })
                    .setNegativeButton(R.string.download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            downloadWithADM(url,cookie);
                        }
                    })
                    .show();
        });

        binding.btnDownload.setOnClickListener(v -> letDownload());

        binding.editQuery.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if(event.getAction() == MotionEvent.ACTION_UP) {
                if(event.getRawX() >= (binding.editQuery.getRight() - binding.editQuery.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    if (clipboardManager.hasPrimaryClip())
                        binding.editQuery.setText(clipboardManager.getPrimaryClip().getItemAt(0).getText());
                    else Toast.makeText(MainActivity.this, R.string.empty, Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            return false;
        });

        binding.dev.setOnClickListener(view -> openDevFB());
    }

    public void letDownload() {
        String input = Objects.requireNonNull(binding.editQuery.getText()).toString();
        String id = get_drive_id(input);
        if (URLUtil.isValidUrl(input) && input.contains("drive.google.com") && id!=null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(binding.editQuery.getWindowToken(), 0);
            binding.webView.loadUrl("https://drive.google.com/uc?export=download&id="+id);
            progressDialog.show();
        }else Toast.makeText(MainActivity.this, R.string.please_input_valid_url, Toast.LENGTH_SHORT).show();
    }

    private void downloadMe() {
        String myScript = "var title = \"Sorry\",\n" +
                "    desc = \"Error\",\n" +
                "    dl_id = \"uc-download-link\";\n" +
                "\n" +
                "if (document.getElementById(dl_id)) {\n" +
                "    document.getElementById(dl_id).click();\n" +
                "} else if (document.getElementsByClassName('uc-error-caption')) {\n" +
                "    title = document.getElementsByClassName('uc-error-caption')[0].textContent;\n" +
                "    if (document.getElementsByClassName('uc-error-subcaption')) {\n" +
                "        desc = document.getElementsByClassName('uc-error-subcaption')[0].textContent;\n" +
                "    }\n" +
                "    HtetzNaing.error(title, desc);\n" +
                "}";
            binding.webView.loadUrl("javascript: (function() {" + myScript + "})()");
    }

    private String get_drive_id(String string) {
        final String regex = "[-\\w]{25,}";
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public boolean appInstalledOrNot(String str) {
        try {
            getPackageManager().getPackageInfo(str, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    //Example Open Google Drive Video with MX Player
    private void openWithMXPlayer(String url,String cookie) {
        boolean appInstalledOrNot = appInstalledOrNot("com.mxtech.videoplayer.ad");
        boolean appInstalledOrNot2 = appInstalledOrNot("com.mxtech.videoplayer.pro");
        String str2;
        if (appInstalledOrNot || appInstalledOrNot2) {
            String str3;
            if (appInstalledOrNot2) {
                str2 = "com.mxtech.videoplayer.pro";
                str3 = "com.mxtech.videoplayer.ActivityScreen";
            } else {
                str2 = "com.mxtech.videoplayer.ad";
                str3 = "com.mxtech.videoplayer.ad.ActivityScreen";
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(url), "application/x-mpegURL");
                intent.setPackage(str2);
                intent.setClassName(str2, str3);
                if (cookie != null) {
                    intent.putExtra("headers", new String[]{"cookie", cookie});
                    intent.putExtra("secure_uri", true);
                }
                startActivity(intent);
                return;
            } catch (Exception e) {
                e.fillInStackTrace();
                Log.d("errorMx", e.getMessage());
                return;
            }
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.mxtech.videoplayer.ad")));
        } catch (ActivityNotFoundException e2) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.mxtech.videoplayer.ad")));
        }
    }

    //Example Download Google Drive Video with ADM
    public void downloadWithADM(String url,String cookie) {
        boolean appInstalledOrNot = appInstalledOrNot( "com.dv.adm");
        boolean appInstalledOrNot2 = appInstalledOrNot("com.dv.adm.pay");
        boolean appInstalledOrNot3 = appInstalledOrNot( "com.dv.adm.old");
        String str3;
        if (appInstalledOrNot || appInstalledOrNot2 || appInstalledOrNot3) {
            if (appInstalledOrNot2) {
                str3 = "com.dv.adm.pay";
            } else if (appInstalledOrNot) {
                str3 = "com.dv.adm";
            } else {
                str3 = "com.dv.adm.old";
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setClassName(str3,str3+".AEditor");
                intent.putExtra(Intent.EXTRA_TEXT,url);
                if (cookie!=null)
                    intent.putExtra("Cookie",cookie);
                intent.setPackage(str3);
                startActivity(intent);
                return;
            } catch (Exception e) {
                return;
            }
        }
        str3 = "com.dv.adm";
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+str3)));
        } catch (ActivityNotFoundException e2) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+str3)));
        }
    }

    class MyInterface{
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void error(String title,String msg) {
            new Handler(Looper.getMainLooper()).post(() -> {
                progressDialog.dismiss();
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(title)
                        .setMessage(msg)
                        .setPositiveButton(R.string.ok,null)
                        .show();
            });
        }
    }

    private void openDevFB() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            intent.setData(Uri.parse("fb://profile/100011402500763"));
            startActivity(intent);
        } catch (Exception e) {
            intent.setData(Uri.parse("https://facebook.com/100011402500763"));
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()==R.id.get_source_code)
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.source_code_url))));
        return super.onOptionsItemSelected(item);
    }
}