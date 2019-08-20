package com.gmail.katsaros.s.dimitris.e_ktima;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class LoadAreaMap extends FragmentActivity implements OnMapReadyCallback, RoutingListener {

    private static final String TAG = "LoadAreaMap";
    private static final float DEFAULT_ZOOM = 15f;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000 * 5;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation = null;
    private String mLastUpdateTime;
    private Boolean mRequestingLocationUpdates;

    private static final int REQUEST_CHECK_SETTINGS = 100;

    private GoogleMap mMap;

    private Polygon polygon;

    private AreaInfo areaInfo = null;

    private List<Polyline> polylines;
    //    private static final int[] COLORS = new int[]{R.color.primary_dark,R.color.primary,R.color.primary_light,R.color.accent,R.color.primary_dark_material_light};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_area_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(LoadAreaMap.this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String value = extras.getString("area");
            areaInfo = new Gson().fromJson(value, new TypeToken<AreaInfo>() {
            }.getType());
        }

        if (new Permissions(LoadAreaMap.this).isPermissionsOK()) {
            Log.d(TAG, "PERMISSIONS: ALL GOOD");

            startLocationUpdates();
            init();
//            restoreValuesFromBundle(savedInstanceState);
        } else {
            Log.d(TAG, "SOME PERMISSIONS WHERE NOT GRANTED");
        }

        polylines = new ArrayList<>();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

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

        Button deleteAreaBtn = (Button) findViewById(R.id.deleteAreaBtn);
        deleteAreaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: delete button");

                if (areaInfo != null) {
                    confirmDialog();
                } else {
                    Toast.makeText(LoadAreaMap.this, "Δέν υπάρχει έγγυρη περιοχή για διαγραφή", Toast.LENGTH_LONG).show();
                }
            }
        });

        // directions button
        Button directionsBtn = (Button) findViewById(R.id.directionsBtn);
        directionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: directions button");
                LatLng destination = calculateCentroid();
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1&"
                                + "origin=" + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude()
                                + "&destination=" + destination.latitude + "," + destination.longitude));
                startActivity(intent);
//                directions();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);
            mMap.setOnMyLocationButtonClickListener(onMyLocationButtonClickListener);
        }

        drawArea();
    }

    private void init() {
        Log.d(TAG, "inti: has been called");
        mFusedLocationClient = getFusedLocationProviderClient(LoadAreaMap.this);
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

//    private void restoreValuesFromBundle(Bundle savedInstanceState) {
//        if (savedInstanceState != null) {
//            if (savedInstanceState.containsKey("is_requesting_updates")) {
//                mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
//            }
//
//            if (savedInstanceState.containsKey("last_known_location")) {
//                mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
//            }
//
//            if (savedInstanceState.containsKey("last_updated_on")) {
//                mLastUpdateTime = savedInstanceState.getString("last_updated_on");
//            }
//        }
//    }

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
//        outState.putParcelable("last_known_location", mCurrentLocation);
//        outState.putString("last_updated_on", mLastUpdateTime);
//
//    }

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

                        Toast.makeText(getApplicationContext(), "Started location updates!", Toast.LENGTH_SHORT).show();
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
                                    rae.startResolutionForResult(LoadAreaMap.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);

                                Toast.makeText(LoadAreaMap.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new Permissions(LoadAreaMap.this);
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

    private void stopLocationUpdates() {
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onLocationChanged(Location location) {
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Log.d(TAG, "onLocationChanged: " + msg);
        mCurrentLocation = location;
    }

    private void getLastLocation() {
        mFusedLocationClient = getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new Permissions(LoadAreaMap.this);
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

        if (mRequestingLocationUpdates && new Permissions(LoadAreaMap.this).isPermissionsOK()) {
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

    private void drawArea() {
        Log.d(TAG, "drawArea: call");

        if (polygon != null) {
            polygon.remove();
            polygon = null;
            Log.d(TAG, "drawArea: polygon not null");
        }

        if (mMap != null && areaInfo != null) {
            Log.d(TAG, "drawArea: map and polygon exist");
            PolygonOptions polygonOptions = new PolygonOptions().strokeWidth(10).strokeColor(Color.argb(82, 41, 123, 238)).fillColor(Color.argb(82, 238, 41, 44));

            for (int i = 0; i < areaInfo.getMarkersList().size(); i++) {
                polygonOptions.add(areaInfo.getMarkersList().get(i).getLatLng());
            }
            polygon = mMap.addPolygon(polygonOptions);

            moveCamera(calculateCentroid(), DEFAULT_ZOOM);

        } else {
            Log.d(TAG, "drawArea: map or polygon does not exist");
//            Toast.makeText(this, "Ο Χάρτης δέν είναι έτοιμος ακόμη", Toast.LENGTH_LONG).show();
        }
    }

    private LatLng calculateCentroid() {
        double[] centroid = {0.0, 0.0};

        for (int i = 0; i < areaInfo.getMarkersList().size(); i++) {
            centroid[0] += areaInfo.getMarkersList().get(i).getLatLng().latitude;
            centroid[1] += areaInfo.getMarkersList().get(i).getLatLng().longitude;
        }

        int totalPoints = areaInfo.getMarkersList().size();
        centroid[0] = centroid[0] / totalPoints;
        centroid[1] = centroid[1] / totalPoints;

        return new LatLng(centroid[0], centroid[1]);
    }

    private void deleteArea() {
        MyJSON.deleteArea(LoadAreaMap.this, areaInfo.getId());
        areaInfo = null;
        drawArea();
        Toast.makeText(LoadAreaMap.this, "Η περιοχή έχει διαγραφεί με επιτυχία", Toast.LENGTH_LONG).show();
    }

    private void directions() {
        if (mCurrentLocation != null)
            if (areaInfo != null) {
                LatLng start = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                LatLng end = calculateCentroid();

                Routing routing = new Routing.Builder()
                        .travelMode(AbstractRouting.TravelMode.WALKING)
                        .withListener(this)
                        .waypoints(start, end)
                        .key(getResources().getString(R.string.api_key))
                        .build();
                routing.execute();
            } else {
                Toast.makeText(this, "Δέν υπάρχει περιοχή ώστε να υπάρξουν οδηγίες", Toast.LENGTH_LONG).show();
            }
        else
            Toast.makeText(this, "Δέν έχει βρεθεί η τοποθεσία σας ακόμη, παρακαλώ περιμένετε.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
//            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(R.color.routeColor));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    private void erasePolylines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
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
