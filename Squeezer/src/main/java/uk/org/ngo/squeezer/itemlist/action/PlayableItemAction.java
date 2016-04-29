package uk.org.ngo.squeezer.itemlist.action;

import android.content.Context;
import android.support.annotation.NonNull;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithText;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.framework.PlaylistItem;

public abstract class PlayableItemAction {

    public static final Type[] ALBUM_ACTIONS = new Type[]{
            Type.NONE,
            Type.PLAY,
            Type.INSERT,
            Type.ADD,
            Type.BROWSE
    };
    public static final Type[] SONG_ACTIONS = new Type[]{
            Type.NONE,
            Type.PLAY,
            Type.INSERT,
            Type.ADD
    };

    public enum Type implements EnumWithText {
        /**
         * Ask
         */
        NONE(R.string.NO_ACTION),
        /**
         * PLay item immediately
         */
        PLAY(R.string.PLAY_NOW),
        /**
         * Add the item to the playlist
         */
        ADD(R.string.ADD_TO_END),
        /**
         * Play the item after the current song
         */
        INSERT(R.string.PLAY_NEXT),
        /**
         * Browse contents.
         */
        BROWSE(R.string.BROWSE_SONGS);

        private final int labelId;

        Type(int label) {
            this.labelId = label;
        }

        @Override
        public String getText(Context context) {
            return context.getString(labelId);
        }
    }

    protected final ItemListActivity activity;

    public PlayableItemAction(ItemListActivity activity) {
        super();
        this.activity = activity;
    }

    protected String getTag() {
        return getClass().getSimpleName();
    }

    public abstract void execute(PlaylistItem item);

    @NonNull
    public static PlayableItemAction createAction( ItemListActivity activity, Type type) {
        switch (type) {
            case BROWSE:
                return new BrowseSongsAction(activity);
            case ADD:
                return new AddAction(activity);
            case INSERT:
                return new InsertAction(activity);
            case PLAY:
                return new PlayAction(activity);
            default:
                return new AskAction(activity);
        }
    }
}
