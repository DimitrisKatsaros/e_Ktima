package com.gmail.katsaros.s.dimitris.e_ktima;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class SaveAreaMap extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final float DEFAULT_ZOOM = 18f;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000 * 5;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private String mLastUpdateTime;

    private static final int REQUEST_CHECK_SETTINGS = 100;

    private Boolean mRequestingLocationUpdates;

    private GoogleMap mMap;
    private Button addMarker;
    private Button doneBtn;
    private Button deleteButton;

    private ArrayList<MarkerInfo> markerList = new ArrayList<>();

    private Polygon polygon;

    private String dialogInput = "";
    private AreaInfo areaInfo = null;
    private String uniqueID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_area_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(SaveAreaMap.this);


        if (new Permissions(SaveAreaMap.this).isPermissionsOK()) {
            Log.d(TAG, "PERMISSIONS: ALL GOOD");

            startLocationUpdates();
            init();
//            restoreValuesFromBundle(savedInstanceState);

        } else {
            Log.d(TAG, "SOME PERMISSIONS WHERE NOT GRANTED");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "onMapReady: MAP IS READY");

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        Button mapBtn = (Button) findViewById(R.id.mapBtn);
        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleView("mapBtn");
            }
        });

        Button satelliteBtn = (Button) findViewById(R.id.satelliteBtn);
        satelliteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleView("satelliteBtn");
            }
        });

        addMarker = (Button) findViewById(R.id.addMarker);
        addMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLastLocation();
                if (mCurrentLocation != null) {
                    addMarker(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                } else {
                    Toast.makeText(getApplicationContext(), "Δεν έχει βρεθεί η τοποθεσίας σας ακόμη, παρακαλώ περιμένετε.", Toast.LENGTH_LONG).show();
                }
            }
        });

        doneBtn = (Button) findViewById(R.id.doneBtn);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (markerList.size() > 2) {
                    titleDialog();
                } else {
                    Toast.makeText(SaveAreaMap.this, "Πρέπει να υπάρχουν τουλάχιστον 3 σημεία για να μπορέσει να σχηματιστεί περιοχή", Toast.LENGTH_SHORT).show();
                }
            }
        });

        deleteButton = (Button) findViewById(R.id.deleteAreaBtn);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: delete button");
                confirmDialog();
            }
        });
        deleteButton.setEnabled(false);

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker arg0) {
                // TODO Auto-generated method stub
                Log.d("System out", "onMarkerDragStart..." + arg0.getPosition().latitude + "..." + arg0.getPosition().longitude + "..." + arg0.getId());
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onMarkerDragEnd(Marker arg0) {
                // TODO Auto-generated method stub
                Log.d("System out", "onMarkerDragEnd..." + arg0.getPosition().latitude + "..." + arg0.getPosition().longitude + "..." + arg0.getId());
                for (int i = 0; i < markerList.size(); i++) {
                    Log.d("System out", "previous details: id = " + markerList.get(i).getIndex() + " latlng = " + markerList.get(i).getLatLng());
                    if (markerList.get(i).getIndex().equals(arg0.getId())) {
                        markerList.get(i).setLatLng(new LatLng(arg0.getPosition().latitude, arg0.getPosition().longitude));
                    }
                    Log.d("System out", "new details: id = " + markerList.get(i).getIndex() + " latlng = " + markerList.get(i).getLatLng());
                }
            }

            @Override
            public void onMarkerDrag(Marker arg0) {
                // TODO Auto-generated method stub
                Log.i("System out", "onMarkerDrag...");
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);
            mMap.setOnMyLocationButtonClickListener(onMyLocationButtonClickListener);
        }
    }

    private void titleDialog() {
        uniqueID = UUID.randomUUID().toString();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Δώστε όνομα για την περιοχή");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("ΠΡΟΣΘΗΚΗ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                MyJSON.clearData(SaveAreaMap.this);
                dialogInput = input.getText().toString().trim();

                if (!dialogInput.isEmpty()) {
                    areaInfo = new AreaInfo(dialogInput, uniqueID, markerList);
                    MyJSON.addArea(SaveAreaMap.this, areaInfo);
                    addMarker.setEnabled(false);
                    doneBtn.setEnabled(false);
                    deleteButton.setEnabled(true);
                    drawArea();
                } else {
                    Toast.makeText(SaveAreaMap.this, "Παρακαλώ δώστε έγκυρο όνομα που να περιέχει χαρακτήρες", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }
            }
        });

        builder.setNegativeButton("ΑΚΥΡΩΣΗ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void init() {
        Log.d(TAG, "inti: has been called");
        mFusedLocationClient = getFusedLocationProviderClient(SaveAreaMap.this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(TAG, "onLocationResult has been called");
                mCurrentLocation = locationResult.getLastLocation();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                Log.d(TAG, "mCurrentLocation " + mCurrentLocation + " at " + mLastUpdateTime);
            }
        };

        mRequestingLocationUpdates = false;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private GoogleMap.OnMyLocationButtonClickListener onMyLocationButtonClickListener = new GoogleMap.OnMyLocationButtonClickListener() {
        @Override
        public boolean onMyLocationButtonClick() {
            getLastLocation();
            if (mCurrentLocation == null) {
                Log.d(TAG, "MapActivity: onMyLocationButtonClick: on mCurrentLocation is null");
                Toast.makeText(getApplicationContext(), "Δεν έχει βρεθεί η τοποθεσίας σας ακόμη, παρακαλώ περιμένετε.", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "MapActivity: onMyLocationButtonClick: mCurrentLocation is " + mCurrentLocation);
            }
            return false;
        }
    };

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), 2000, null);
    }

    private void addMarker(final LatLng latLng) {
        MarkerOptions marker = new MarkerOptions().position(latLng).draggable(true);

        Marker markerObj = mMap.addMarker(marker);

        markerList.add(new MarkerInfo(latLng, markerObj.getId()));
//        Log.d(TAG, "Marker list: index " + markerList.get(0).getIndex() + " Lat: " + markerList.get(0).getLatLng().latitude + " Lng: " + markerList.get(0).getLatLng().longitude);
    }

    private void updateLocationUI() {
        Log.d(TAG, "updateLocationUI call");
        if (mCurrentLocation != null) {
            moveCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), DEFAULT_ZOOM);
        }
    }

    protected void startLocationUpdates() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

        mSettingsClient = LocationServices.getSettingsClient(this);
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

//                        Toast.makeText(getApplicationContext(), "Started location updates!", Toast.LENGTH_SHORT).show();
                        mRequestingLocationUpdates = true;
                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(SaveAreaMap.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(SaveAreaMap.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new Permissions(SaveAreaMap.this);
        } else {
            getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            onLocationChanged(locationResult.getLastLocation());
                        }
                    },
                    Looper.myLooper());
        }
    }

    public void onLocationChanged(Location location) {
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Log.d(TAG, "onLocationChanged: " + msg);
        mCurrentLocation = location;
    }

    public void getLastLocation() {
        mFusedLocationClient = getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new Permissions(SaveAreaMap.this);
        } else {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                onLocationChanged(location);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("MapDemoActivity", "Error trying to get last GPS location");
                            e.printStackTrace();
                        }
                    });
        }

    }

    private void toggleView(String btnName) {
        if (btnName.equals("satelliteBtn"))
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        else
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mRequestingLocationUpdates && new Permissions(SaveAreaMap.this).isPermissionsOK()) {
            startLocationUpdates();
        }

        updateLocationUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
    }

    public void stopLocationUpdates() {
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void drawArea() {
        if (polygon != null) {
            polygon.remove();
            polygon = null;
            mMap.clear();
        }

        if (mMap != null && areaInfo != null) {

            PolygonOptions polygonOptions = new PolygonOptions().strokeWidth(10).strokeColor(Color.argb(82, 41, 123, 238)).fillColor(Color.argb(82, 238, 41, 44));

            for (int i = 0; i < markerList.size(); i++) {
                polygonOptions.add(markerList.get(i).getLatLng());
            }
            polygon = mMap.addPolygon(polygonOptions);

        } else {
            Toast.makeText(this, "Ο Χάρτης δέν είναι έτοιμος ακόμη", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteArea() {
        mMap.clear();
        polygon = null;
        MyJSON.deleteArea(SaveAreaMap.this, areaInfo.getId());
        areaInfo = null;
        markerList = new ArrayList<>();
        addMarker.setEnabled(true);
        doneBtn.setEnabled(true);
        deleteButton.setEnabled(false);
        Toast.makeText(SaveAreaMap.this, "Η περιοχή έχει διαγραφεί με επιτυχία", Toast.LENGTH_LONG).show();
    }

    private void confirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Είστε σίγουροι ότι θέλετε να διαγράψετε την περιοχή με όνομα " + areaInfo.getTitle() + ";");

        // Set up the buttons
        builder.setPositiveButton("ΔΙΑΓΡΑΦΗ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // add area or delete area
                deleteArea();
            }
        });

        builder.setNegativeButton("ΑΚΥΡΩΣΗ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
