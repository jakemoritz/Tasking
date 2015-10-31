package me.jakemoritz.tasking;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * Created by jakemoritz on 10/31/15.
 */
public class AccountDialogPreference extends DialogPreference {

    private Context context;

    public AccountDialogPreference(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        this.context = context;
    }

    public interface OnSignOutListener {
        public void signOut();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        if (which == Dialog.BUTTON_POSITIVE){
            ((LoginActivity) context).signOut();
        }
        else {
            dialog.cancel();
        }
    }

}
