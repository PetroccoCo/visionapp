/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.samples.vision.ocrreader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.opencsv.CSVWriter;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Main activity demonstrating how to pass extra parameters to an activity that
 * recognizes text.
 */
public class MainActivity extends Activity {

    // Use a compound button so either checkbox or switch widgets work.
    @BindView(R.id.status_message) TextView statusMessage;

    @BindView(R.id.brand_value) EditText brandValue;
    @BindView(R.id.amount_value) EditText amountValue;
    @BindView(R.id.code_value) EditText codeValue;
    @BindView(R.id.pin_value) EditText pinValue;

    @BindView(R.id.detect_text) Button detectText;

    private static final int RC_OCR_CAPTURE = 9003;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_main);
        ButterKnife.bind(this);
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @OnClick(R.id.detect_text)
    public void detect() {
        Intent intent = new Intent(this, OcrCaptureActivity.class);
        intent.putExtra(OcrCaptureActivity.AutoFocus, true);
        intent.putExtra(OcrCaptureActivity.UseFlash, false);

        startActivityForResult(intent, RC_OCR_CAPTURE);
    }

    @OnClick(R.id.save_card)
    public void save() {
        String[] cardRowData = new String[4];
        cardRowData[0] = String.valueOf(brandValue.getText());
        cardRowData[1] = String.valueOf(amountValue.getText());
        cardRowData[2] = String.valueOf(codeValue.getText());
        cardRowData[3] = String.valueOf(pinValue.getText());

        Log.i(TAG, StringUtils.join(cardRowData, ','));
        if (isExternalStorageWritable()) {
            File csvFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "cards.csv");
            try {
                if(!csvFile.exists()) {
                    csvFile.createNewFile();
                }
                CSVWriter writer = new CSVWriter(new FileWriter(csvFile, true));
                writer.writeNext(cardRowData, true);
                writer.close();
                clearFields();
            } catch (IOException e) {
                Log.e(TAG, "Could not open file: "+csvFile.getAbsolutePath()+" for writing", e);
                e.printStackTrace();
            }
        }
    }

    @OnClick(R.id.clear_brand)
    public void clearBrand() {
        brandValue.setText("");
    }

    @OnClick(R.id.clear_amount)
    public void clearAmount(){
        amountValue.setText("");
    }

    private void clearFields() {
        codeValue.setText("");
        pinValue.setText("");
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
            save();
        }
    }

    public boolean isExternalStorageWritable() {
        isStoragePermissionGranted();
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == RC_OCR_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    String text = data.getStringExtra(OcrCaptureActivity.TextBlockObject);
                    statusMessage.setText(R.string.ocr_success);
                    Log.d(TAG, "Text read: " + text);
                    text = text.replaceAll(" ", "");
                    text = text.toUpperCase();
                    if (text.contains("PIN")) {
                        String[] split = text.split("PIN");
                        if(split.length == 2) {
                            codeValue.setText(split[0]);
                            pinValue.setText(split[1]);
                            return;
                        }
                    }
                    if (brandValue.getText().length() == 0) {
                        brandValue.setText(text);
                    } else if (amountValue.getText().length() == 0) {
                        amountValue.setText(text);
                    } else if (codeValue.getText().length() == 0) {
                        codeValue.setText(text);
                    } else if (pinValue.getText().length() == 0) {
                        pinValue.setText(text);
                    }
                } else {
                    statusMessage.setText(R.string.ocr_failure);
                    Log.d(TAG, "No Text captured, intent data is null");
                }
            } else {
                statusMessage.setText(String.format(getString(R.string.ocr_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
