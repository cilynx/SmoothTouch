package com.wolfteck.smoothtouch;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

public class WizardSurfacingActivity extends AppCompatActivity {

    double jogScale = 1;

    public void jogAmount(View v) {
        Button b = (Button) v;
        if(jogScale == 0.1) {
            jogScale = 100;
            b.setText("10cm");
        } else if(jogScale == 100) {
            jogScale = 10;
            b.setText("1cm");
        } else if(jogScale == 10) {
            jogScale = 1;
            b.setText("1mm");
        } else if(jogScale == 1) {
            jogScale = 0.1;
            b.setText("0.1mm");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard_surfacing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
