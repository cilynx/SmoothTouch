package com.wolfteck.smoothtouch;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class WizardSurfacingActivity extends AppCompatActivity {

    private double mJogScale = 1;
    private MySingleton mSmoothie;
    private ToggleButton mRel;
    private ToggleButton mToolInches;

    public void jogAmount(View v) {
        Button b = (Button) v;
        if(mJogScale == 0.1) {
            mJogScale = 100;
            b.setText("10cm");
        } else if(mJogScale == 100) {
            mJogScale = 10;
            b.setText("1cm");
        } else if(mJogScale == 10) {
            mJogScale = 1;
            b.setText("1mm");
        } else if(mJogScale == 1) {
            mJogScale = 0.1;
            b.setText("0.1mm");
        }
    }

    public void jogXP(View button) { mSmoothie.jogX(mJogScale); }

    public void jogXN(View button) { mSmoothie.jogX(-mJogScale); }

    public void jogYP(View button) { mSmoothie.jogY(mJogScale); }

    public void jogYN(View button) { mSmoothie.jogY(-mJogScale); }

    public void jogZP(View button) { mSmoothie.jogZ(mJogScale); }

    public void jogZN(View button) { mSmoothie.jogZ(-mJogScale); }

    public void zeroWorkspaceDRO(View button) { mSmoothie.zeroWorkspaceDRO(); mRel.setChecked(true); }

    public void setUpperLeft(View button) {
        TextView ul_x = (TextView) findViewById(R.id.upper_left_x);
        ul_x.setText(Double.toString(mSmoothie.getMachineDRO()[0]));

        TextView ul_y = (TextView) findViewById(R.id.upper_left_y);
        ul_y.setText(Double.toString(mSmoothie.getMachineDRO()[1]));

        TextView startDepth = (TextView) findViewById(R.id.start_depth);
        startDepth.setText(Double.toString(mSmoothie.getMachineDRO()[2]));
    }

    public void setLowerRight(View button) {
        TextView lr_x = (TextView) findViewById(R.id.lower_right_x);
        lr_x.setText(Double.toString(mSmoothie.getMachineDRO()[0]));

        TextView lr_y = (TextView) findViewById(R.id.lower_right_y);
        lr_y.setText(Double.toString(mSmoothie.getMachineDRO()[1]));

        TextView endDepth = (TextView) findViewById(R.id.end_depth);
        endDepth.setText(Double.toString(mSmoothie.getMachineDRO()[2]));
    }

    public void setStartDepth(View button) {
        TextView sd = (TextView) findViewById(R.id.start_depth);
        sd.setText(Double.toString(mSmoothie.getMachineDRO()[2]));
    }

    public void setEndDepth(View button) {
        TextView ed = (TextView) findViewById(R.id.end_depth);
        ed.setText(Double.toString(mSmoothie.getMachineDRO()[2]));
    }

    public void surfaceThePart(View button) {

        TextView tv;

        tv = (TextView) findViewById(R.id.upper_left_x);
        double ulx = Double.parseDouble(tv.getText().toString());
        tv = (TextView) findViewById(R.id.upper_left_y);
        double uly = Double.parseDouble(tv.getText().toString());
        tv = (TextView) findViewById(R.id.lower_right_x);
        double lrx = Double.parseDouble(tv.getText().toString());
        tv = (TextView) findViewById(R.id.lower_right_y);
        double lry = Double.parseDouble(tv.getText().toString());
        tv = (TextView) findViewById(R.id.start_depth);
        double sd = Double.parseDouble(tv.getText().toString());
        tv = (TextView) findViewById(R.id.end_depth);
        double ed = Double.parseDouble(tv.getText().toString());
        tv = (TextView) findViewById(R.id.tool_diameter);
        double td = Double.parseDouble(tv.getText().toString());
        tv = (TextView) findViewById(R.id.pass_depth);
        double dpp = Double.parseDouble(tv.getText().toString());

        StringBuilder message = new StringBuilder();

        if(!(ed < sd)) {
            message.append("End Depth must be deeper than Start Depth.\n");
        }

        if(!(ulx < lrx)) {
            message.append("Upper Left must be to the left of Lower Right.\n");
        }

        if(!(uly > lry)) {
            message.append("Upper Left must be higher than Lower Right.\n");
        }

        if(message.toString().isEmpty()) {

            if(mToolInches.isChecked()) { td *= 25.4; }
            double shift = (uly-lry)/((double)Math.ceil(((uly-lry)-td)/td)+1);
            double y = uly;
            double z = sd;
            int count = 0;

            final StringBuilder gcode = new StringBuilder();
            gcode.append("G90 G21\nM17\n");
            gcode.append("G0 Z").append(sd+10).append("\n");
            gcode.append("G0 X").append(ulx).append(" Y").append(uly).append("\n");
            gcode.append("M3\n");

            while(z >= ed) {
                gcode.append("G1 Z").append(z).append("\n");
                count = 0;
                y = uly;
                while (y >= lry) {
                    gcode.append("G1 Y").append(y).append("\n");
                    gcode.append("G1 X");
                    if (count % 2 == 0) {
                        gcode.append(lrx);
                    } else {
                        gcode.append(ulx);
                    }
                    gcode.append("\n");
                    y -= shift;
                    count++;
                }
                if(z == ed) { break; }
                z -= dpp;
                if(z < ed) { z = ed; }
                gcode.append("G0 Z").append(sd).append("\n");
                gcode.append("G0 X").append(ulx).append(" Y").append(uly).append("\n");
            }

            gcode.append("M5\n");
            gcode.append("G0 Z").append(sd).append("\n");
            gcode.append("G0 X").append(ulx).append(" Y").append(uly).append("\n");
            gcode.append("M18\n");

            new AlertDialog.Builder(this)
                    .setTitle("Send to machine?")
                    .setMessage(gcode)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Toast.makeText(WizardSurfacingActivity.this, Long.toString(System.currentTimeMillis()), Toast.LENGTH_LONG).show();
                            String filename = Long.toString(System.currentTimeMillis()) + ".g";
                            mSmoothie.sendFile(gcode.toString(), filename);
                            mSmoothie.playFile(filename);
//                            mSmoothie.deleteFile(filename);
                            //mSmoothie.lineByLine(gcode.toString());
                        }})
                    .setNegativeButton(android.R.string.no, null).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Invalid Parameters!")
                    .setMessage(message.toString())
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_surfacing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSmoothie = MySingleton.getInstance(getApplicationContext());

        ToggleButton halt = (ToggleButton) findViewById(R.id.play_pause);
        halt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSmoothie.halt();
                } else {
                    mSmoothie.reset();
                }
            }
        });

        mRel = (ToggleButton) findViewById(R.id.relative_toggle);
        mRel.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSmoothie.setWorkspaceDRO();
                } else {
                    mSmoothie.setMachineDRO();
                }
            }
        });

        mToolInches = (ToggleButton) findViewById(R.id.tool_unit);
        mToolInches.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TextView tv = (TextView) findViewById(R.id.tool_diameter);
                String text = tv.getText().toString();
                if(!text.isEmpty()) {
                    double value = Double.parseDouble(text);

                    if (isChecked) {
                        tv.setText(Double.toString(value / 25.4));
                    } else {
                        tv.setText(Double.toString(value * 25.4));
                    }
                }
            }
        });

        mSmoothie.watchDRO(new TextView[] {
                (TextView) findViewById(R.id.dro_x),
                (TextView) findViewById(R.id.dro_y),
                (TextView) findViewById(R.id.dro_z)
        });

    }

    @Override
    protected void onDestroy() {
        MySingleton.getInstance(WizardSurfacingActivity.this).stopDRO();
        super.onDestroy();
    }

}
