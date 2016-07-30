package com.wolfteck.smoothtouch;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

public class GcodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gcode);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        queue = Volley.newRequestQueue(this);
        sendCommand("M20");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //startActivity(new Intent(getActivity(), SettingsActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    // This code is so not DRY that it isn't even funny
    // http://stackoverflow.com/questions/35168981/how-can-i-share-code-between-multiple-activities-in-android
    SharedPreferences prefs;
    String url, localCmd;
    RequestQueue queue;
    ArrayAdapter adapter;
    ListView listView;

    private void sendCommand(final String cmd) {
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        url = "http://" + prefs.getString("smoothie_host", "smoothie") + "/command";

        /* Show the gcode, if we're configured to do that */
        if(prefs.getBoolean("show_codes", true)) {
            Toast.makeText(GcodeActivity.this, cmd, Toast.LENGTH_SHORT).show();
        }

        /* Actually send the request */
        StringRequest postRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(GcodeActivity.this, response, Toast.LENGTH_SHORT).show();
                ArrayList<String> files = new ArrayList<String>();
                for (String line: response.split("\n")) {
                    if(line.contains(".g") || line.contains(".ngc")) {
                        files.add(line);
                    }
                }
                Collections.sort(files); 

                adapter = new ArrayAdapter<String> (GcodeActivity.this, R.layout.list_item, files.toArray(new String[files.size()]));
                listView = (ListView) findViewById(R.id.gcode_file_list);
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        String gcode_file = (String)adapter.getItem(i);
                        sendCommand("M32 " + gcode_file);
                    }
                });
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(GcodeActivity.this, error.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
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
        // Add the request to the RequestQueue.
        queue.add(postRequest);
    }
}
