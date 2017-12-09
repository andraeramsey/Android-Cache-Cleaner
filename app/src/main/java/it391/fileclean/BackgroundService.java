package it391.fileclean;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StatFs;
import android.text.format.Formatter;
import android.widget.Toast;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;

/**
 * BackgroundService class is used to perform scanning and cleaning operations in the background
 * and then publish results on the UI thread. Modify this class to change background scanning and deleting
 * behavior.
 */
public class BackgroundService extends Service {
    //variable declarations and initialization
    public static final String CLEAN_AND_EXIT = "CLEAN_AND_EXIT";
    private OnActionListener onActionListener;
    private Method getPackageSizeInfo;
    private Method freeStorageAndNotify;
    private long cacheSize = 0;
    private boolean scanning = false;
    private boolean cleaning = false;
    //This interface declares the methods used for major scanning and deleting operations
    //modify the implementations in MainFragment.java to alter scanning/deleting progression behavior
    public interface OnActionListener {
         void scanStart(Context context);

         void scanProgressUpdate(Context context, int current, int max);

         void scanComplete(Context context, List<ListApps> apps);

         void cleanStart(Context context);

         void cleanComplete(Context context, long cacheSize);
    }
    //Binder class is used for inter-process communication RPC
    public class ServiceBinder extends Binder {

        public BackgroundService getService() {
            return BackgroundService.this;
        }
    }

    private ServiceBinder binder = new ServiceBinder();

    /**
     * This class handles background scanning behavior and publishing results to UI thread.
     * Modify threading behavior in doInBackground method
     */
    private class ScanTask extends AsyncTask<Void, Integer, List<ListApps>> {

        private int appCount = 0;
    //invoked on the UI thread before the task is executed. Sets up actionlistener
        @Override
        protected void onPreExecute() {
            if (onActionListener != null) {
                onActionListener.scanStart(BackgroundService.this);
            }
        }

        /**
         *  invoked on the background thread immediately after onPreExecute() finishes executing.
         *  This step is used to perform background computation that can take a long time.
         *  The parameters of AsyncTask are passed to this method. Modify to alter background thread
         *  scanning behavior
         *
         */
        @Override
        protected List<ListApps> doInBackground(Void... params) {
            cacheSize = 0;

            final List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(
                    PackageManager.GET_META_DATA);

            publishProgress(0, packages.size());

            final CountDownLatch countDownLatch = new CountDownLatch(packages.size());

            final List<ListApps> apps = new ArrayList<ListApps>();

            try {
                for (ApplicationInfo pkg : packages) {  //loop through packages and make RPCs using Android's AIDL
                    getPackageSizeInfo.invoke(getPackageManager(), pkg.packageName,
                            new IPackageStatsObserver.Stub() {

                                @Override
                                public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
                                        throws RemoteException {
                                    synchronized (apps) {   //synchronize so thread finishes operation before another thread attempts to enter
                                        publishProgress(++appCount, packages.size());

                                        if (succeeded && pStats.cacheSize > 0) {
                                            try {
                                                apps.add(new ListApps(pStats.packageName,
                                                        getPackageManager().getApplicationLabel(
                                                                getPackageManager().getApplicationInfo(
                                                                        pStats.packageName,
                                                                        PackageManager.GET_META_DATA)
                                                        ).toString(),
                                                        getPackageManager().getApplicationIcon(
                                                                pStats.packageName),
                                                        pStats.cacheSize
                                                ));

                                                cacheSize += pStats.cacheSize;
                                            } catch (PackageManager.NameNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    synchronized (countDownLatch) {
                                        countDownLatch.countDown();
                                    }
                                }
                            }
                    );
                }
                countDownLatch.await();

            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            return new ArrayList<ListApps>(apps);
        }
    //This method is used to display any form of progress in the user interface while the background computation is still executing
        @Override
        protected void onProgressUpdate(Integer... values) {
            if (onActionListener != null) {
                onActionListener.scanProgressUpdate(BackgroundService.this, values[0], values[1]);
            }
        }
    //invoked on the UI thread after the background computation finishes. The result of the background computation is passed to this step as a parameter.
        @Override
        protected void onPostExecute(List<ListApps> result) {
            if (onActionListener != null) {
                onActionListener.scanComplete(BackgroundService.this, result);
            }
            scanning = false;
        }
    }

    /**
     * This class handles background deleting behavior and publishing results to UI thread.
     * Modify threading behavior in doInBackground method
     */
    private class CleanTask extends AsyncTask<Void, Void, Long> {
        //invoked on the UI thread before the task is executed. Sets up actionlistener

        @Override
        protected void onPreExecute() {
            if (onActionListener != null) {
                onActionListener.cleanStart(BackgroundService.this);
            }
        }
        /**
         *  invoked on the background thread immediately after onPreExecute() finishes executing.
         *  This step is used to perform background computation that can take a long time.
         *  The parameters of AsyncTask are passed to this method. Modify to alter background thread
         *  scanning behavior
         *
         */
        @Override
        protected Long doInBackground(Void... params) {
            final CountDownLatch countDownLatch = new CountDownLatch(1);

            StatFs stat = new StatFs(Environment.getDataDirectory().getAbsolutePath());
            //RPCs with Android's AIDL
            try {
                freeStorageAndNotify.invoke(getPackageManager(),
                        (long) stat.getBlockCount() * (long) stat.getBlockSize(),
                        new IPackageDataObserver.Stub() {
                            @Override
                            public void onRemoveCompleted(String packageName, boolean succeeded)
                                    throws RemoteException {
                                countDownLatch.countDown();
                            }
                        }
                );

                countDownLatch.await();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            return cacheSize;
        }
        //invoked on the UI thread after the background computation finishes. The result of the background computation is passed to this step as a parameter.
        @Override
        protected void onPostExecute(Long result) {
            cacheSize = 0;

            if (onActionListener != null) {
                onActionListener.cleanComplete(BackgroundService.this, result);
            }

            cleaning = false;
        }
    }
    //Return the communication channel to the service.
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    //Called by the system when the service is first created.
    @Override
    public void onCreate() {
        try {
            getPackageSizeInfo = getPackageManager().getClass().getMethod(
                    "getPackageSizeInfo", String.class, IPackageStatsObserver.class);

            freeStorageAndNotify = getPackageManager().getClass().getMethod(
                    "freeStorageAndNotify", long.class, IPackageDataObserver.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }
    //Called by the system every time a client explicitly starts the service
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        //modifying the interface methods alters the major functionality of app features
        if (action != null) {
            if (action.equals(CLEAN_AND_EXIT)) {
                setOnActionListener(new OnActionListener() {        //initialize OnActionListener
                    @Override
                    public void scanStart(Context context) {

                    }

                    @Override
                    public void scanProgressUpdate(Context context, int current, int max) {

                    }

                    @Override
                    public void scanComplete(Context context, List<ListApps> apps) {
                        if (getCacheSize() > 0) {
                            cleanCache();
                        }
                    }

                    @Override
                    public void cleanStart(Context context) {

                    }

                    @Override
                    public void cleanComplete(Context context, long cacheSize) {
                        String msg = getString(R.string.cleaned, Formatter.formatShortFileSize(
                                BackgroundService.this, cacheSize));

                        Toast.makeText(BackgroundService.this, msg, Toast.LENGTH_LONG).show();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                stopSelf();
                            }
                        }, 5000);
                    }
                });

                scanCache();
            }
        }

        return START_NOT_STICKY;
    }

    /**
     * Executes the scan task with the specified parameters. The task returns itself (this) so that the caller
     * can keep a reference to it. This function schedules the task on a queue for a single background thread
     */
    public void scanCache() {
        scanning = true;

        new ScanTask().execute();
    }
    /**
     * Executes the clean task with the specified parameters. The task returns itself (this) so that the caller
     * can keep a reference to it. This function schedules the task on a queue for a single background thread
     */
    public void cleanCache() {
        cleaning = true;

        new CleanTask().execute();
    }
    //getters and setter methods
    public void setOnActionListener(OnActionListener listener) {
        onActionListener = listener;
    }

    public boolean isScanning() {
        return scanning;
    }

    public boolean isCleaning() {
        return cleaning;
    }

    public long getCacheSize() {
        return cacheSize;
    }
}
