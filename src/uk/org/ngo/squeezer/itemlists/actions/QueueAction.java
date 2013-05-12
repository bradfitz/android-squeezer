package uk.org.ngo.squeezer.itemlists.actions;

import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;
import android.os.RemoteException;
import android.util.Log;

public class QueueAction extends PlayableItemAction {

	public QueueAction(SqueezerItemListActivity activity) {
		super(activity);
	}

	@Override
	public void execute(SqueezerPlaylistItem item) throws RemoteException {
		Log.d(getTag(), "Queueing song");
		activity.add(item);
	}
}
