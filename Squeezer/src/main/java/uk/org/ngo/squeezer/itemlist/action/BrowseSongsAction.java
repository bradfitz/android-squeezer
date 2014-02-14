package uk.org.ngo.squeezer.itemlist.action;

import android.util.Log;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;
import uk.org.ngo.squeezer.itemlist.SongListActivity;

public class BrowseSongsAction extends PlayableItemAction {

    public BrowseSongsAction(ItemListActivity activity) {
        super(activity);
    }

    @Override
    public void execute(PlaylistItem item) {
        Log.d(getTag(), "Browsing songs of " + item);
        SongListActivity.show(activity, item);
    }

}
