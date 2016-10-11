package com.wolfteck.smoothtouch;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.IntegerRes;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.format.Time;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class DepthMapActivity extends AppCompatActivity {

    private String m_Filename;
    private StringBuilder m_Gcode;
    private String picturePath;

    private static int RESULT_LOAD_IMAGE = 1;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_depth_map);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        queue = Volley.newRequestQueue(this);

        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
            }
        });

        Button buttonRunGcode = (Button) findViewById(R.id.buttonRunGcode);
        buttonRunGcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DepthMapActivity.this);
                builder.setTitle("G Code Name");
                final EditText input = new EditText(DepthMapActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                String basename = picturePath.substring(picturePath.lastIndexOf("/")+1);
                basename = basename.substring(0,basename.lastIndexOf("."));
                input.setText(basename + ".g");
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        m_Filename = input.getText().toString();

                        final ProgressDialog loading = ProgressDialog.show(DepthMapActivity.this, "Uploading...","Please wait...",false,false);

                        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        url = "http://" + prefs.getString("smoothie_host", "smoothie") + "/upload";

                        StringRequest uploadRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                loading.setProgress(100);
                                loading.dismiss();
                                Toast.makeText(DepthMapActivity.this, response, Toast.LENGTH_SHORT).show();
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                loading.dismiss();
                                Toast.makeText(DepthMapActivity.this, error.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                            }
                        }
                        ) {
                            // Put the Gcode in the POST body
                            @Override
                            public byte[] getBody() throws AuthFailureError {
                                Log.d("uploadRequest","getBody()");
                                TextView textView = (TextView) findViewById(R.id.imgGcode);
                                return(m_Gcode.toString().getBytes());
//                                return(textView.getText().toString().getBytes());
                            }

                            // Set the X-Filename header
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                Log.d("uploadRequest","getHeaders()");
                                // String fileName = Long.toString(System.currentTimeMillis()) + ".g";
                                Map<String, String> params = new HashMap<String, String>();
                                params.put("X-Filename", m_Filename);
                                return params;
                            }
                        };

                        uploadRequest.setRetryPolicy(new DefaultRetryPolicy(60000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                        Log.d("uploadRequest","Adding uploadRequest to queue");
                        queue.add(uploadRequest);

                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.show();
            }
        });

        // TODO: Refactor this to email attachments
        Button buttonEmailGcode = (Button) findViewById(R.id.buttonEmailGcode);
        buttonEmailGcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                TextView textView = (TextView) findViewById(R.id.imgGcode);

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"randall.will@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, "G-Code from SmoothTouch");
  //              i.putExtra(Intent.EXTRA_TEXT, textView.getText());
                i.putExtra(Intent.EXTRA_TEXT, m_Gcode.toString());

                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(DepthMapActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                }


            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Toast.makeText(DepthMapActivity.this, selectedImage.toString(), Toast.LENGTH_SHORT).show();

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            picturePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap imgBitmap = BitmapFactory.decodeFile(picturePath);

            ImageView imageView = (ImageView) findViewById(R.id.imgView);
            imageView.setImageBitmap(imgBitmap);

            int height = imgBitmap.getHeight();
            int width = imgBitmap.getWidth();

            m_Gcode = new StringBuilder();

            m_Gcode.append("G90 ; Absolute mode\nG21 ; Metric mode\nM03 ; Laser on\n");

            double scale = 2.0;

            for (int y = 0; y < height; y++) {
                if (y % 2 == 0) {
                    m_Gcode.append("\nG0 X0 Y").append((height - y - 1)/scale).append(" F3000\n\n");
                    for (int x = 0; x < width; x++) {
                        int color = imgBitmap.getPixel(x, y);
                        int brightness = (int) (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
                        m_Gcode.append("G1 X" + ((x + 1)/scale) + " Y" + ((height - y - 1)/scale) + " S" + (1.0 - brightness / 255.0) + " F3000\n");
                    }
                } else {
                    m_Gcode.append("\nG0 X").append(width / scale).append(" Y" + ((height - y - 1)/scale) + " F3000\n\n");
                    for (int x = width - 1; x >= 0; x--) {
                        int color = imgBitmap.getPixel(x, y);
                        int brightness = (int) (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
                        m_Gcode.append("G1 X" + (x/scale) + " Y" + ((height - y - 1)/scale) + " S" + (1.0 - brightness / 255.0) + " F3000\n");
                    }
                }
            }

            m_Gcode.append("\nM05 ; Laser off\n");

            TextView textView = (TextView) findViewById(R.id.imgGcode);
            //textView.setMovementMethod(new ScrollingMovementMethod());
            //textView.setText(gcode);
        }


    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("DepthMap Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
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
            Toast.makeText(DepthMapActivity.this, cmd, Toast.LENGTH_SHORT).show();
        }

        /* Actually send the request */
        StringRequest postRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(DepthMapActivity.this, response, Toast.LENGTH_SHORT).show();
                if(cmd.equals("M20")) {
                   // Do something if we care about the response
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(DepthMapActivity.this, error.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
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
