package com.example.kthbwagnerguideview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    WebView myWeb;
    private DrawerLayout drawerLayout;
    private View triggerArea;
    private EditText urlInput;
    private EditText pincodeInput;
    private EditText initialscaleInput;
    private EditText inactivitytimeoutInput;
    private EditText inactivitytimeoutwebInput;
    private Spinner orientationSpinner;
    private CheckBox fullscreenCheckbox;
    private CheckBox splashscreenCheckbox;
    private CheckBox splashscreenvideoCheckbox;

    private Handler inactivityHandler;
    private Runnable inactivityRunnable;

    private Handler inactivitywebHandler;
    private Runnable inactivitywebRunnable;

    // Variabler för att spara settings i shared preferences
    private static final String PREFS_INACTIVITY_TIMEOUT = "inactivitytimeout";
    private static final String PREFS_INACTIVITY_TIMEOUT_WEB = "inactivitytimeoutweb";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String PREF_PIN = "pin";
    private static final String PREF_INITIAL_SCALE = "initialscale";
    private static final String PREF_ORIENTATION = "orientation";
    private static final String PREF_FULLSCREEN = "fullscreen";
    private static final String PREF_SPLASHSCREEN = "splashscreen";
    private static final String PREF_SPLASHSCREENVIDEO = "splashscreenvideo";
    private static final String PREF_URL = "url";

    // Settings
    private String savedPincode;
    private int savedOrientation;
    private String savedInitialScale;
    private boolean savedFullscreen;
    private boolean savedSplashscreen;
    private boolean savedSplashscreenvideo;
    private String savedUrl;
    private String savedInactivityTimeout;
    private String savedInactivityTimeoutWeb;

    private boolean isPinDialogOpen = false; // Flag to check if the dialog is open
    private boolean isPinVerified = false; // Track if the PIN has been verified

    private static final int CLICK_THRESHOLD = 5; // Number of clicks required to open settings
    private static final long TIME_LIMIT = 1000; // Time limit in milliseconds between clicks
    private int clickCount = 0; // Count of clicks
    private long lastClickTime = 0; // Last click timestamp

    private boolean screentouched = false;

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Möjliggör debug i chrome
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        setContentView(R.layout.activity_main);

        //Hämta webview etc från xml
        myWeb = findViewById(R.id.myWeb);
        ConstraintLayout myMain = findViewById(R.id.main);

        myWeb.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        drawerLayout = findViewById(R.id.drawer_layout);
        triggerArea = findViewById(R.id.trigger_area); // Initialize the trigger area
        initialscaleInput = findViewById(R.id.initialscale_input);
        inactivitytimeoutInput = findViewById(R.id.inactivitytimeout_input);
        inactivitytimeoutwebInput = findViewById(R.id.inactivitytimeoutweb_input);
        urlInput = findViewById(R.id.url_input);
        orientationSpinner = findViewById(R.id.orientation_spinner);
        fullscreenCheckbox = findViewById(R.id.fullscreen_checkbox);
        splashscreenCheckbox = findViewById(R.id.splashscreen_checkbox);
        splashscreenvideoCheckbox = findViewById(R.id.splashscreenvideo_checkbox);
        pincodeInput = findViewById(R.id.pincode_input);
        Button saveButton = findViewById(R.id.save_button); // Get reference to Save button

        myMain.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                myMain.getWindowVisibleDisplayFrame(r);
                int screenHeight = myMain.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // If keyboard is shown
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) myWeb.getLayoutParams();
                    params.bottomMargin = keypadHeight;
                    myWeb.setLayoutParams(params);
                } else {
                    ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) myWeb.getLayoutParams();
                    params.bottomMargin = 0;
                    myWeb.setLayoutParams(params);
                }
            }
        });

        // Visa loggen när en knapp klickas
        Button showLogButton = findViewById(R.id.showLogButton);  // Se till att ha en knapp i din layout
        showLogButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LogActivity.class);
            startActivity(intent);
        });

        //Gör så att javascript kan användas
        myWeb.getSettings().setJavaScriptEnabled(true);
        myWeb.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

        myWeb.addJavascriptInterface(new WebAppInterface(this), "Android");

        //Se till att alerts från websidor visas(exvis vid delete av bokning)
        myWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        });
        myWeb.setWebViewClient(new WebViewClient() {
            // Hindra att andra websidor än den initiala kan öppnas
            //public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //    return !request.getUrl().toString().equals("savedUrl");
            //}

            // Skapa javascript på laddad websida(lägger till en knapp med länk tillbaks till huvudsida)
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Om det inte är den initial sidan så visa en navigation
                if (!url.equals(savedUrl)) {
                    new Handler().postDelayed(() -> {
                            String js = "javascript:(function() {" +
                                // Function to get URL parameter
                                "function getParameterByName(name, url) {" +
                                "    if (!url) url = window.location.href;" +
                                "    name = name.replace(/[\\[\\]]/g, '\\\\$&');" +
                                "    var regex = new RegExp('[?&]' + name + '(=([^&#]*)|&|#|$)')," +
                                "        results = regex.exec(url);" +
                                "    if (!results) return null;" +
                                "    if (!results[2]) return '';" +
                                "    return decodeURIComponent(results[2].replace(/\\+/g, ' '));" +
                                "}" +

                                "var lang = getParameterByName('lang');" +

                                "var homeText = 'Library Map';" +
                                "if (lang === 'sv') {" +
                                "    homeText = 'Karta över biblioteket';" +
                                "}" +

                                "var link = document.createElement('link');" +
                                "link.rel = 'stylesheet';" +
                                "link.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css';" +
                                "document.head.appendChild(link);" +
                                "var nav = document.createElement('div');" +
                                "nav.style.position = 'fixed';" +
                                "nav.style.top = '0';" +
                                "nav.style.left = '0';" +
                                "nav.style.backgroundColor = 'transparent';" +
                                "nav.style.padding = '10px';" +
                                "nav.style.zIndex = '1000';" +
                                "nav.style.display = 'flex';" +
                                "nav.style.justifyContent = 'space-between';" +
                                "nav.style.alignItems = 'center';" +
                                "var homeButton = document.createElement('button');" +
                                "homeButton.className = 'btn btn-sprimary';" +
                                "homeButton.style.position = 'relative';" +
                                "homeButton.style.color = '#ffffff';" +
                                "homeButton.style.width = '150px';" +
                                "homeButton.style.height = '100px';" +
                                "homeButton.style.backgroundColor = '#d02f80';" +
                                "homeButton.onclick = function() {" +
                                        "window.location.href = '" + savedUrl + "';" +
                                "};" +
                                "homeButton.innerHTML = '<i class=\"fas fa-location-dot\" style=\"color:#ffffff26;font-size: 70px; position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);\"></i><span style=\"font-size: 20px;font-weight: 700\">' + homeText +'</span>';"  +
                                "nav.appendChild(homeButton);" +
                                "document.body.appendChild(nav);" +
                            "})()";
                            view.evaluateJavascript(js, null);
                    }, 100);
                    Log.d("timer", "onPageFinished web");
                    // Starta timer för inaktivitet för extern websida
                    startWebInactivityDetection();
                } else {
                    // Funktion för att kunna logga användaraktivitet(klick på websidans element)
                    new Handler().postDelayed(() -> {
                        String js =
                                "if (!window.hasLoggedClickEvent) {" +
                                "  document.addEventListener('click', function(event) {" +
                                "    let element = event.target;" +
                                "    let details = {" +
                                "        tag: element.tagName," +
                                "        id: element.id || null," +
                                "        class: element.className || null," +
                                "        text: element.innerText.trim() || null," +
                                "        attributes: {}" +
                                "    };" +
                                "    for (let attr of element.attributes) {" +
                                "        details.attributes[attr.name] = attr.value;" +
                                "    }" +
                                "    if (typeof Android !== 'undefined') {" +
                                "        Android.logActivity(JSON.stringify(details));" +
                                "    } else {" +
                                "        console.log('Android interface not available:', details);" +
                                "    }" +
                                "  });" +
                                "  window.hasLoggedClickEvent = true;" +
                                "}";
                        view.evaluateJavascript(js, null);
                    }, 100);
                    Log.d("timer", "onPageFinished main");
                    // Starta timer för inaktivitet för huvudsidan
                    startInactivityDetection();
                }
            }
        });

        // Disable long press context menu
        myWeb.setOnLongClickListener(v -> true);
        myWeb.setLongClickable(false);

        // Hämta spinner options för orientation från string.xml
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.orientation_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        orientationSpinner.setAdapter(adapter);

        // Hämta skärmens upplösning
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;   // Bredd i pixlar
        int height = displayMetrics.heightPixels; // Höjd i pixlar

        // Hitta TextView och sätt upplösningen
        TextView resolutionText = findViewById(R.id.resolutionTextView);
        resolutionText.setText("Upplösning: " + width + " x " + height);
        // Ladda settings
        loadSettings();

        disableSwipeToOpenDrawer();
        setupClickListener();

        saveButton.setOnClickListener(v -> {
            saveSettings(); // Save the settings
            applySettings();
            drawerLayout.closeDrawer(Gravity.LEFT); // Close the drawer
        });

        // Set listener for orientation selection
        orientationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                savedOrientation = position; // Update saved orientation value
                //setOrientation(position); // Apply new orientation
                //saveSettings(); // Save settings
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set listener for fullscreen checkbox
        fullscreenCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savedFullscreen = isChecked; // Update saved fullscreen value
            //saveSettings(); // Save settings
        });

        // Set listener for splashscreen checkbox
        splashscreenCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savedSplashscreen = isChecked; // Update saved fullscreen value
            //saveSettings(); // Save settings
        });

        // Set listener for splashscreen checkbox
        splashscreenvideoCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savedSplashscreenvideo = isChecked; // Update saved fullscreen value
            //saveSettings(); // Save settings
        });

        // Set listener for URL input changes
        urlInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savedUrl = urlInput.getText().toString().trim();
            }
        });

        // Set listener for Pincode input changes
        pincodeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savedPincode = pincodeInput.getText().toString().trim();
            }
        });

        // Set listener for Scale input changes
        initialscaleInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savedInitialScale = initialscaleInput.getText().toString().trim();
            }
        });

        // Set listener for Timeout input changes
        inactivitytimeoutInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savedInactivityTimeout = inactivitytimeoutInput.getText().toString().trim();
            }
        });

        // Set listener for Timeout input changes
        inactivitytimeoutwebInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savedInactivityTimeoutWeb = inactivitytimeoutwebInput.getText().toString().trim();
            }
        });



        // Initialisera inaktivitethanterare för huvudsidan
        inactivityHandler = new Handler(Looper.getMainLooper());

        // Initialisera inaktivitethanterare för externa URL:er
        inactivitywebHandler = new Handler(Looper.getMainLooper());

    }

    //Timer för inaktivitet som startar när en huvudsidan  laddats i webview
    private void startInactivityDetection() {
        if (inactivityRunnable == null) {  // Kontrollera om en timer redan är igång
            // När tiden går ut så stoppas timern för main //
            // om splash är enablqat så avslutas main och splash startas
            // Om splash inte är enablat så laddas huvudsidan om
            inactivityRunnable = () -> {
                // Hämta preferenser
                SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                boolean isSplashEnabled = sharedPreferences.getBoolean(PREF_SPLASHSCREEN, false);

                if (isSplashEnabled) {
                    // Stoppa timer för main
                    Log.d("timer", "Inactivity timer stopped for main page");
                    inactivityHandler.removeCallbacks(inactivityRunnable);
                    inactivityRunnable = null;
                    Intent intent = new Intent(this, SplashActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish(); // Stänger den nuvarande aktiviteten
                } else {
                    resetInactivityDetection();
                    Log.d("timer", "screentouched" + screentouched);
                    if (screentouched) {
                        myWeb.loadUrl(savedUrl);  // Ladda om huvudsidan
                    }
                    // Reset att användaren inte har rört skärmen
                    screentouched = false;
                }
            };

            // Starta timern
            inactivityHandler.postDelayed(inactivityRunnable, Long.parseLong(savedInactivityTimeout));
            Log.d("timer", "Inactivity timer started for main page");
        } else {
            Log.d("timer", "Inactivity timer is already running for main page");
        }
    }

    private void resetInactivityDetection() {
        if (inactivityRunnable != null) {
            inactivityHandler.removeCallbacks(inactivityRunnable);
            inactivityHandler.postDelayed(inactivityRunnable, Long.parseLong(savedInactivityTimeout));
            Log.d("timer", "Inactivity timer reset for main page");
        }
    }

    //Timer för inaktivitet som startar när en extern sida laddats i webview
    private void startWebInactivityDetection() {
        if (inactivitywebRunnable == null) {  // Kontrollera om en timer redan är igång
            // När tiden går ut så stoppas timern för web och statar om timern för main.
            inactivitywebRunnable = () -> {
                // Reset att användaren inte har rört skärmen
                screentouched = false;
                // Stoppa timer för web
                inactivitywebHandler.removeCallbacks(inactivitywebRunnable);
                inactivitywebRunnable = null;
                Log.d("timer", "Inactivity timer stopped for external URL");
                //Reset timer för main
                resetInactivityDetection();
                if (!Objects.equals(myWeb.getUrl(), savedUrl)) {
                    myWeb.loadUrl(savedUrl);  // Ladda om huvudsidan
                }
            };
            // Starta timern för externa URL:er
            inactivitywebHandler.postDelayed(inactivitywebRunnable, Long.parseLong(savedInactivityTimeoutWeb));
            Log.d("timer", "Inactivity timer started for external URL");
        } else {
            Log.d("timer", "Inactivity timer is already running for external URL");
        }



    }

    private void resetWebInactivityDetection() {
        if (inactivitywebRunnable != null) {
            inactivitywebHandler.removeCallbacks(inactivitywebRunnable);
            inactivitywebHandler.postDelayed(inactivitywebRunnable, Long.parseLong(savedInactivityTimeoutWeb));
            Log.d("timer", "Inactivity timer reset for external URL");
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        screentouched = true;
        Log.d("timer", "reset timeout from touchevent");
        // Starta om timers
        resetInactivityDetection();
        resetWebInactivityDetection();
        return super.dispatchTouchEvent(ev);
    }

    private void setupClickListener() {
        triggerArea.setOnClickListener(view -> {
            long currentTime = System.currentTimeMillis();

            // Check if the time since the last click is within the limit
            if (currentTime - lastClickTime <= TIME_LIMIT) {
                clickCount++; // Increment the click count
            } else {
                clickCount = 1; // Reset count if the time limit has passed
            }

            lastClickTime = currentTime; // Update last click time

            // Check if the click count has reached the threshold
            if (clickCount >= CLICK_THRESHOLD) {
                if (isPinVerified) {
                    drawerLayout.openDrawer(Gravity.LEFT); // Open the drawer if the PIN is verified
                } else {
                    promptForPin(); // Show PIN dialog if not verified
                }
                clickCount = 0; // Reset the click count after opening the drawer
            }
        });
    }

    private void disableSwipeToOpenDrawer() {
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Do nothing to prevent swipe from opening
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Do nothing
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                isPinVerified = false; // Set the flag to false
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // Do nothing
            }
        });

        // Set the drawer lock mode to LOCKED_CLOSED to prevent opening by swipe
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply settings when the activity is resumed
        applySettings();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     *
     */
    private void loadSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load settings from SharedPreferences
        savedPincode = sharedPreferences.getString(PREF_PIN, "1234"); // Default PIN
        savedInitialScale = sharedPreferences.getString(PREF_INITIAL_SCALE, "100");
        savedInactivityTimeout = sharedPreferences.getString(PREFS_INACTIVITY_TIMEOUT, "60000");
        savedInactivityTimeoutWeb = sharedPreferences.getString(PREFS_INACTIVITY_TIMEOUT_WEB, "30000");
        savedOrientation = sharedPreferences.getInt(PREF_ORIENTATION, 1); // Default to landscape
        savedFullscreen = sharedPreferences.getBoolean(PREF_FULLSCREEN, true);
        savedSplashscreen = sharedPreferences.getBoolean(PREF_SPLASHSCREEN, false);
        savedSplashscreenvideo = sharedPreferences.getBoolean(PREF_SPLASHSCREENVIDEO, false);
        savedUrl = sharedPreferences.getString(PREF_URL, "https://wagnerguide.com/c/kth/kth"); // Default URL

        // Set the retrieved values to UI components
        orientationSpinner.setSelection(savedOrientation);
        fullscreenCheckbox.setChecked(savedFullscreen);
        splashscreenCheckbox.setChecked(savedSplashscreen);
        splashscreenvideoCheckbox.setChecked(savedSplashscreenvideo);
        urlInput.setText(savedUrl);
        pincodeInput.setText(savedPincode);
        initialscaleInput.setText(savedInitialScale);
        inactivitytimeoutInput.setText(savedInactivityTimeout);

        // Load the URL in WebView
        myWeb.loadUrl(savedUrl);
    }

    private void saveSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save settings to SharedPreferences
        editor.putString(PREF_PIN, savedPincode);
        editor.putString(PREF_INITIAL_SCALE, savedInitialScale);
        editor.putString(PREFS_INACTIVITY_TIMEOUT, savedInactivityTimeout);
        editor.putInt(PREF_ORIENTATION, savedOrientation);
        editor.putBoolean(PREF_FULLSCREEN, savedFullscreen);
        editor.putBoolean(PREF_SPLASHSCREEN, savedSplashscreen);
        editor.putBoolean(PREF_SPLASHSCREENVIDEO, savedSplashscreenvideo);
        editor.putString(PREF_URL, savedUrl);
        editor.apply();
    }

    private void applySettings() {
        // Apply settings based on saved values
        setInitialScale(savedInitialScale);
        setOrientation(savedOrientation);
        applyFullscreen(savedFullscreen);
        myWeb.loadUrl(savedUrl); // Reload URL in WebView
    }

    private void applyFullscreen(boolean isFullscreen) {
        if (isFullscreen) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }

    private void promptForPin() {
        // Create a dialog for PIN entry
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter PIN");

        // Set up the input
        final EditText pinInput = new EditText(this);
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(pinInput);

        // Set up the buttons
        builder.setPositiveButton("Verify", null); // Initially set to null, we will handle the action manually
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            isPinDialogOpen = false; // Reset the flag if the dialog is canceled
            dialog.cancel();
        });


        builder.setOnDismissListener(dialog -> {
            isPinDialogOpen = false; // Reset the flag when the dialog is dismissed
        });

        // Create the AlertDialog instance
        AlertDialog dialog = builder.create();

        dialog.show(); // Show the dialog

        // Set a listener for the "Verify" button after dialog is shown
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String enteredPin = pinInput.getText().toString();
            if (verifyPin(enteredPin)) {
                dialog.dismiss(); // Dismiss the dialog if the PIN is verified
            }
        });
    }

    private boolean verifyPin(String enteredPin) {
        Log.d("UserActivity", savedPincode);
        if (enteredPin.equals(savedPincode)) {
            Toast.makeText(this, "PIN Verified!", Toast.LENGTH_SHORT).show();
            drawerLayout.openDrawer(Gravity.LEFT);
            isPinVerified = true; // Set the flag to true
            return true;
        } else {
            Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void setInitialScale(String scale) {
        myWeb.setInitialScale(Integer.parseInt(scale));
    }

    private void setOrientation(int position) {
        switch (position) {
            case 0: // Portrait
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case 1: // Landscape
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            default:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
        }
    }

    private void hideSystemUI() {
        // Enables immersive full-screen mode
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    public static class WebAppInterface {
        Context context;

        // Filnamn för loggen
        private static final String LOG_FILE_NAME = "webview_logs.txt";

        WebAppInterface(Context context) {
            this.context = context;
        }

        @JavascriptInterface
        // Anropas av tillagda javascript på laddade websidor i webview
        public void logActivity(String data) {
            Log.d("UserActivity", data);
            // Parse JSON and handle details
            try {
                JSONObject json = new JSONObject(data);
                String tag = json.getString("tag");
                String id = json.optString("id", "no-id");
                String className = json.optString("class", "no-class");
                String text = json.optString("text", "no-text");
                /*
                Log.d("ElementDetails", "Tag: " + tag + ", ID: " + id +
                        ", Class: " + className + ", Text: " + text);

                */
                saveLogToFile("ElementDetails: " + "Tag: " + tag + ", ID: " + id + ", Class: " + className + ", Text: " + text);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Metod för att spara loggen till en fil
        private void saveLogToFile(String log) {
            // Hämta katalogen där loggen ska sparas
            File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

            // Kontrollera om katalogen finns, skapa den om inte
            if (directory != null && !directory.exists()) {
                directory.mkdirs(); // Skapar katalogen om den inte existerar
            }

            // Skapa filen i katalogen
            File logFile = new File(directory, LOG_FILE_NAME);

            try {
                // Använd FileWriter för att öppna filen i append-läge
                FileWriter writer = new FileWriter(logFile, true); // true för att lägga till i slutet av filen
                writer.append(log).append("\n");
                writer.flush();
                writer.close();

                Log.d("WebAppInterface", "Log saved: " + log);
            } catch (IOException e) {
                Log.e("WebAppInterface", "Failed to save log", e);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fullscreenCheckbox.isChecked()) {
            hideSystemUI();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Save fullscreen preference
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_FULLSCREEN, fullscreenCheckbox.isChecked());
        editor.apply();
    }
}