package com.lionel.googlemapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.lionel.googlemapp.databinding.ActivityMainBinding;
import com.lionel.googlemapp.databinding.LayoutMapInfoWindowBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, LocationSource {

    private static final int REQUEST_PERMISSION_LOCATION_FINE = 1000;

    private GoogleMap mapView;
    private Marker marker;
    private LayoutMapInfoWindowBinding layoutMapInfoWindowBinding;
    private OnLocationChangedListener myLocationListener;
    private LocationManager locationManager;
    private boolean isPermissionChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        dataBinding.setMapHandler(this);
        layoutMapInfoWindowBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.layout_map_info_window, null, false);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        initMap();
    }

    private void initMap() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapView = googleMap;
        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            isPermissionChecked = true;
            runMapFunctions();
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION_FINE);
            } else {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("需要權限")
                        .setMessage("本頁面需要開啟定位權限, 否則將無法正常使用")
                        .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION_FINE);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_LOCATION_FINE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isPermissionChecked = true;
            runMapFunctions();
        } else {
            checkPermission();
        }
    }

    private void runMapFunctions() {
        if (mapView != null) {
            initPolyPath();
            initInfoWindow();
            initLocationFun();
        }
    }

    private void initPolyPath() {
        List<LatLng> latLngList = new ArrayList<>();
        for (double[] latLngArray : SomeMapData.TAIWAN_SOME_POINTS) {
            LatLng latLng = new LatLng(latLngArray[0], latLngArray[1]);
            latLngList.add(latLng);
        }

        //add init point to form a cycle
        LatLng latLng = new LatLng(SomeMapData.TAIWAN_SOME_POINTS[0][0], SomeMapData.TAIWAN_SOME_POINTS[0][1]);
        latLngList.add(latLng);

        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(latLngList)
                .color(Color.RED)
                .width(15)
                .jointType(JointType.ROUND);

        if (mapView != null) {
            mapView.addPolyline(polylineOptions);
        }
    }

    private void initInfoWindow() {
        if (mapView != null) {
            mapView.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    MapInfoWindowDataContainer container = (MapInfoWindowDataContainer) marker.getTag();

                    // don't know why cant use binding.setVariable() here...
//                    layoutMapInfoWindowBinding.setInfoData(container);

                    layoutMapInfoWindowBinding.img.setBackgroundResource(container.getSrcDrawable());
                    layoutMapInfoWindowBinding.title.setText(container.getTitle());
                    layoutMapInfoWindowBinding.content.setText(container.getLatLng());
                    return layoutMapInfoWindowBinding.getRoot();
                }

                @Override
                public View getInfoContents(Marker marker) {
                    return null;
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void initLocationFun() {
        mapView.setMyLocationEnabled(true);
        mapView.setLocationSource(this);
    }

    @SuppressLint("MissingPermission")
    private void enableLocationUpdate(boolean isEnabled) {
        if (isEnabled && isPermissionChecked) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, this);
            }

            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            LogUtil.print("GPS_PROVIDER location is null: " + (location == null));
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                LogUtil.print("NETWORK_PROVIDER location is null: " + (location == null));
            }

            if (location != null) {
                LogUtil.print("into locationChanged");
                onLocationChanged(location);
            }
        } else {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        LogUtil.print("onLocationChanged");
        if (myLocationListener != null) {
            myLocationListener.onLocationChanged(location);
            mapView.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LogUtil.print("onStatusChanged");

    }

    @Override
    public void onProviderEnabled(String provider) {
        LogUtil.print("onProviderEnabled");
        enableLocationUpdate(true);
    }

    @Override
    public void onProviderDisabled(String provider) {
        LogUtil.print("onProviderDisabled");
        enableLocationUpdate(false);
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        LogUtil.print("activate");
        myLocationListener = onLocationChangedListener;
        enableLocationUpdate(true);
    }

    @Override
    public void deactivate() {
        LogUtil.print("deactivate");
        myLocationListener = null;
        enableLocationUpdate(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) {
            enableLocationUpdate(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapView != null) {
            enableLocationUpdate(false);
        }
    }

    /**
     * method for spinner in dataBinding
     */
    public void onPointSelected(AdapterView<?> parent, View view, int pos, long id) {
        updateMapPoint(pos);
    }

    private void updateMapPoint(int pos) {
        if (mapView != null) {
            String pointName = getResources().getStringArray(R.array.spinner_items)[pos];
            int srcDrawable = SomeMapData.MARKS_DRAWABLES[pos];
            double[] arrayLatLng = SomeMapData.TAIWAN_SOME_POINTS[pos];

            LatLng latLng = new LatLng(arrayLatLng[0], arrayLatLng[1]);

            if (pos > 0) {
//                mapView.animateCamera(CameraUpdateFactory.newLatLng(latLng));
                mapView.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(15)
                        .tilt(0)
                        .build()));
            } else {  // only for 1st item
                mapView.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(18)
                        .tilt(67.5f)
                        .build()));
            }

            //remove the old marker
            if (marker != null) {
                marker.remove();
            }
            marker = mapView.addMarker(new MarkerOptions()
                    .title(pointName)
                    .position(latLng)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromResource(srcDrawable))
                    .flat(false)
            );


            MapInfoWindowDataContainer container = new MapInfoWindowDataContainer();
            container.setLatLng(latLng.latitude + " , " + latLng.longitude);
            container.setTitle(pointName);
            container.setSrcDrawable(srcDrawable);
            marker.setTag(container);
        }
    }
}
