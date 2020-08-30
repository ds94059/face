package com.microsoft.projectoxford.face.samples.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.StorageHelper;
import com.microsoft.projectoxford.face.samples.persongroupmanagement.PersonGroupListActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by wangjun on 5/24/2016.
 */
public class SettingPersonGroupActivity extends AppCompatActivity {
    private TextView selectedGroup;
    // When the activity is created, set all the member variables to initial state.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_persongroup_menu);
        selectedGroup = (TextView) findViewById(R.id.selected_group);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 讀取選擇的group ID
        File fileDir = getFilesDir();
        BufferedReader reader = null;
        String persongroupid = "";
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileDir + "/selectedGroup.txt"), "UTF-8")); // 指定讀取文件的編碼格式，以免出現中文亂碼
            String str = null;
            while ((str = reader.readLine()) != null) {
                persongroupid = str;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(persongroupid == "")
        {
            selectedGroup.setText("Selected Group: No selected group");
        }
        else
        {
            String groupName =  StorageHelper.getPersonGroupName(persongroupid, SettingPersonGroupActivity.this);
            selectedGroup.setText(getString(R.string.selected_group,groupName));
        }
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
