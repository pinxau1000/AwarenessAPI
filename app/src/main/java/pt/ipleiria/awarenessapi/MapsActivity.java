package pt.ipleiria.awarenessapi;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng currentLatLng;
    Intent return_int = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Intent i = getIntent();
        this.currentLatLng = i.getParcelableExtra("currentLatLng");
        setResult(Activity.RESULT_CANCELED, return_int);
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in current user location and move the camera
        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Current Position"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));

        mMap.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                MapsActivity.this.mMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title("Your Marker")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                MapsActivity.this.mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                return_int.putExtra("newFencePos", latLng);
                setResult(Activity.RESULT_OK, return_int);
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
