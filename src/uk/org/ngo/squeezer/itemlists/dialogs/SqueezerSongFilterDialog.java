package uk.org.ngo.squeezer.itemlists.dialogs;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlists.SqueezerSongListActivity;
import uk.org.ngo.squeezer.model.SqueezerGenre;
import uk.org.ngo.squeezer.model.SqueezerYear;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

public class SqueezerSongFilterDialog extends SqueezerBaseFilterDialog {
    private SqueezerSongListActivity activity;
    private Spinner genreSpinnerView;
    private Spinner yearSpinnerView;
    private EditText editText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        activity = (SqueezerSongListActivity) getActivity();
        editText = (EditText) filterForm.findViewById(R.id.search_string);
        editText.setHint(getString(R.string.filter_text_hint, activity.getItemAdapter().getQuantityString(2)));
        editText.setText(activity.getSearchString());

        genreSpinnerView = (Spinner) filterForm.findViewById(R.id.genre_spinner);
        yearSpinnerView = (Spinner) filterForm.findViewById(R.id.year_spinner);
        activity.setGenreSpinner(genreSpinnerView);
        activity.setYearSpinner(yearSpinnerView);

        if (activity.getArtist() != null) {
            ((EditText)filterForm.findViewById(R.id.artist)).setText(activity.getArtist().getName());
            filterForm.findViewById(R.id.artist_view).setVisibility(View.VISIBLE);
        }
        if (activity.getAlbum() != null) {
            ((EditText) filterForm.findViewById(R.id.album)).setText(activity.getAlbum().getName());
            filterForm.findViewById(R.id.album_view).setVisibility(View.VISIBLE);
        }

        return dialog;
}

    @Override
    protected void filter() {
        activity.setSearchString(editText.getText().toString());
        activity.setGenre((SqueezerGenre) genreSpinnerView.getSelectedItem());
        activity.setYear((SqueezerYear) yearSpinnerView.getSelectedItem());
        activity.orderItems();
    }

}
