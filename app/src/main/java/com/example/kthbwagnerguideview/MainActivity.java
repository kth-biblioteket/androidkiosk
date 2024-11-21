package com.example.kthbwagnerguideview;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    WebView myWeb;
    private DrawerLayout drawerLayout;
    private View triggerArea;
    private EditText urlInput;
    private EditText initialscaleInput;
    private EditText inactivitytimeoutInput;
    private Spinner orientationSpinner;
    private CheckBox fullscreenCheckbox;

    private Handler inactivityHandler;
    private Runnable inactivityRunnable;

    // Variabler för att spara settings i shared preferences
    private static final String PREFS_INACTIVITY_TIMEOUT = "inactivitytimeout";
    private static final String PREFS_NAME = "MyPrefs";
    private static final String PREF_PIN = "pin";
    private static final String PREF_INITIAL_SCALE = "initialscale";
    private static final String PREF_ORIENTATION = "orientation";
    private static final String PREF_FULLSCREEN = "fullscreen";
    private static final String PREF_URL = "url";

    // Settings
    private String savedPin;
    private int savedOrientation;
    private String savedInitialScale;
    private boolean savedFullscreen;
    private String savedUrl;
    private String savedInactivityTimeout;

    private boolean isPinDialogOpen = false; // Flag to check if the dialog is open
    private boolean isPinVerified = false; // Track if the PIN has been verified

    private static final int CLICK_THRESHOLD = 5; // Number of clicks required to open settings
    private static final long TIME_LIMIT = 1000; // Time limit in milliseconds between clicks
    private int clickCount = 0; // Count of clicks
    private long lastClickTime = 0; // Last click timestamp


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
        drawerLayout = findViewById(R.id.drawer_layout);
        triggerArea = findViewById(R.id.trigger_area); // Initialize the trigger area
        initialscaleInput = findViewById(R.id.initialscale_input);
        inactivitytimeoutInput = findViewById(R.id.inactivitytimeout_input);
        urlInput = findViewById(R.id.url_input);
        orientationSpinner = findViewById(R.id.orientation_spinner);
        fullscreenCheckbox = findViewById(R.id.fullscreen_checkbox);
        Button saveButton = findViewById(R.id.save_button); // Get reference to Save button

        //Gör så att javascript kan användas
        myWeb.getSettings().setJavaScriptEnabled(true);
        myWeb.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");

        //Se till att alerts från websidor visas(exvis vid delete av bokning)
        myWeb.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        });
        myWeb.setWebViewClient(new WebViewClient() {
            // Hindra att andra websidor än den initiala kan öppnas
            // Lägg in navigering överst på öppnade sidor istället(onPageFinished)
            //public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            //    return !request.getUrl().toString().equals("savedUrl");
            //}

            // Skapa javascript på laddad websida(lägger till en knapp med länk tillbaks till huvudsida)
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

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
                                "nav.style.backgroundColor = '#ffffff';" +
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
                }

                /*
                if (!url.equals(savedUrl)) {
                    new Handler().postDelayed(() -> {
                            String js = "javascript:(function() {" +
                            "var link = document.createElement('link');" +
                            "link.rel = 'stylesheet';" +
                            "link.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css';" +
                            "document.head.appendChild(link);" +

                            "var nav = document.createElement('div');" +
                            "nav.style.position = 'relative';" + // Change to relative
                            "nav.style.width = '100%';" +
                            "nav.style.backgroundColor = 'transparent';" + // Make background transparent
                            "nav.style.borderBottom = '1px solid #ddd';" +
                            "nav.style.padding = '10px';" +
                            "nav.style.display = 'flex';" +
                            "nav.style.justifyContent = 'space-between';" +
                            "nav.style.alignItems = 'center';" +

                            "var homeButton = document.createElement('button');" +
                            "homeButton.innerHTML = '<i class=\"fas fa-home\"></i>';" +
                            "homeButton.className = 'btn btn-primary';" +
                            "homeButton.style.fontSize = '36px';" +
                            "homeButton.onclick = function() {" +
                            "window.location.href = '" + savedUrl + "';" +
                            "};" +

                            "var backButton = document.createElement('button');" +
                            "backButton.innerHTML = 'Back';" +
                            "backButton.className = 'btn btn-secondary';" +
                            "backButton.onclick = function() {" +
                            "window.history.back();" +
                            "};" +

                            "var forwardButton = document.createElement('button');" +
                            "forwardButton.innerHTML = 'Forward';" +
                            "forwardButton.className = 'btn btn-secondary';" +
                            "forwardButton.onclick = function() {" +
                            "window.history.forward();" +
                            "};" +

                            "nav.appendChild(homeButton);" +
                            "nav.appendChild(backButton);" +
                            "nav.appendChild(forwardButton);" +
                            "document.body.insertBefore(nav, document.body.firstChild);" + // Insert as the first element
                            "})()";
                    view.evaluateJavascript(js, null);
    }, 100);
                }
                 */
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

        // Set listener for URL input changes
        urlInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                savedUrl = urlInput.getText().toString().trim();
                //saveSettings(); // Save updated URL
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

        // Initialize inactivity handler and runnable
        inactivityHandler = new Handler(Looper.getMainLooper());
        inactivityRunnable = () -> {
            myWeb.loadUrl(savedUrl); // Reload URL in WebView
            resetInactivityDetection(); // Restart the inactivity timer after reload
        };

        // Start inactivity detection
        startInactivityDetection();
    }

    private void startInactivityDetection() {
        inactivityHandler.postDelayed(inactivityRunnable, Long.parseLong(savedInactivityTimeout));
    }

    private void resetInactivityDetection() {
        inactivityHandler.removeCallbacks(inactivityRunnable);
        startInactivityDetection();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        resetInactivityDetection();
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
        savedPin = sharedPreferences.getString(PREF_PIN, "1234"); // Default PIN
        savedInitialScale = sharedPreferences.getString(PREF_INITIAL_SCALE, "100");
        savedInactivityTimeout = sharedPreferences.getString(PREFS_INACTIVITY_TIMEOUT, "10000");
        savedOrientation = sharedPreferences.getInt(PREF_ORIENTATION, 0); // Default to portrait
        savedFullscreen = sharedPreferences.getBoolean(PREF_FULLSCREEN, false);
        savedUrl = sharedPreferences.getString(PREF_URL, "https://wagnerguide.com/c/kth/kth"); // Default URL

        // Set the retrieved values to UI components
        orientationSpinner.setSelection(savedOrientation);
        fullscreenCheckbox.setChecked(savedFullscreen);
        urlInput.setText(savedUrl);
        initialscaleInput.setText(savedInitialScale);
        inactivitytimeoutInput.setText(savedInactivityTimeout);

        // Load the URL in WebView
        myWeb.loadUrl(savedUrl);
    }

    private void saveSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Save settings to SharedPreferences
        editor.putString(PREF_PIN, savedPin);
        editor.putString(PREF_INITIAL_SCALE, savedInitialScale);
        editor.putString(PREFS_INACTIVITY_TIMEOUT, savedInactivityTimeout);
        editor.putInt(PREF_ORIENTATION, savedOrientation);
        editor.putBoolean(PREF_FULLSCREEN, savedFullscreen);
        editor.putString(PREF_URL, savedUrl);
        editor.apply();
    }

    private void applySettings() {
        // Apply settings based on saved values
        setInitialScale(savedInitialScale);
        startInactivityDetection();
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
        if (enteredPin.equals(savedPin)) {
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