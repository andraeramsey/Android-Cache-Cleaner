package it391.fileclean;

import android.content.Context;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.text.BidiFormatter;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import java.util.ArrayList;
import java.util.List;
import android.text.format.Formatter;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;

/**
 * Main class with majority of underlying logic and interface method implementations.
 * Extends Fragment, which is a piece of an application's user interface or behavior
 * that can be placed in an activity. Implementations are defined for Background Service OnActionListener
 * methods. Modifying these methods alters behavior for carrying out scanning/deleting background
 * operations.
 */
public class MainFragment extends Fragment implements BackgroundService.OnActionListener {
    //all object and variable declarations
    private BackgroundService myBackgroundService;
    private SharedPreferences mySharedPreferences;
    private ColorBar myColorBar;
    private TextView mySystemSizeText;
    private TextView myCacheSizeText;
    private TextView myFreeSizeText;
    private SearchView mySearchView;
    private ProgressDialog myProgressDialog;
    private ListAdapter myListAdapter;
    private TextView myEmptyView;
    private View myHeaderView;
    private View myProgressBar;
    private TextView myProgressBarText;
    private boolean alreadyScanned = false;
    private boolean alreadyCleaned = false;
    private String mySortByKey;
    private String myCleanOnAppStartupKey;
    private String myExitAfterCleanKey;
    //Interface for monitoring the state of an application service.
    private ServiceConnection myServiceConnection = new ServiceConnection() {
        /**
         * Called when a connection to the Service has been established, with
         * the IBinder of the communication channel to the Service. Updates
         * storage usage on connect and attempts scan.
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBackgroundService = ((BackgroundService.ServiceBinder) service).getService();
            myBackgroundService.setOnActionListener(MainFragment.this);

            updateStorageUsage();

            if (!myBackgroundService.isScanning() && !alreadyScanned) {
                myBackgroundService.scanCache();
            }
        }

        /**
         * Called when a connection to the Service has been lost. This typically
         * happens when the process hosting the service has crashed or been killed.
         * This does not remove the ServiceConnection itself -- this binding to the
         * service will remain active, and you will receive a call to onServiceConnected(ComponentName, IBinder)
         * when the Service is next running.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            myBackgroundService.setOnActionListener(null);
            myBackgroundService = null;
        }
    };
    //begin overriding activity lifecycle methods

    /**
     * Called to do initial creation of the fragment. If the fragment is being
     * recreated from a previous saved state, savedInstanceState is used, but
     * it can be null if there's no previous saved instance. Can be modified
     * to retrieve additional preferences saved in app settings and alter dialog
     * behavior.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(true);

        mySortByKey = getString(R.string.sort_by_key);
        myCleanOnAppStartupKey = getString(R.string.clean_on_app_startup_key);
        myExitAfterCleanKey = getString(R.string.exit_after_clean_key);
        mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        myListAdapter = new ListAdapter(getActivity());
        myProgressDialog = new ProgressDialog(getActivity());
        myProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        myProgressDialog.setCanceledOnTouchOutside(false);
        myProgressDialog.setTitle(R.string.cleaning_cache);
        myProgressDialog.setMessage(getString(R.string.cleaning_in_progress));
        myProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                return true;
            }
        });

        /**
         * This connects to an application service, creating it if needed.
         * this will create the service, but its onStartCommand(Intent, int, int)
         * method will still only be called from a call to startService(Intent).
         * This still provides you with access to the service object while the
         * service is created.
         */

        getActivity().getApplication().bindService(new Intent(getActivity(), BackgroundService.class),
                myServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * This method is called to have the fragment instantiate its user interface view.
     * This will be called between onCreate and onActivityCreated. If you return a View
     * object from here, it will call DestroyView() when the view is being released. Modify
     * this method to alter setup of storage header and listView layout.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.main_fragment, container, false);

        myEmptyView = (TextView) rootView.findViewById(android.R.id.empty);

        ListView listView = (ListView) rootView.findViewById(android.R.id.list);

        View headerLayout = inflater.inflate(R.layout.storage_layout, listView, false);
        myHeaderView = headerLayout.findViewById(R.id.apps_list_header);

        listView.setEmptyView(myEmptyView);
        listView.addHeaderView(headerLayout, null, false);
        listView.setAdapter(myListAdapter);
        listView.setOnItemClickListener(myListAdapter);

        myColorBar = (ColorBar) rootView.findViewById(R.id.color_bar);
        myColorBar.setColors(getResources().getColor(R.color.apps_list_system_memory),
                getResources().getColor(R.color.apps_list_cache_memory),
                getResources().getColor(R.color.apps_list_free_memory));
        mySystemSizeText = (TextView) rootView.findViewById(R.id.systemSize);
        myCacheSizeText = (TextView) rootView.findViewById(R.id.cacheSize);
        myFreeSizeText = (TextView) rootView.findViewById(R.id.freeSize);
        myProgressBar = rootView.findViewById(R.id.progressBar);
        myProgressBarText = (TextView) rootView.findViewById(R.id.progressBarText);

        return rootView;
    }

    /**
     * Initialize the contents of the activity's options menu. You should
     * place your menu items in to menu variable. For this method to be called
     * you must first have called setHasOptionsMenu. Modify this method to
     * alter settings menu. Modify SearchView widget and result sorting for
     * list adapter here.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_bar, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        mySearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mySearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mySearchView.clearFocus();

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                myListAdapter.sortResults(getSortBy(), newText);

                return true;
            }
        });

        MenuItemCompat.setOnActionExpandListener(searchItem,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        myHeaderView.setVisibility(View.GONE);

                        myEmptyView.setText(R.string.no_such_app);

                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        myHeaderView.setVisibility(View.VISIBLE);

                        myListAdapter.resetFilter();

                        myEmptyView.setText(R.string.cache_deleted);

                        return true;
                    }
                });

        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * This method is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal processing
     * happen, such as calling the item's runnable or sending a message to its handler.
     * You can use this method for other items you would like to do processing on.
     * Modify to alter behavior of action bar options.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                if (myBackgroundService != null && !myBackgroundService.isScanning() &&
                        !myBackgroundService.isCleaning() && myBackgroundService.getCacheSize() > 0) {
                    alreadyCleaned = false;

                    myBackgroundService.cleanCache();
                }
                return true;

            case R.id.action_rescan:
                if (myBackgroundService != null && !myBackgroundService.isScanning() &&
                        !myBackgroundService.isCleaning()) {
                    myBackgroundService.scanCache();
                }
                return true;

            case R.id.action_sort_by_app_name:
                setSortBy(ListAdapter.SortBy.APP_NAME);
                return true;

            case R.id.action_sort_by_cache_size:
                setSortBy(ListAdapter.SortBy.CACHE_SIZE);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is usually tied to onResume of the containing activity's lifecycle.
     */
    @Override
    public void onResume() {
        updateStorageUsage();

        if (myBackgroundService != null) {
            if (myBackgroundService.isScanning() && !isProgressBarVisible()) {
                showProgressBar(true);
            } else if (!myBackgroundService.isScanning() && isProgressBarVisible()) {
                showProgressBar(false);
            }

            if (myBackgroundService.isCleaning() && !myProgressDialog.isShowing()) {
                myProgressDialog.show();
            }
        }

        super.onResume();
    }

    /**
     * Called when the Fragment is no longer resumed. This is
     * usually tied to onPause of the containing activity's lifecycle.
     */
    @Override
    public void onPause() {
        if (myProgressDialog.isShowing()) {
            myProgressDialog.dismiss();
        }

        super.onPause();
    }

    /**
     * Called when the fragment is no longer in use. This is
     * called after onStop() and before onDetach() activity
     * lifecycle methods.
     */
    @Override
    public void onDestroy() {
        getActivity().getApplication().unbindService(myServiceConnection);

        super.onDestroy();
    }

    /**
     * Called to update current device storage information and display results.
     * Modify this method to alter behavior of retrieving and displaying storage
     * information on device.
     */
    private void updateStorageUsage() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());

        long totalMemory = (long) stat.getBlockCount() * (long) stat.getBlockSize();
        long medMemory = myBackgroundService != null ? myBackgroundService.getCacheSize() : 0;
        long lowMemory = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        long highMemory = totalMemory - medMemory - lowMemory;

        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        String sizeStr = bidiFormatter.unicodeWrap(
                Formatter.formatShortFileSize(getActivity(), lowMemory));
        myFreeSizeText.setText(getString(R.string.apps_list_header_memory, sizeStr));
        sizeStr = bidiFormatter.unicodeWrap(
                Formatter.formatShortFileSize(getActivity(), medMemory));
        myCacheSizeText.setText(getString(R.string.apps_list_header_memory, sizeStr));
        sizeStr = bidiFormatter.unicodeWrap(
                Formatter.formatShortFileSize(getActivity(), highMemory));
        mySystemSizeText.setText(getString(R.string.apps_list_header_memory, sizeStr));
        myColorBar.setRatios((float) highMemory / (float) totalMemory,
                (float) medMemory / (float) totalMemory,
                (float) lowMemory / (float) totalMemory);
    }
    //getter method for sort preference
    private ListAdapter.SortBy getSortBy() {
        try {
            return ListAdapter.SortBy.valueOf(mySharedPreferences.getString(mySortByKey,
                    ListAdapter.SortBy.CACHE_SIZE.toString()));
        } catch (ClassCastException e) {
            return ListAdapter.SortBy.CACHE_SIZE;
        }
    }
    //setter method for sort preference
    private void setSortBy(ListAdapter.SortBy sortBy) {
        mySharedPreferences.edit().putString(mySortByKey, sortBy.toString()).apply();

        if (myBackgroundService != null && !myBackgroundService.isScanning() &&
                !myBackgroundService.isCleaning()) {
            myListAdapter.sortResults(sortBy, mySearchView.getQuery().toString());
        }
    }

    //Getter method for progress bar visibility

    private boolean isProgressBarVisible() {
        return myProgressBar.getVisibility() == View.VISIBLE;
    }
    //display progress bar
    private void showProgressBar(boolean show) {
        if (show) {
            myProgressBar.setVisibility(View.VISIBLE);
        } else {
            myProgressBar.startAnimation(AnimationUtils.loadAnimation(
                    getActivity(), android.R.anim.fade_out));
            myProgressBar.setVisibility(View.GONE);
        }
    }
//begin overriding OnActionListener interface methods

    /**
     * These methods control behavior throughout process of starting a scan,
     * showing progress, completing a scan, starting a delete operation, and
     * completing a delete operation. Modify these methods to alter the main
     * functionalities of the app throughout user interaction.
     */
    @Override
    public void scanStart(Context context) { //starting scan initialize progress bar
        if (isAdded()) {
            if (myProgressDialog.isShowing()) {
                myProgressDialog.dismiss();
            }

            myProgressBarText.setText(R.string.scanning);
            showProgressBar(true);
        }
    }
    //progress bar text updating
    @Override
    public void scanProgressUpdate(Context context, int current, int max) { //updating progress
        if (isAdded()) {
            myProgressBarText.setText(getString(R.string.scanning_m_of_n, current, max));
        }
    }

    //updating list and storage usage after scan completion
    @Override
    public void scanComplete(Context context, List<ListApps> apps) { //scan complete
        String filter = "";

        if (mySearchView != null && mySearchView.isShown()) {
            filter = mySearchView.getQuery().toString();
        }

        myListAdapter.setItems(apps, getSortBy(), filter);

        if (isAdded()) {
            updateStorageUsage();

            showProgressBar(false);
        }

        if (!alreadyScanned) {
            alreadyScanned = true;

            if (myBackgroundService != null && mySharedPreferences.getBoolean(
                    myCleanOnAppStartupKey, false)) {
                alreadyCleaned = true;

                myBackgroundService.cleanCache();
            }
        }
    }

    @Override
    public void cleanStart(Context context) { //starting delete
        if (isAdded()) {
            if (isProgressBarVisible()) {
                showProgressBar(false);
            }

        }
    }
    //updating list and storage usage after delete operation
    @Override
    public void cleanComplete(Context context, long cacheSize) { //delete complete
        String filter = "";

        if (mySearchView != null && mySearchView.isShown()) {
            filter = mySearchView.getQuery().toString();
        }

        myListAdapter.setItems(new ArrayList<ListApps>(), getSortBy(), filter);

        if (isAdded()) {
            updateStorageUsage();

            if (myProgressDialog.isShowing()) {
                myProgressDialog.dismiss();
            }
        }
        //toast message momentarily displayed near bottom of screen
        Toast.makeText(context, context.getString(R.string.cleaned, Formatter.formatShortFileSize(
                getActivity(), cacheSize)), Toast.LENGTH_LONG).show();

        if (getActivity() != null && !alreadyCleaned &&
                mySharedPreferences.getBoolean(myExitAfterCleanKey, false)) {
            getActivity().finish();
        }
    }
}
