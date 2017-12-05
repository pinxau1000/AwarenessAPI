package pt.ipleiria.awarenessapi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.FenceClient;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.awareness.fence.HeadphoneFence;
import com.google.android.gms.awareness.fence.LocationFence;
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse;
import com.google.android.gms.awareness.snapshot.HeadphoneStateResponse;
import com.google.android.gms.awareness.snapshot.LocationResponse;
import com.google.android.gms.awareness.snapshot.PlacesResponse;
import com.google.android.gms.awareness.snapshot.WeatherResponse;
import com.google.android.gms.awareness.state.HeadphoneState;
import com.google.android.gms.awareness.state.Weather;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.List;

import static android.content.ContentValues.TAG;

//TODO: DON'T FORGET TO CHANGE THE GOOGLE MAPS API KEYS!! Check the TODO list and Manifest!
//TODO: INSTALL A GPS EMULATOR TO EASILY TEST THIS APP.

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private static final String FENCE_RECEIVER_ACTION = "FENCE_RECEIVER_ACTION";
    private static final int REQUEST_MAP_ACTIVITY = 100;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION_SNAPSHOTS = 1000;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION_SET_FENCES = 1001;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION_MAPS_ACTIVITY = 1002;

    private static final String[] FENCE_KEYS = {"headphoneFenceKey", "onFootWithHeadphonesFenceKey"
            , "locationFenceKey"};

    private GoogleApiClient mGoogleApiClient;
    private MyFenceReceiver myFenceReceiver;

    //To handle the state change
    private PendingIntent myPendingIntent;
    private LatLng latLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Register API
        this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        this.mGoogleApiClient.connect();

        Button btn_snapShot = findViewById(R.id.btn_getSnapShot);
        btn_snapShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //App doesn't have permission
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION_SNAPSHOTS);
                    return;
                }

                //Permission already granted
                MainActivity.this.snapshots();
            }
        });


        Intent intent = new Intent(FENCE_RECEIVER_ACTION);
        myPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        myFenceReceiver = new MyFenceReceiver(this);
        registerReceiver(myFenceReceiver, new IntentFilter(FENCE_RECEIVER_ACTION));

        Button btn_fence = findViewById(R.id.btn_fence);

        btn_fence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //App doesn't have permission
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION_SET_FENCES);
                    return;
                }

                //Permission already granted
                MainActivity.this.setFences();
            }
        });

    }

    private void snapshots(){

        TextView tv_snapShot = findViewById(R.id.textView_snapShots);
        tv_snapShot.setText("");
        getHeadphoneState(tv_snapShot);
        getWeather(tv_snapShot);
        getLocation(tv_snapShot);
        getCurrentActivity(tv_snapShot);
        getNearbyPlaces(tv_snapShot);
    }

    private void setFences() {
        AwarenessFence headphoneFence = HeadphoneFence.during(HeadphoneState.PLUGGED_IN);
        registerFence(FENCE_KEYS[0], headphoneFence);

        AwarenessFence onFootFence = DetectedActivityFence.during(DetectedActivityFence.ON_FOOT);
        AwarenessFence onFootWithHeadphonesFence = AwarenessFence.and(onFootFence, headphoneFence);
        registerFence(FENCE_KEYS[1], onFootWithHeadphonesFence);

        @SuppressLint("MissingPermission")
        AwarenessFence locationFence = LocationFence.in(MainActivity.this.latLng.latitude, MainActivity.this.latLng.longitude, 0.000001, 5);
        registerFence(FENCE_KEYS[2], locationFence);
    }

    //TODO: Method 1 of handling the task result with onComplete Listener
    public void getHeadphoneState(final TextView v) {
        Awareness.getSnapshotClient(this).getHeadphoneState().addOnCompleteListener(new OnCompleteListener<HeadphoneStateResponse>() {
            @Override
            public void onComplete(@NonNull Task<HeadphoneStateResponse> task) {
                if (task.isSuccessful()) {
                    HeadphoneStateResponse result = task.getResult();
                    int state = result.getHeadphoneState().getState();

                    if (state == HeadphoneState.PLUGGED_IN) {
                        v.append("\n___\nHeadphones plugged in.");
                    } else if (state == HeadphoneState.UNPLUGGED) {
                        v.append("\n___\nHeadphones unplugged.");
                    }
                } else {
                    task.getException().printStackTrace();
                    v.append("\n___\nUnable to Get Headphones!");
                }
            }
        });
    }

    //TODO: Method 2 of handling the task result with onSucessListener and onFailureListener
    public void getHeadphoneStateV2(final TextView v) {
        Awareness.getSnapshotClient(this).getHeadphoneState().addOnSuccessListener(new OnSuccessListener<HeadphoneStateResponse>() {
            @Override
            public void onSuccess(HeadphoneStateResponse headphoneStateResponse) {
                int state = headphoneStateResponse.getHeadphoneState().getState();

                if (state == HeadphoneState.PLUGGED_IN) {
                    v.append("\n___\nHeadphones plugged in.");
                } else if (state == HeadphoneState.UNPLUGGED) {
                    v.append("\n___\nHeadphones unplugged.");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
                v.append("\n___\nUnable to Get Headphones!");
            }
        });
    }

    //At this point the app should already checked the permissions
    @SuppressLint("MissingPermission")
    private void getWeather(final TextView tv_snapShot) {
        Awareness.getSnapshotClient(this).getWeather().addOnCompleteListener(new OnCompleteListener<WeatherResponse>() {
            @Override
            public void onComplete(@NonNull Task<WeatherResponse> task) {
                if (task.isSuccessful()) {
                    WeatherResponse result = task.getResult();
                    Weather weather = result.getWeather();
                    tv_snapShot.append("\n___\nWeather: " + weather);
                } else {
                    task.getException().printStackTrace();
                    tv_snapShot.append("\n___\nUnable to Get Weather!");
                }
            }
        });
    }

    //At this point the app should already checked the permissions
    @SuppressLint("MissingPermission")
    private void getLocation(final TextView tv_snapShot) {
        Awareness.getSnapshotClient(this).getLocation().addOnCompleteListener(new OnCompleteListener<LocationResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationResponse> task) {
                if (task.isSuccessful()) {
                    LocationResponse result = task.getResult();
                    Location location = result.getLocation();
                    tv_snapShot.append("\n___\nLocation: " + "Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                    MainActivity.this.latLng = new LatLng(location.getLatitude(), location.getLongitude());
                } else {
                    task.getException().printStackTrace();
                    tv_snapShot.append("\n___\nUnable to Get Location!");
                }
            }
        });
    }

    private void getCurrentActivity(final TextView tv_snapShot) {
        Awareness.getSnapshotClient(this).getDetectedActivity().addOnCompleteListener(new OnCompleteListener<DetectedActivityResponse>() {
            @Override
            public void onComplete(@NonNull Task<DetectedActivityResponse> task) {
                if (task.isSuccessful()) {
                    DetectedActivityResponse result = task.getResult();
                    ActivityRecognitionResult arr = result.getActivityRecognitionResult();
                    tv_snapShot.append("\n___\nCurrent Activity: " + arr.getMostProbableActivity().toString());
                } else {
                    task.getException().printStackTrace();
                    tv_snapShot.append("\n___\nUnable to Get Activities!");
                }
            }
        });
    }

    //At this point the app should already checked the permissions
    @SuppressLint("MissingPermission")
    private void getNearbyPlaces(final TextView tv_snapShot) {
        Awareness.getSnapshotClient(this).getPlaces().addOnCompleteListener(new OnCompleteListener<PlacesResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlacesResponse> task) {
                if (task.isSuccessful()) {
                    PlacesResponse result = task.getResult();
                    List<PlaceLikelihood> placeLikelihoodsList = result.getPlaceLikelihoods();

                    if(placeLikelihoodsList!=null){
                        tv_snapShot.append("\n___\nNearbyPlaces: ");
                        for (int i = 0; i < placeLikelihoodsList.size(); i++) {
                            PlaceLikelihood p = placeLikelihoodsList.get(i);
                            tv_snapShot.append("\n\t•" + (i+1) + ": " + p.getPlace().getName().toString()
                                    + "\n\tlikelihood: " + p.getLikelihood()
                                    + "\n\taddress: " + p.getPlace().getAddress()
                                    + "\n\tlocation: " + p.getPlace().getLatLng()
                                    + "\n\twebsite: " + p.getPlace().getWebsiteUri()
                                    + "\n\tplaceTypes: " + p.getPlace().getPlaceTypes());
                        }
                        getPlacePhoto(placeLikelihoodsList.get(0).getPlace().getId());
                    }else{
                        tv_snapShot.append("\n___\nYou have no \"famous\" places nearby. Try another location.");
                    }
                }else{
                    task.getException().printStackTrace();
                    tv_snapShot.append("\n___\nUnable to Get Places!");
                }
            }
        });
    }

    //At this point the app should already checked the permissions
    @SuppressLint("StaticFieldLeak")
    private void getPlacePhoto(String placeId) {
        final ImageView mImageView = findViewById(R.id.imageView);

        // Create a new AsyncTask that displays the bitmap and attribution once loaded.
        //THE RIGHT WAY -> new PhotoTask(mImageView.getWidth(), mImageView.getHeight()) {
        new PhotoTask(mImageView.getWidth(), 1000, mGoogleApiClient) {

            @Override
            protected void onPreExecute() {
                // Display a temporary image to show while bitmap is loading.
                mImageView.setImageResource(R.color.colorPrimaryDark);
            }

            @Override
            protected void onPostExecute(AttributedPhoto attributedPhoto) {
                if (attributedPhoto != null) {
                    // Photo has been loaded, display it.
                    mImageView.setImageBitmap(attributedPhoto.bitmap);
                    TextView mText = findViewById(R.id.textView_snapShots);

                    // Display the attribution as HTML content if set.
                    if (attributedPhoto.attribution == null) {
                        mText.append("\n\t-> Photo Author: Unidentified");
                    } else {
                        mText.append("\n\t -> Photo Author: "+Html.fromHtml(attributedPhoto.attribution.toString()));
                    }

                }
            }
        }.execute(placeId);
    }

    protected void registerFence(final String fenceKey, final AwarenessFence fence) {
        FenceClient fenceClient = Awareness.getFenceClient(this);

        fenceClient.updateFences(new FenceUpdateRequest.Builder()
                        .addFence(fenceKey, fence, myPendingIntent)
                        .build()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    //task.getResult();
                    Toast.makeText(MainActivity.this, "Fence " + fenceKey
                            + " was successfully registered.", Toast.LENGTH_SHORT).show();
                }else{
                    task.getException().printStackTrace();
                    Toast.makeText(MainActivity.this, "Fence " + fenceKey
                            + " could not be registered: " + task.getResult().toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    protected void unregisterFence(final String fenceKey) {
        FenceClient fenceClient = Awareness.getFenceClient(this);

        fenceClient.updateFences(new FenceUpdateRequest.Builder()
                .removeFence(fenceKey)
                .build()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    //task.getResult();
                    Toast.makeText(MainActivity.this, "Fence " + fenceKey
                            + " successfully removed.", Toast.LENGTH_SHORT).show();
                }else{
                    task.getException().printStackTrace();
                    Toast.makeText(MainActivity.this, "Fence " + fenceKey
                            + " could NOT be removed.", Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.btn_map:
                //App doesn't have permission
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION_MAPS_ACTIVITY);
                    return true;
                }

                //Already have permission
                Intent i = new Intent(this, MapsActivity.class);
                //Gives the current location to add a marker on the map
                i.putExtra("currentLatLng", MainActivity.this.latLng);
                startActivityForResult(i, REQUEST_MAP_ACTIVITY);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch(requestCode){
            case REQUEST_MAP_ACTIVITY:
                if(resultCode == RESULT_OK){
                    //Gets the marker coordinates from maps activity
                    this.latLng = data.getParcelableExtra("location");

                    TextView tv_snapShot = findViewById(R.id.textView_snapShots);
                    tv_snapShot.setText("-> FROM MAPS LatLng"+this.latLng.toString());
                }
            break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {

        unregisterFence(FENCE_KEYS[0]);
        unregisterFence(FENCE_KEYS[1]);
        unregisterFence(FENCE_KEYS[2]);

        super.onPause();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();

        //if(myFenceReceiver!=null) unregisterReceiver(myFenceReceiver);

        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("CONNECTION ERROR", "Connection to Google API Client Failed. Error Number: "+connectionResult.getErrorCode());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION_SNAPSHOTS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.snapshots();
                } else {
                    Log.w(TAG, "PERMISSION FINE LOCATION STORAGE NOT GRANTED BY USER");
                    Toast.makeText(this, "App doesn't have location permission!", Toast.LENGTH_SHORT).show();
                }
                break;
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION_SET_FENCES:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MainActivity.this.setFences();
                } else {
                    Log.w(TAG, "PERMISSION FINE LOCATION STORAGE NOT GRANTED BY USER");
                    Toast.makeText(this, "App doesn't have location permission!", Toast.LENGTH_SHORT).show();
                }
                break;
                case MY_PERMISSIONS_REQUEST_FINE_LOCATION_MAPS_ACTIVITY:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent i = new Intent(this, MapsActivity.class);
                    //Gives the current location to add a marker on the map
                    i.putExtra("currentLatLng", MainActivity.this.latLng);
                    startActivityForResult(i, REQUEST_MAP_ACTIVITY);
                } else {
                    Log.w(TAG, "PERMISSION FINE LOCATION STORAGE NOT GRANTED BY USER");
                    Toast.makeText(this, "App doesn't have location permission!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
