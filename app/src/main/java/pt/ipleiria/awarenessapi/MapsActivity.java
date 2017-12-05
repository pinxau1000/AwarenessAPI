package pt.ipleiria.awarenessapi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        final Marker[] mapMarker = new Marker[1];
        final LatLng[] location = new LatLng[1];

        Awareness.getSnapshotClient(MapsActivity.this).getLocation().addOnSuccessListener(new OnSuccessListener<LocationResponse>() {
            @Override
            public void onSuccess(LocationResponse locationResponse) {
                location[0] = new LatLng(locationResponse.getLocation().getLatitude(), locationResponse.getLocation().getLongitude());



                // Move the camera to current location
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                        //Position, Zoom, Tilt, Bearing
                        new CameraPosition(location[0], 12, 0, 0)));
                mMap.addMarker(new MarkerOptions().position(location[0])
                        .draggable(false)
                        .title("Your current location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        if (mapMarker[0] != null) {
                            mapMarker[0].setPosition(latLng);
                        } else {
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(latLng)
                                    .title("Filter Location")
                                    .draggable(false);

                            mapMarker[0] = mMap.addMarker(markerOptions);
                        }

                        location[0] = latLng;

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("location", location[0]);
                        setResult(RESULT_OK, returnIntent);
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MapsActivity.this, "Unable to get your current location.", Toast.LENGTH_SHORT).show();

                Intent returnIntent = new Intent();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
            }
        });

        FloatingActionButton btn_done = findViewById(R.id.floatingActionButton);
        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
