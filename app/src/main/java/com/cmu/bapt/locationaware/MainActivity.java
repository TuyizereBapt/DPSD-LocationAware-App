package com.cmu.bapt.locationaware;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final int LOCATION_PERMISSION_REQUEST_CODE = 13;
    private final int SMS_PERMISSION_REQUEST_CODE = 12;
    private String SMS_SENT = "SMS_SENT";
    private String SMS_DELIVERED = "SMS_DELIVERED";
    private Location location;
    private LocationManager locationManager;
    private PendingIntent sentPendingIntent;
    private PendingIntent deliveredPendingIntent;
    private TextView txtLatLong;
    private Button btnTextMe;
    private Button btnMapMe;
    private Locale currentLocale;
    private BroadcastReceiver smsReceivedReceiver;
    private BroadcastReceiver smsDeliveredReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLatLong = (TextView) findViewById(R.id.txt_lat_long);
        btnTextMe = (Button) findViewById(R.id.btn_text_me);
        btnMapMe = (Button) findViewById(R.id.btn_map_me);

        currentLocale = getResources().getConfiguration().locale;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        location = this.getCurrentLocation();
        displayLocation();
        registerForLocationUpdates();

        //Registering BroadCast receivers to listen for SMS sent and Delivered events
        sentPendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(SMS_SENT), 0);
        //When the SMS has been sent
        smsReceivedReceiver = new SmsReceivedReceiver();
        registerReceiver(smsReceivedReceiver, new IntentFilter(SMS_SENT));

        deliveredPendingIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(SMS_DELIVERED), 0);
        //When the SMS has been delivered
        smsDeliveredReceiver = new SmsDeliveredReceiver();
        registerReceiver(smsDeliveredReceiver, new IntentFilter(SMS_DELIVERED));


        btnTextMe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (location != null) {
                    sendSMS("0781633004",
                            String.format(
                                    getString(R.string.sms_body), location.getLatitude(), location.getLongitude()));
                } else
                    Toast.makeText(MainActivity.this, "Location is not available.Cannot send SMS message without location.", Toast.LENGTH_LONG).show();
            }
        });
        btnMapMe.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openDialog();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                location = getCurrentLocation();
                displayLocation();
                registerForLocationUpdates();
            } else {
                Toast.makeText(this, getString(R.string.no_permission_warning), Toast.LENGTH_LONG).show();

                finishAffinity();
                System.exit(0);
            }
        }
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length <= 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "No permission to send SMS was given", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (smsDeliveredReceiver != null)
            unregisterReceiver(smsDeliveredReceiver);
        if (smsReceivedReceiver != null)
            unregisterReceiver(smsReceivedReceiver);
    }

    public boolean checkForLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    public boolean checkForSmsPermission() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void registerForLocationUpdates() {
        if (checkForLocationPermission())
            locationManager.requestLocationUpdates("gps", 30000L, 10.0f, new LocationUpdatesListener());
    }

    public Location getCurrentLocation() {
        if (checkForLocationPermission())
            return locationManager.getLastKnownLocation("gps");
        else
            return null;
    }

    public void displayLocation() {
        if (location != null)
            txtLatLong.setText(String.format(
                    currentLocale, "(%.2f,%.2f)", location.getLatitude(), location.getLongitude()
            ));
        else
            txtLatLong.setText(getString(R.string.null_location_message));
    }

    private void openDialog() {
        if (location != null)
            MapDialog.newInstance(location).show(getSupportFragmentManager(), null);
        else
            Toast.makeText(this, "Location not available", Toast.LENGTH_LONG).show();
    }

    void sendSMS(String phoneNumber, String message) {
        if (checkForSmsPermission()) {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, sentPendingIntent, deliveredPendingIntent);
        }
    }

    private class LocationUpdatesListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            location = loc;
            displayLocation();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    public class SmsDeliveredReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SMS_DELIVERED)) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(MainActivity.this, "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(MainActivity.this, "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }

    public class SmsReceivedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SMS_SENT)) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(MainActivity.this, "SMS sent",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(MainActivity.this, "Generic failure",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(MainActivity.this, "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(MainActivity.this, "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(MainActivity.this, "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }
}
