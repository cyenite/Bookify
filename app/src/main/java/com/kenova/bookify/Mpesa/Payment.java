package com.kenova.bookify.Mpesa;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;

import android.content.Intent;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.kenova.bookify.Model.AccessToken;
import com.kenova.bookify.Model.STKPush;
import com.kenova.bookify.R;
import com.kenova.bookify.Services.DarajaApiClient;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;


import butterknife.BindView;
import butterknife.ButterKnife;

import static com.kenova.bookify.Constants.Constants.BUSINESS_SHORT_CODE;
import static com.kenova.bookify.Constants.Constants.CALLBACKURL;
import static com.kenova.bookify.Constants.Constants.PARTYB;
import static com.kenova.bookify.Constants.Constants.PASSKEY;
import static com.kenova.bookify.Constants.Constants.TRANSACTION_TYPE;

public class Payment extends AppCompatActivity implements View.OnClickListener {
    @BindView(R.id.phone)
    EditText mNumber;
    @BindView(R.id.BookPrice)
    EditText mAmount;
    @BindView(R.id.btn_continue_stk)
    Button mButton;
    @BindView(R.id.progressBarStk)
    ProgressBar mLoad;
    String amount, title;
    private DarajaApiClient mApiClient;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stkpush);
        ButterKnife.bind(this);

        Intent intent = getIntent();

        amount = intent.getStringExtra("price");
        title = intent.getStringExtra("title");
        mAmount.setText(amount);

        mProgressDialog = new ProgressDialog(this);
        mApiClient = new DarajaApiClient();
        mApiClient.setIsDebug(true); //Set True to enable logging, false to disable.

        mButton.setOnClickListener(this);

        getAccessToken();

    }

    public void getAccessToken() {
        mApiClient.setGetAccessToken(true);
        mApiClient.mpesaService().getAccessToken().enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(retrofit2.Call<AccessToken> call, Response<AccessToken> response) {
                if (response.isSuccessful()) {
                    mApiClient.setAuthToken(response.body().accessToken);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<AccessToken> call, Throwable t) {

            }

        });
    }

    @Override
    public void onClick(View v) {
        if (v == mButton) {
            String phone_number = mNumber.getText().toString();
            //String amount = mAmount.getText().toString();

            performSTKPush(phone_number,amount);
        }
    }

    public void performSTKPush(String phone_number,String amount) {
        mProgressDialog.setMessage("Processing your request");
        mProgressDialog.setTitle("Please Wait...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.show();
        String timestamp = Utils.getTimestamp();
        STKPush stkPush = new STKPush(
                BUSINESS_SHORT_CODE,
                Utils.getPassword(BUSINESS_SHORT_CODE, PASSKEY, timestamp),
                timestamp,
                TRANSACTION_TYPE,
                String.valueOf(amount),
                Utils.sanitizePhoneNumber(phone_number),
                PARTYB,
                Utils.sanitizePhoneNumber(phone_number),
                CALLBACKURL,
                title, //Account reference
                "Testing"  //Transaction description
        );

        mApiClient.setGetAccessToken(false);

        //Sending the data to the Mpesa API, remember to remove the logging when in production.
        mApiClient.mpesaService().sendPush(stkPush).enqueue(new Callback<STKPush>() {
            @Override
            public void onResponse(retrofit2.Call<STKPush> call, Response<STKPush> response) {
                mProgressDialog.dismiss();
                try {
                    if (response.isSuccessful()) {
                        Timber.d("post submitted to API. %s", response.body());
                    } else {
                        Timber.e("Response %s", response.errorBody().string());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<STKPush> call, Throwable t) {
                mProgressDialog.dismiss();
                Timber.e(t);
            }

        });
    }
}