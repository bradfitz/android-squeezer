package uk.org.ngo.squeezer.itemlist.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import uk.org.ngo.squeezer.R;

public abstract class BaseEditTextDialog extends DialogFragment {

    protected TextInputLayout editTextLayout;
    protected EditText editText;

    abstract protected boolean commit(String string);

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        @SuppressLint({"InflateParams"}) // OK, as view is passed to AlertDialog.Builder.setView()
        View form = getActivity().getLayoutInflater().inflate(R.layout.edittext_dialog, null);
        builder.setView(form);
        editTextLayout = form.findViewById(R.id.edittext_til);
        editText = form.findViewById(R.id.edittext);

        editText.setText("");
        editText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode
                        == KeyEvent.KEYCODE_ENTER)) {
                    if (commit(editText.getText().toString())) {
                        dismiss();
                    }
                    return true;
                }
                return false;
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                commit(editText.getText().toString());
            }
        });

        return builder.create();
    }

}
