package uk.org.ngo.squeezer.itemlist.action;

import android.os.RemoteException;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;

public class InsertAction extends PlayableItemAction {

    public InsertAction(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public void execute(PlaylistItem item) throws RemoteException {
        activity.insert(item);
    }

}
