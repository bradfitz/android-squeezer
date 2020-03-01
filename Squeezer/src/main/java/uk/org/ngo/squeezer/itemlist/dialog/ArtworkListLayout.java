package uk.org.ngo.squeezer.itemlist.dialog;

import android.content.Context;

import androidx.annotation.StringRes;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithTextAndIcon;

/**
 * Supported list layouts.
 */
public enum ArtworkListLayout implements EnumWithTextAndIcon {
    grid(R.attr.ic_action_view_as_grid, R.string.SWITCH_TO_GALLERY),
    list(R.attr.ic_action_view_as_list, R.string.SWITCH_TO_EXTENDED_LIST);

    /**
     * The icon to use for this layout
     */
    private final int iconAttribute;

    @Override
    public int getIconAttribute() {
        return iconAttribute;
    }

    /**
     * The text to use for this layout
     */
    @StringRes
    private final int stringResource;

    @Override
    public String getText(Context context) {
        return context.getString(stringResource);
    }

    ArtworkListLayout(int iconAttribute, @StringRes int serverString) {
        this.stringResource = serverString;
        this.iconAttribute = iconAttribute;
    }
}
