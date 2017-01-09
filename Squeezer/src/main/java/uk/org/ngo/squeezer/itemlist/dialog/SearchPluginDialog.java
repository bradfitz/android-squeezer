package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.InputType;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.PluginItemListActivity;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class SearchPluginDialog extends BaseEditTextDialog {

    private PluginItemListActivity activity;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (PluginItemListActivity) getActivity();
        dialog.setTitle(R.string.search_text_hint);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setHint(R.string.search_music_library_hint);

        return dialog;
    }

    @Override
    protected boolean commit(String search_text) {
        ISqueezeService service = activity.getService();
        if (service == null) {
            return false;
        }

        Bundle args = getArguments();
        int start = args.getInt("start");
        Plugin plugin = args.getParcelable("plugin");
        PluginItem parent = args.getParcelable("parent");
        service.pluginItems(start, plugin, parent, search_text, activity);

        return true;
    }

    public static void addTo(PluginItemListActivity activity, int start, Plugin plugin, PluginItem parent) {
        SearchPluginDialog dialog = new SearchPluginDialog();
        Bundle args = new Bundle();
        args.putInt("start", start);
        args.putParcelable("plugin", plugin);
        args.putParcelable("parent", parent);
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), "SearchPluginDialog");
    }
}
