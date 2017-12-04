package pt.ipleiria.awarenessapi;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.awareness.fence.FenceState;

import static android.content.ContentValues.TAG;

/**
 * Created by Zé on 21/11/2017.
 */

public class MyFenceReceiver extends BroadcastReceiver {
    private AlertDialog alertDialog;
    private static Context context;

    public MyFenceReceiver(Context context) {
        MyFenceReceiver.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        FenceState fenceState = FenceState.extract(intent);

        switch (fenceState.getFenceKey()) {
            case "headphoneFenceKey":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "Headphones are plugged in.");
                        if (alertDialog != null) alertDialog.dismiss();
                        alertDialog = new AlertDialog.Builder(MyFenceReceiver.context)
                                .setMessage("Headphones are plugged in.").show();
                        break;
                    case FenceState.FALSE:
                        Log.i(TAG, "Headphones are NOT plugged in.");
                        if (alertDialog != null) alertDialog.dismiss();
                        alertDialog = new AlertDialog.Builder(MyFenceReceiver.context)
                                .setMessage("Headphones are NOT plugged in.").show();
                        break;
                    case FenceState.UNKNOWN:
                        Log.i(TAG, "The headphone fence is in an unknown state.");
                        break;
                }
                break;
            case "onFootWithHeadphonesFenceKey":
                switch (fenceState.getCurrentState()) {
                    case FenceState.TRUE:
                        Log.i(TAG, "Walking With Headphones.");
                        if (alertDialog != null) alertDialog.dismiss();
                        alertDialog = new AlertDialog.Builder(MyFenceReceiver.context).setMessage("Walking With Headphones.").show();
                        break;
                    case FenceState.FALSE:
                        Log.i(TAG, "NOT Walking With Headphones.");
                        if (alertDialog != null) alertDialog.dismiss();
                        alertDialog = new AlertDialog.Builder(MyFenceReceiver.context).setMessage("NOT Walking With Headphones.").show();
                        break;
                    case FenceState.UNKNOWN:
                        Log.i(TAG, "The Walking With Headphones fence is in an unknown state.");
                        Toast.makeText(MyFenceReceiver.context, "The headphone fence is in an unknown state.", Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
            case "locationFenceKey":
                if (fenceState.getCurrentState()==FenceState.TRUE){
                    Log.i(TAG, "User inside last known location");
                    if (alertDialog != null) alertDialog.dismiss();
                    alertDialog = new AlertDialog.Builder(MyFenceReceiver.context).setMessage("User inside last known location").show();
                }else if (fenceState.getCurrentState()==FenceState.FALSE){
                    Log.i(TAG, "User NOT inside last known location");
                    if (alertDialog != null) alertDialog.dismiss();
                    alertDialog = new AlertDialog.Builder(MyFenceReceiver.context).setMessage("User NOT inside last known location").show();
                }else if (fenceState.getCurrentState()==FenceState.UNKNOWN){
                    Log.i(TAG, "User in unknown location");
                    if (alertDialog != null) alertDialog.dismiss();
                    alertDialog = new AlertDialog.Builder(MyFenceReceiver.context).setMessage("User in unknown location").show();
                }
                break;
        }
    }
}
