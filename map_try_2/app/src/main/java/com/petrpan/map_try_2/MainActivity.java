package com.petrpan.map_try_2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MapListener {

    //Below line from OsmDroid gitHub
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView mapView = null;

    private IMapController mapController;
    //From gitHub
    private CompassOverlay mCompassOverlay;
    //From gitHub
    private RotationGestureOverlay mRotationGestureOverlay;
    //From gitHub
    private ScaleBarOverlay mScaleBarOverlay;



    //From gitHub
    private MyLocationNewOverlay myLocationOverlay;

    // Items
    ArrayList<OverlayItem> items = new ArrayList<>();

    // Declare mOverlay as a class field
    private ItemizedOverlayWithFocus<OverlayItem> mOverlay;

    // MapEventOverlay to detect single or long presses on the map
    private MapEventsOverlay mapEventsOverlay ;

    // Define longPressHandler as a class field
    private final Handler longPressHandler = new Handler();

    private Runnable logGPSTask; // Declare logGPSTask here

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate called"); // Added logging statement

        //2 lines below are from gitHub of OsmDroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //Set the layout
        setContentView(R.layout.activity_main);

        //Initialize the map
        initializeMapView();

        //Centralize the map to Greece
        setupInitialMapPosition();

        //Permissions
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
        });

        // Compass
        addCompassOverlay(ctx);
        //Rotation Gestures
        addMyRotationGestures(ctx);
        //Map scale
        addMyMapScale(ctx);

        // Marker at the center of Greece
        //markerDetails(items);
        //Log.d("MainActivity", "Marker details set"); // Added logging statement


        // GPS
        addMyLocationOverlay();
        Log.d("MainActivity", "Location overlay added"); // Added logging statement


        // Define the Runnable to log GPS position every 30 seconds
        Runnable logGPSTask = new Runnable() {
            @Override
            public void run() {
                // Log GPS position
                Log.d("MainActivity", "Current GPS position: " + myLocationOverlay.getLastFix().getLatitude() + ", " + myLocationOverlay.getLastFix().getLongitude());
                // Run this task again after 30 seconds
                longPressHandler.postDelayed(this, 30000);
            }
        };

        // Start logging GPS position every 30 seconds
        longPressHandler.postDelayed(logGPSTask, 30000);

    }

    // MapOverlayCreator in order to be able to add markers
    private MapEventsOverlay setMapEventsOverlay(){
        // Create a new MapEventsOverlay and add it to the map view overlays

        return new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // Handle single tap
                return false;
            }

            // Set/Unset a marker at a position
            private void setMarker(GeoPoint point) {
                markerDetails_2(items, point);
                mapView.invalidate(); // Refresh the map view
            }


            @Override
            public boolean longPressHelper(GeoPoint p) {
                // Handle long press
                    // Call setMarker to add a marker at the long press position
                    setMarker(p);
                    // Log the position where the marker is added
                    Log.d("MainActivity", "Marker added at position: " + p.getLatitude() + ", " + p.getLongitude());
                    // your items
                    items.add(new OverlayItem("", "", new GeoPoint(p.getLatitude(), p.getLongitude()))); // Lat/Lon decimal degrees

                return false;
            }

        });
    }

    // Reveal/Hide details of markers
    private void markerDetails_2(ArrayList<OverlayItem> items, GeoPoint p){
        // your items
        items.add(new OverlayItem("new pointer", "pointer to something", p)); // Lat/Lon decimal degrees

        // the overlay
        mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(this, items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        mOverlay.setFocusItemsOnTap(true);
                        Log.d("MainActivity", "Marker tapped: " + item.getTitle()); // Log marker tapped
                        // Set a delay for 5 seconds to remove the focus
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mOverlay.unSetFocusedItem();
                                mapView.invalidate(); // Force the map to refresh
                            }
                        }, 5000);
                        return true;
                    }

                    //Part from chatGPT
                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        // Get the long-pressed marker
                        // Remove the long-pressed marker from the map overlays
                        mOverlay.removeItem(index);
                        mOverlay.removeItem(item);
                        // Remove the long-pressed marker from the marker list
                        mapView.invalidate(); // Refresh the map view

                        // Log the removal of the marker
                        Log.d("MainActivity", "Marker removed : " + item.getTitle());
                        addMyLocationOverlay();

                        return true;
                    }
                });



        mapView.getOverlays().add(mOverlay);
    }


    //From gitHub, for scaling
    private void addMyMapScale(Context ctx){
        final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        mScaleBarOverlay = new ScaleBarOverlay(mapView);
        mScaleBarOverlay.setCentred(true);
        //play around with these values to get the location on screen in the right place for your application
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        mapView.getOverlays().add(this.mScaleBarOverlay);
    }

    //From gitHub, for rotation gestures
    private void addMyRotationGestures(Context ctx){
        mRotationGestureOverlay = new RotationGestureOverlay(ctx, mapView);
        mRotationGestureOverlay.setEnabled(true);
        mapView.setMultiTouchControls(true);
        mapView.getOverlays().add(this.mRotationGestureOverlay);
    }

    //From gitHub, for GPS showing
    private void addMyLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    //From gitHub, for compass showing
    private void addCompassOverlay(Context ctx) {
        this.mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx), mapView);
        this.mCompassOverlay.enableCompass();
        mapView.getOverlays().add(this.mCompassOverlay);
    }

    // Load the map
    private void initializeMapView() {
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(true);


        mapView.getOverlays().add(setMapEventsOverlay());
    }

















    // Below are very basic things //
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Set Greece as the center of the map
    private void setupInitialMapPosition() {
        GeoPoint greeceCenter = new GeoPoint(39.0742, 21.8243);
        mapController = mapView.getController();
        mapController.setCenter(greeceCenter);
        mapController.setZoom(7.5);
    }

    //Scrolling
    @Override
    public boolean onScroll(ScrollEvent event) {
        mapView.invalidate(); // Add this line
        return false;
    }

    //Zooming
    @Override
    public boolean onZoom(ZoomEvent event) {
        mapView.invalidate(); // Add this line
        return false;
    }

    // Resume app
    @Override
    public void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume called"); // Added logging statement
        mapView.onResume(); //needed for compass, my location overlays, v6.0.0 and up
        // GPS
        addMyLocationOverlay();
        // Start logging GPS position every 30 seconds
        longPressHandler.postDelayed(logGPSTask, 30000);
    }

    // Pause app
    @Override
    public void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause called"); // Added logging statement
        mapView.onPause();  //needed for compass, my location overlays, v6.0.0 and up
        // GPS
        addMyLocationOverlay();
        // Stop logging GPS position
        longPressHandler.removeCallbacks(logGPSTask);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}
