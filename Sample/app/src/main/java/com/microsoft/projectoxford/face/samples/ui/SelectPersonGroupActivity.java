//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Face-Android
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.microsoft.projectoxford.face.samples.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.projectoxford.face.samples.R;
import com.microsoft.projectoxford.face.samples.helper.StorageHelper;
import com.microsoft.projectoxford.face.samples.persongroupmanagement.PersonGroupListActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by wangjun on 5/24/2016.
 */
public class SelectPersonGroupActivity extends AppCompatActivity {

    PersonGroupListAdapter mPersonGroupListAdapter;
    String mPersonGroupId;

    // When the activity is created, set all the member variables to initial state.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_group);


        //initialize the personGroup
        ListView listView = (ListView) findViewById(R.id.list_person_groups_identify);
        mPersonGroupListAdapter = new PersonGroupListAdapter();
        listView.setAdapter(mPersonGroupListAdapter);


        setPersonGroupSelected(MainActivity.personGroupPosition);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ListView listView = (ListView) findViewById(R.id.list_person_groups_identify);
        //mPersonGroupListAdapter = new PersonGroupListAdapter();
        //listView.setAdapter(mPersonGroupListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.personGroupPosition=position;
                setPersonGroupSelected(position);
            }
        });

        if (mPersonGroupListAdapter.personGroupIdList.size() != 0) {
            setPersonGroupSelected(0);
        } else {
            setPersonGroupSelected(-1);
        }
    }

    void setPersonGroupSelected(int position) {
        TextView textView = (TextView) findViewById(R.id.text_person_group_selected);
        if (position > 0) {
            String personGroupIdSelected = mPersonGroupListAdapter.personGroupIdList.get(position);
            mPersonGroupListAdapter.personGroupIdList.set(
                    position, mPersonGroupListAdapter.personGroupIdList.get(0));
            mPersonGroupListAdapter.personGroupIdList.set(0, personGroupIdSelected);
            ListView listView = (ListView) findViewById(R.id.list_person_groups_identify);
            listView.setAdapter(mPersonGroupListAdapter);
            setPersonGroupSelected(0);
        } else if (position < 0) {
            //setIdentifyButtonEnabledStatus(false);
            textView.setTextColor(Color.RED);
            textView.setText(R.string.no_person_group_selected_for_identification_warning);
        } else {
            mPersonGroupId = mPersonGroupListAdapter.personGroupIdList.get(0);
            String personGroupName = StorageHelper.getPersonGroupName(
                    mPersonGroupId, SelectPersonGroupActivity.this);
            //refreshIdentifyButtonEnabledStatus();
            textView.setTextColor(Color.BLACK);
            textView.setText(String.format("Person group to use: %s", personGroupName));
        }
    }


    // The adapter of the ListView which contains the person groups.
    private class PersonGroupListAdapter extends BaseAdapter {
        List<String> personGroupIdList;

        // Initialize with detection result.
        PersonGroupListAdapter() {
            personGroupIdList = new ArrayList<>();

            Set<String> personGroupIds
                    = StorageHelper.getAllPersonGroupIds(SelectPersonGroupActivity.this);

            for (String personGroupId: personGroupIds) {
                personGroupIdList.add(personGroupId);
                if (mPersonGroupId != null && personGroupId.equals(mPersonGroupId)) {
                    personGroupIdList.set(
                            personGroupIdList.size() - 1,
                            mPersonGroupListAdapter.personGroupIdList.get(0));
                    mPersonGroupListAdapter.personGroupIdList.set(0, personGroupId);
                }
            }
        }

        @Override
        public int getCount() {
            return personGroupIdList.size();
        }

        @Override
        public Object getItem(int position) {
            return personGroupIdList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_person_group, parent, false);
            }
            convertView.setId(position);

            // set the text of the item
            String personGroupName = StorageHelper.getPersonGroupName(
                    personGroupIdList.get(position), SelectPersonGroupActivity.this);
            int personNumberInGroup = StorageHelper.getAllPersonIds(
                    personGroupIdList.get(position), SelectPersonGroupActivity.this).size();
            ((TextView)convertView.findViewById(R.id.text_person_group)).setText(
                    String.format(
                            "%s (Person count: %d)",
                            personGroupName,
                            personNumberInGroup));

            if (position == 0) {
                ((TextView)convertView.findViewById(R.id.text_person_group)).setTextColor(
                        Color.parseColor("#3399FF"));
            }

            return convertView;
        }
    }
}
