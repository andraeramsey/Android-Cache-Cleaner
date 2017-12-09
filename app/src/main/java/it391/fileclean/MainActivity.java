package it391.fileclean;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * AppCompatActivity is base class for activities that use the support library action bar features.
 * You can add an ActionBar to your activity when running on API level 7 or higher by extending this
 * class for your activity and setting the activity theme to Theme.AppCompat or a similar theme. MainActivity
 * contains MainFragment
 */
public class MainActivity extends AppCompatActivity {
    //Perform initialization of all fragments and loaders
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
    }
}
