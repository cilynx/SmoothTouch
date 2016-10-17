package com.wolfteck.smoothtouch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    SharedPreferences prefs;
    String url;
    String localCmd;
    String[] parts;
    RequestQueue queue;
    TextView dro_x;
    TextView dro_y;
    TextView dro_z;
    float x;
    float y;
    float z;
    Float x_offset;
    Float y_offset;
    Float z_offset;
    boolean relative;
    ToggleButton rel;
    Toolbar toolbar;

    public void zeroDRO(View view) {
        x_offset = x;
        y_offset = y;
        z_offset = z;
        rel.setChecked(true);
    }

    private void sendCommand(final String cmd) {
        sendCommand(cmd, true);
    }

    private void sendCommand(final String cmd, final boolean show_toast) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        url = "http://" + prefs.getString("smoothie_host", "smoothie") + "/command";

        /* Show the gcode, if we're configured to do that */
        if(prefs.getBoolean("show_codes", true) && show_toast) {
            Toast.makeText(MainActivity.this, cmd, Toast.LENGTH_SHORT).show();
        }

        /* Actually send the request */
        StringRequest postRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                toolbar.setSubtitle("Connected to " + prefs.getString("smoothie_host","smoothie"));
                if(show_toast) {
                    Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
                }
/*                if(cmd.equals("M114.1")) {
                    parts = response.split(" ");
                    x = Float.valueOf(parts[2].split(":")[1]);
                    y = Float.valueOf(parts[3].split(":")[1]);
                    z = Float.valueOf(parts[4].split(":")[1]);
                    dro_x = (TextView) findViewById(R.id.dro_x);
                    dro_y = (TextView) findViewById(R.id.dro_y);
                    dro_z = (TextView) findViewById(R.id.dro_z);
                    if(relative) {
                        dro_x.setText(String.format("%.4f", x - x_offset));
                        dro_y.setText(String.format("%.4f", y - y_offset));
                        dro_z.setText(String.format("%.4f", z - z_offset));
                    } else {
                        dro_x.setText(String.format("%.4f", x));
                        dro_y.setText(String.format("%.4f", y));
                        dro_z.setText(String.format("%.4f", z));
                    }
                    sendCommand("M114.1", false);
                } */
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                toolbar.setSubtitle("Cannot connect to " + prefs.getString("smoothie_host","smoothie") + ".  Please check your hostname / IP.");
                Toast.makeText(MainActivity.this, error.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                if(cmd.equals("M114.1")) {
                    // SystemClock.sleep(5000);
                    sendCommand("M114.1", false);
                }
            }
        }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.d("getBody",cmd);
                localCmd = cmd + "\n";
                return(localCmd.getBytes());
            }
        };

        // Set timeout
        postRequest.setRetryPolicy(new DefaultRetryPolicy(
                60000,  // 60 second timeout
                0,      // Don't retry.  DefaultRetryPolicy.DEFAULT_MAX_RETRIES == 1
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT // 1f
        ));

        // Add the request to the RequestQueue.
        queue.add(postRequest);
    }

    private WebView webView;

    private class WebAppInterface {

        @JavascriptInterface
        public void runCommand(String cmd) {
            sendCommand(cmd);
        }

        @JavascriptInterface
        public void jog(String jog) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            String G = "G0";
            String S = "";

            if(prefs.getString("machine_type","").equals("laser") && prefs.getBoolean("burn_move", false)) {
                S = "S" + Float.parseFloat(prefs.getString("laser_power","0"))/100;
                G = "G1";
            }

            sendCommand("G91" + G + jog + "F" + prefs.getString(jog.substring(0,1).toLowerCase() + "_velocity", "0") + S + "G90");
        }
    }

    private String loadSvg() {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    getAssets().open("jogger_rose.svg")));
            StringBuilder buf = new StringBuilder();
            String s;
            while ((s = input.readLine()) != null) {
                buf.append(s);
                buf.append('\n');
            }
            input.close();
            return buf.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize the request queue
        queue = Volley.newRequestQueue(this);

        // Start the DRO loop
        sendCommand("M114.1", false);

        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        String svg = loadSvg();
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadData(svg, "image/svg+xml", "utf-8");

        rel = (ToggleButton) findViewById(R.id.relative_toggle);
        rel.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    relative = true;
                    if(x_offset == null) { x_offset = x; }
                    if(y_offset == null) { y_offset = y; }
                    if(z_offset == null) { z_offset = z; }
                } else {
                    relative = false;
                }
            }
        });

        ToggleButton light = (ToggleButton) findViewById(R.id.light_toggle);
        light.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sendCommand("M106S100");
                } else {
                    sendCommand("M107");
                }
            }
        });

        ToggleButton laser = (ToggleButton) findViewById(R.id.laser_toggle);
        laser.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sendCommand("M3");
                } else {
                    sendCommand("M5");
                }
            }
        });

        ToggleButton halt = (ToggleButton) findViewById(R.id.play_pause);
        halt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    sendCommand("M112");
                } else {
                    sendCommand("M999");
                }
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_gcode) {
            Intent intent = new Intent(this, GcodeActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_wizard) {
            Intent intent = new Intent(this, WizardActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_depth_map) {
            Intent intent = new Intent(this, DepthMapActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.wolfteck.smoothtouch/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.wolfteck.smoothtouch/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
