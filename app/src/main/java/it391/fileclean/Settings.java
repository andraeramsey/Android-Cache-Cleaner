package it391.fileclean;

import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;

/**
 * Settings class handles settings activity. Modify this class to alter
 * toolbar setup in settings and the layout view.
 */
public class Settings extends PreferenceActivity {

    private Toolbar myToolbar;

    /**
     * Called when the activity is starting. This is where most
     * initialization should go
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        myToolbar.setTitle(getTitle());
    }

    /**
     * Set the setting activity content to an explicit view. This view
     * is placed directly into the activity's view hierarchy. Modify settings layout
     * in this method.
     */
    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.settings_activity, (ViewGroup) getWindow().getDecorView().getParent(), false);

        myToolbar = (Toolbar) contentView.findViewById(R.id.toolbar);
        myToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);
        getWindow().setContentView(contentView);
    }
}
