package com.microsoft.projectoxford.face.samples.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.persongroupmanagement.PersonGroupListActivity;

/**
 * Created by wangjun on 5/24/2016.
 */
public class SettingPersonGroupActivity extends AppCompatActivity {

    // When the activity is created, set all the member variables to initial state.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_persongroup_menu);
    }
    //face to face verification button click
    public void selectPersonGroup(View view)
    {
        Intent intent = new Intent(this, SelectPersonGroupActivity.class);
        startActivity(intent);
    }

    //face to face verification button click
    public void managePersonGroup(View view)
    {
        Intent intent = new Intent(this, PersonGroupListActivity.class);
        startActivity(intent);
    }
}
