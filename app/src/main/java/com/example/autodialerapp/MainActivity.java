package com.example.autodialerapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText edtCallGap, edtJSONData;
    Button btnStartPauseDialer, btnStopDialer;
    TextView txtCurrentStatus, txtCallGapCounter;

    private boolean dialerStatus = false;
    private boolean isPaused = false;

    int seconds=-1;
    String jsonData;
    private CountDownTimer countDownTimer;
    private List<DataItem> dataItemList;
    private int currentIndex;

    private boolean isCallInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtCallGap = findViewById(R.id.call_gap_edit_text);
        edtJSONData = findViewById(R.id.json_data_edit_text);
        btnStartPauseDialer = findViewById(R.id.btn_start_pause);
        btnStopDialer = findViewById(R.id.btn_stop);
        txtCurrentStatus = findViewById(R.id.txt_current_status);
        txtCallGapCounter = findViewById(R.id.txt_call_gap_counter);

        btnStartPauseDialer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(dialerStatus==false && isPaused==false){
                    startDialer();
                } else if (dialerStatus==true && isPaused) {
                    resumeDialer();
                } else if (dialerStatus==true && isPaused==false){
                    pauseDialer();
                }
            }
        });

        btnStopDialer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopDialer();
            }
        });


//        TelephonyCallback.CallStateListener callStateListener = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
//            callStateListener = new TelephonyCallback.CallStateListener() {
//                @Override
//                public void onCallStateChanged(int state) {
//                    if (state == TelephonyManager.CALL_STATE_IDLE) {
//                        // Call has ended
//                        isCallInProgress = false;
//                        dialNextNumber();
//                    }
//                }
//            };
//        }


        TelephonyManager telephonyManager = getSystemService(TelephonyManager.class);
        if (telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);

            if (state == TelephonyManager.CALL_STATE_IDLE && isCallInProgress) {
                // Call has ended
                isCallInProgress = false;
                System.out.println("this fired");
                dialNextNumber();
            }
        }
    };





    private void startDialer() {
        if (!edtCallGap.getText().toString().isEmpty() && Integer.parseInt(edtCallGap.getText().toString())>=0 && !edtJSONData.getText().toString().isEmpty()){
            dialerStatus = true;
            btnStartPauseDialer.setText("Pause Auto Dialer");

            seconds = Integer.parseInt(edtCallGap.getText().toString());
            jsonData = edtJSONData.getText().toString();

            Gson gson = new Gson();
            DataItem[] dataItems = gson.fromJson(jsonData, DataItem[].class);
            dataItemList = Arrays.asList(dataItems);

            // Sort the data items based on priority in ascending order
            dataItemList.sort(Comparator.comparingInt(item -> item.getPriority()));
            btnStartPauseDialer.setBackgroundColor(getResources().getColor(R.color.yellow));
            btnStartPauseDialer.setTextColor(getResources().getColor(R.color.black));

            startCountdown(seconds);




        } else {
            Toast.makeText(this, "Input Error", Toast.LENGTH_SHORT).show();
        }

    }

    private void startCountdown(int seconds) {
        countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                txtCallGapCounter.setText("Call gap counter:    " + secondsRemaining);
            }

            public void onFinish() {
                dialNextNumber();
            }
        }.start();
    }

    private void pauseCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isPaused = true;
    }

    private void resumeCountdown() {
        if (isPaused) {
            startCountdown(seconds);
            isPaused = false;
        }
    }

    private void pauseDialer() {
        btnStartPauseDialer.setText("Start Auto Dialer");
        txtCurrentStatus.setText("Auto Dialer:  Paused");
        btnStartPauseDialer.setBackgroundColor(getResources().getColor(R.color.blue));
        btnStartPauseDialer.setTextColor(getResources().getColor(R.color.white));
        dialerStatus=false;
        pauseCountdown();

    }

    private void resumeDialer(){
        btnStartPauseDialer.setText("Pause Auto Dialer");
        btnStartPauseDialer.setBackgroundColor(getResources().getColor(R.color.yellow));
        btnStartPauseDialer.setTextColor(getResources().getColor(R.color.black));
        resumeCountdown();

    }


    private void stopDialer() {
        countDownTimer.cancel();
        btnStartPauseDialer.setText("Start Auto Dialer");
        dialerStatus=false;
        seconds=-1;
        edtCallGap.setText("");
        edtJSONData.setText("");
        txtCurrentStatus.setText("Auto Dialer:  \nPhone number:  \nPriority order:  ");
        txtCallGapCounter.setText("Call gap counter:    ");
        btnStartPauseDialer.setBackgroundColor(getResources().getColor(R.color.blue));
        btnStartPauseDialer.setTextColor(getResources().getColor(R.color.white));
    }

    private void dialNextNumber() {
        if (currentIndex >= dataItemList.size()) {
            // All numbers have been processed
            txtCallGapCounter.setText("All numbers processed");
            return;
        }
        if (isCallInProgress) {
            // A call is still in progress, wait for it to end
            System.out.println("this ran too");
            return;
        }

        DataItem dataItem = dataItemList.get(currentIndex);
        currentIndex++;

        txtCurrentStatus.setText("Auto Dialer:  Running\nPhone number:  " + dataItem.getNumber() + "\nPriority order:  " + dataItem.getPriority());

        if (dataItem.getStatus().equals("dial")) {
            // Perform dialing operation with dataItem.Number
            // ...

//            countdownTextView.setText("Dialing number: " + dataItem.Number);
            System.out.println("Dialing number: " + dataItem.getNumber());

            String phoneNumber = dataItem.getNumber();
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));

            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                isCallInProgress = true;
                startActivity(intent);
            } else {
                // Request the CALL_PHONE permission
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CALL_PHONE}, 1);
            }


        } else if (dataItem.getStatus().equals("skip")) {

            // Skip operation
            // ...

//            countdownTextView.setText("Skipping number: " + dataItem.Number);
            System.out.println("Skipping number: " + dataItem.getNumber());
            Toast.makeText(this, "Skipping number: " + dataItem.getNumber(), Toast.LENGTH_SHORT).show();

        }
        startCountdown(seconds);
    }


}

