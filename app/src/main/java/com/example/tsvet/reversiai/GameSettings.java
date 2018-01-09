package com.example.tsvet.reversiai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class GameSettings extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
    }
}