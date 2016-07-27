package com.wolfteck.smoothtouch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.AuthFailureError;
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

    private void sendCommand(final String cmd) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = "http://" + prefs.getString("smoothie_host", "smoothie") + "/command";

        /* Show the gcode, if we're configured to do that */
        if(prefs.getBoolean("show_codes", true)) {
            Toast.makeText(MainActivity.this, cmd, Toast.LENGTH_SHORT).show();
        }

        /* Actually send the request */
        RequestQueue queue = Volley.newRequestQueue(this);
        // Request a string response from the provided URL.
        StringRequest postRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, error.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            }
        }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.d("getBody",cmd);
                String localCmd = cmd + "\n";
                return(localCmd.getBytes());
            }
        };
        // Add the request to the RequestQueue.
        queue.add(postRequest);
    }

    private WebView webView;

    private class WebAppInterface {
        /** Show a toast from svg */
//        @JavascriptInterface
//        public void showToast(String toast) {
//            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_LONG).show();
//        }

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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        String svg = loadSvg();
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadData(svg, "image/svg+xml", "utf-8");

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
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
