package uk.org.ngo.squeezer.itemlists.actions;

import android.os.RemoteException;
import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.framework.SqueezerPlaylistItem;

public abstract class PlayableItemAction {
	public static enum Type {
		PLAY, QUEUE, INSERT, BROWSE
	}

	protected final SqueezerItemListActivity activity;
	
	public PlayableItemAction(SqueezerItemListActivity activity) {
		super();
		this.activity = activity;
	}

	protected String getTag() {
		return getClass().getSimpleName();
	}
	
	public abstract void execute(SqueezerPlaylistItem item) throws RemoteException;
	
	
	public static PlayableItemAction createAction(SqueezerItemListActivity activity, String actionType) {
		if (actionType ==null || actionType.equals("")) {
			return new PlayAction(activity);
		}
		Type type = Type.valueOf(actionType); 
		switch (type) {
		case BROWSE:
			return new BrowseSongsAction(activity);
		case QUEUE:
			return new QueueAction(activity);
		case INSERT:
			return new InsertAction(activity);
		case PLAY:
		default:
			return new PlayAction(activity);
		}
		
	}
}