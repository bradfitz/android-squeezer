package uk.org.ngo.squeezer.itemlist.action;

import android.os.RemoteException;
import android.util.Log;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;

public class PlayAction extends PlayableItemAction {

    public PlayAction(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public void execute(PlaylistItem item) throws RemoteException {
        Log.d(getTag(), "Playing song");
        activity.play(item);
    }

}
