package com.abhi245y.driverwmb;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import static com.google.android.gms.common.util.CollectionUtils.mapOf;


public class MainMapActivity extends FragmentActivity implements OnMapReadyCallback{

    public static final String TAG = "MainMapActivity";
    private GoogleMap mMap;
    private AutoCompleteTextView bus_no;
    public Double mLatitude = 0.0, mLongitude = 0.0;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference busref = db.collection("Bus List");
    private ArrayList<String> busList = new ArrayList<>();
    LocationBroadcastReceiver receiver= new LocationBroadcastReceiver();
    float Bearing;


    //===== Setting A request =================================================================
    public ToggleButton mTrack;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    public Marker marker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        bus_no = findViewById(R.id.busNoeditText);
        mTrack = findViewById(R.id.track);


        getBusNumber();

        mTrack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    startService();
                }
                else
                {
                    stopService();
                }
            }
        });
    }



    private void getBusNumber() {

        busref.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    if (documentSnapshot.exists()) {
                        SeachBusModel seachBusModel = documentSnapshot.toObject(SeachBusModel.class);
                        busList.add(seachBusModel.getBus_no());
                    }
                }
                Log.d(TAG, "Bus List:" + busList);
            }
        });

        ArrayAdapter<String> adapterBus = new ArrayAdapter<>(this,
                R.layout.custom_bus_list_item, R.id.text_view_bus_list_item, busList);
        bus_no.setAdapter(adapterBus);
        ConstraintLayout pannel=findViewById(R.id.constraintLayout);
        pannel.setVisibility(View.VISIBLE);
    }



    //================================================= Setting up Map =====================================================================================================================================
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

    }

    Compass.CompassListener cl = new Compass.CompassListener() {

        @Override
        public void onNewAzimuth(float azimuth) {
            Bearing=(360 - (azimuth) + 8);
            Log.d(TAG,"onNewAzimuth: Bearing: "+Bearing);
        }
    };

    void startService(){
        Compass compass1=new Compass(this);
        compass1.start();
        compass1.setListener(cl);
        IntentFilter  filter= new IntentFilter("Location Service");
        registerReceiver(receiver,filter);
        Intent intent=new Intent(MainMapActivity.this,LocationService.class);
        this.startService(intent);
    }

    void stopService(){
        Compass cl=new Compass(this);
        cl.stop();
        unregisterReceiver(receiver);
        Intent intent=new Intent(MainMapActivity.this,LocationService.class);
        this.stopService(intent);
    }




    public class LocationBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("Location Service")){

                double latitude= intent.getDoubleExtra("Latitude",0f);
                double longitude= intent.getDoubleExtra("Longitude",0f);


                Log.d(TAG,"\nLocationBroadcastReceiver:   Latitude: "+latitude);
                Log.d(TAG,"\nLocationBroadcastReceiver:   Longitude: "+longitude);

                if (mMap != null) {
                    LatLng latLng = new LatLng(latitude, longitude);
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.icon(bitmapDescriptorFromVector(getApplicationContext()));
                    markerOptions.flat(true);
                    markerOptions.rotation(Bearing);
                    if (marker != null)
                        marker.setPosition(latLng);
                    else
                        marker = mMap.addMarker(markerOptions);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

                    Log.d(TAG,"\nLocationBroadcastReceiver:   Location: "+latLng);
                    setBuslist(latLng);
                }


            }

        }
    }

    private void setBuslist(LatLng latLng) {

        final GeoPoint location= new GeoPoint(latLng.latitude,latLng.longitude);
        Log.d(TAG,"\nLocationBroadcastReceiver:  setBuslist Location: "+latLng);
        String bus_number = bus_no.getText().toString();
//        db.collection("Bus List").document(bus_number).update("bus_location",location);
        Log.d(TAG,"setBuslist:   bus_number: "+bus_number);
        busref.document(bus_number).update("bus_location",location);
        busref.document(bus_number).update("bearing",Bearing);
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context,R.drawable.ic_bus_marker);
        assert vectorDrawable != null;
        vectorDrawable.setBounds(50, 50, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());

        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getMinimumWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);

    }


}
