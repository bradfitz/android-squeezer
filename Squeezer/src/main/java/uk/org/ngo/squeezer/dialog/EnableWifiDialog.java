package uk.org.ngo.squeezer.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import uk.org.ngo.squeezer.R;

public class EnableWifiDialog extends DialogFragment {

    public static final String TAG = EnableWifiDialog.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.wifi_disabled_text);
        builder.setMessage(R.string.enable_wifi_text);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                WifiManager wifiManager = (WifiManager) getActivity()
                        .getApplicationContext().getSystemService(
                        Context.WIFI_SERVICE);
                if (!wifiManager.isWifiEnabled()) {
                    Log.v(getTag(), "Enabling Wifi");
                    wifiManager.setWifiEnabled(true);
                    Toast.makeText(getActivity(), R.string.wifi_enabled_text, Toast.LENGTH_LONG)
                            .show();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        return builder.create();
    }
}
