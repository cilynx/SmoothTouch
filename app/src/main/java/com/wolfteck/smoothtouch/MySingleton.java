package com.wolfteck.smoothtouch;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

// https://developer.android.com/training/volley/requestqueue.html#singleton

public class MySingleton {
    private static MySingleton mInstance;
    private RequestQueue mRequestQueue;
    private static Context mCtx;
    private static SharedPreferences prefs;
    private static String mCommandURL;
    private static String mUploadURL;
    private static StringRequest mRequestDRO;

    private static double mMachineX = 0;
    private static double mMachineY = 0;
    private static double mMachineZ = 0;

    private static double mOffsetX = 0;
    private static double mOffsetY = 0;
    private static double mOffsetZ = 0;

    private static boolean mRunDRO = false;
    private static boolean mWorkspaceDRO = false;

    private MySingleton(Context context) {
        mCtx = context.getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mCommandURL = "http://" + prefs.getString("smoothie_host", "smoothie") + "/command";
        mUploadURL = "http://" + prefs.getString("smoothie_host", "smoothie") + "/upload";

        mRequestQueue = getRequestQueue();
    }

    public static synchronized MySingleton getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new MySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if(mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(mCtx);
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void watchDRO(final TextView[] droViews) {
        mRequestDRO = new StringRequest(Request.Method.POST, mCommandURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                String[] parts = response.split(" ");

                mMachineX = Double.valueOf(parts[2].split(":")[1]);
                mMachineY = Double.valueOf(parts[3].split(":")[1]);
                mMachineZ = Double.valueOf(parts[4].split(":")[1]);

                droViews[0].setText(Double.toString(getDRO()[0]));
                droViews[1].setText(Double.toString(getDRO()[1]));
                droViews[2].setText(Double.toString(getDRO()[2]));

                if(mRunDRO) { addToRequestQueue(mRequestDRO); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //SystemClock.sleep(5000);
            }
        })
        {
            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.d("getBody()","Fetching DRO");
                String cmd = "M114.1\n";
                return(cmd.getBytes());
            }
        };

        startDRO();
    }

    public void startDRO() {
        if(!mRunDRO) {
            mRunDRO = true;
            addToRequestQueue(mRequestDRO);
        }
    }

    public void stopDRO() {
        mRunDRO = false;
    }

    public void zeroWorkspaceDRO() {
        mOffsetX = mMachineX;
        mOffsetY = mMachineY;
        mOffsetZ = mMachineZ;
        setWorkspaceDRO();
    }

    public void setWorkspaceDRO() {
        mWorkspaceDRO = true;
    }

    public void setMachineDRO() {
        mWorkspaceDRO = false;
    }

    public double[] getMachineDRO() {
        return new double[] {mMachineX, mMachineY, mMachineZ};
    }

    public double[] getWorkspaceDRO() {
        return new double[] {mMachineX - mOffsetX, mMachineY - mOffsetY, mMachineZ - mOffsetZ};
    }

    public double[] getDRO() {
        if(mWorkspaceDRO) {
            return(getWorkspaceDRO());
        } else {
            return(getMachineDRO());
        }
    }

    public void jogX(double length) {
        sendCommand("G91 G0 X" + length + " F" + prefs.getString("x_velocity","0") + " G90");
    }

    public void jogY(double length) {
        sendCommand("G91 G0 Y" + length + " F" + prefs.getString("y_velocity","0") + " G90");
    }

    public void jogZ(double length) {
        sendCommand("G91 G0 Z" + length + " F" + prefs.getString("z_velocity","0") + " G90");
    }

    public void halt() {
        sendCommand("M112");
    }

    public void reset() {
        sendCommand("M999");
    }

    public void lineByLine(String gcode) {
        StringTokenizer st = new StringTokenizer(gcode, "\n");
        stopDRO();
        sendCommand(st.nextToken(), st);
    }

    public void playFile(final String filename) {
        sendCommand("M32 " + filename);
    }

    public void deleteFile(final String filename) {
        sendCommand("M400 M30 " + filename);
    }

    public void sendFile(final String gcode, final String filename) {

//        final ProgressDialog loading = ProgressDialog.show(mCtx.getApplicationContext(), "Uploading...","Please wait...",false,false);

        Toast.makeText(mCtx, "Sending " + filename, Toast.LENGTH_SHORT).show();

        StringRequest uploadRequest = new StringRequest(Request.Method.POST, mUploadURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
//                loading.setProgress(100);
//                loading.dismiss();
                Toast.makeText(mCtx, response, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                loading.dismiss();
                Toast.makeText(mCtx, error.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            }
        }
        ) {
            // Put the Gcode in the POST body
            @Override
            public byte[] getBody() throws AuthFailureError {
                return(gcode.getBytes());
            }

            // Set the X-Filename header
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Log.d("uploadRequest","getHeaders()");
                // String fileName = Long.toString(System.currentTimeMillis()) + ".g";
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Filename", filename);
                return params;
            }
        };

        uploadRequest.setRetryPolicy(new DefaultRetryPolicy(60000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        addToRequestQueue(uploadRequest);
    }

    private void sendCommand(final String command) {
        sendCommand(command, null);
    }

    private void sendCommand(final String command, final StringTokenizer st) {
        Toast.makeText(mCtx, command, Toast.LENGTH_SHORT).show();
        StringRequest request = new StringRequest(Request.Method.POST, mCommandURL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("onResponse",response);
                if(st != null) {
                    if(st.hasMoreTokens()) {
                        sendCommand(st.nextToken() + " M400", st);
                    } else {
                        startDRO();
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Should we retry forever or...?  Maybe a user preference?
                Toast.makeText(mCtx, error.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
        })
        {
            @Override
            public byte[] getBody() throws AuthFailureError {
                Log.d("getBody",command);
                return(command.concat("\n").getBytes());
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                60000,  // 60 second timeout
                0,      // Don't retry.  DefaultRetryPolicy.DEFAULT_MAX_RETRIES == 1
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT // 1f
        ));

        addToRequestQueue(request);
    }

}
