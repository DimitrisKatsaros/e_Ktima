package com.gmail.katsaros.s.dimitris.e_ktima;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;

public class SendEmail extends AppCompatActivity {
    private String TAG = "SendEmailActivity";

    private EditText mEditTextTo;
    private EditText mEditTextSubject;
    private EditText mEditTextMessage;
    private ArrayList<AreaInfo> areas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_send_email);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            ArrayList<Integer> values = (ArrayList<Integer>) extras.getSerializable("areas");
            areas = new ArrayList<>();
            ArrayList<AreaInfo> info = MyJSON.getData(SendEmail.this);
            for (int i = 0; i < values.size(); i++) {
                areas.add(info.get(values.get(i)));
            }
            for (int i = 0; i < areas.size(); i++) {
                Log.d(TAG, "onCreate: area list titles: " + areas.get(i).getTitle());
            }

        }

        mEditTextTo = findViewById(R.id.edit_text_to);
        mEditTextSubject = findViewById(R.id.edit_text_subject);
        mEditTextMessage = findViewById(R.id.edit_text_message);

        Button buttonSend = findViewById(R.id.button_send);
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMail();
            }
        });
    }

    private void sendMail() {

        try {
            ArrayList<String> fileNames = new ArrayList<>();
            for (int i = 0; i < areas.size(); i++) {
                LogFileProvider.createCachedFile(SendEmail.this,
                        areas.get(i).getTitle() + "_area_" + (i + 1) + ".json",
                        new Gson().toJson(areas.get(i)));
                fileNames.add(areas.get(i).getTitle() + "_area_" + (i + 1) + ".json");
            }
            startActivity(getSendEmailIntent(SendEmail.this, fileNames));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Intent getSendEmailIntent(Context context, ArrayList<String> fileNames) {

        String recipientList = mEditTextTo.getText().toString();
        String[] recipients = recipientList.split(",");

        String subject = mEditTextSubject.getText().toString();
        String message = mEditTextMessage.getText().toString();

        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.putExtra(Intent.EXTRA_EMAIL, recipients);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);

        ArrayList<Uri> uris = new ArrayList<>();
        for (int i = 0; i < areas.size(); i++) {
            Uri u = Uri.parse("content://" + LogFileProvider.AUTHORITY + "/"
                    + fileNames.get(i));
            uris.add(u);
            Log.d(TAG, "getSendEmailIntent: " + u);
        }

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.setType("message/rfc822");
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        startActivity(Intent.createChooser(intent, "Choose an email client"));

        return intent;
    }
}