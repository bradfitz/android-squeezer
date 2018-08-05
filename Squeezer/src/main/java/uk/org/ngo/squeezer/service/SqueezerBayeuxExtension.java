package uk.org.ngo.squeezer.service;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import uk.org.ngo.squeezer.Squeezer;

class SqueezerBayeuxExtension extends ClientSession.Extension.Adapter {
    //NOTE mysqueezebox.com doesn't accept dash in the uuid field of the ext field
    private String uuid = UUID.randomUUID().toString().replaceAll("-", "");

    private Map<String, String> ext = initExt();

    private Map<String, String> initExt() {
        PackageManager pm = Squeezer.getContext().getPackageManager();
        String rev;
        try {
            PackageInfo packageInfo = pm.getPackageInfo(Squeezer.getContext().getPackageName(), 0);
            rev = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            rev = "1.0";
        }

        WifiManager manager = (WifiManager) Squeezer.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        String mac = info.getMacAddress();

        Map<String, String> ext = new HashMap<>();
        ext.put("mac", mac);
        ext.put("rev", rev);
        ext.put("uuid", uuid);

        return ext;
    }

    @Override
    public boolean sendMeta(ClientSession session, Message.Mutable message) {
        // mysqueezebox.com requires an ext field in the handshake message
        if (Channel.META_HANDSHAKE.equals(message.getChannel())) {
            message.put(Message.EXT_FIELD, ext);
        }

        return true;
    }
}
