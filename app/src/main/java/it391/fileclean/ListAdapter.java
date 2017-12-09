package it391.fileclean;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 *  An Adapter that acts as a bridge between an AdapterView and the underlying data for that view.
 *  The Adapter provides access to the data items. The Adapter is also responsible for making a
 *  View for each item in the data set. Modify this class to alter the how the app data is displayed
 *  in the list and behavior related to user interaction with onItemClicks
 */
public class ListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
    //Sorting filters. Modify for additional sorting filters
    public enum SortBy {
        APP_NAME,
        CACHE_SIZE
    }
    //object declarations
    private List<ListApps> items;
    private List<ListApps> filteredItems;
    private Context myContext;
    private SortBy myLastSortBy;
    //store info for apps in list
    private class ViewHolder {
        ImageView image;
        TextView name;
        TextView size;
        String packageName;
    }
    //constructor
    public ListAdapter(Context context) {
        myContext = context;

        items = new ArrayList<>();
        filteredItems = new ArrayList<>();
    }

    /**
     * override getters below
     *
     */
    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Override
    public ListApps getItem(int i) {
        return filteredItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    /**
     * Get a View that displays the data at the specified position in the data set.
     * You can either create a View manually or inflate it from an XML layout file.
     * When the View is inflated, the parent View will apply default layout parameters
     * unless you use inflate(int, android.view.ViewGroup, boolean)to specify a root view
     * and to prevent attachment to the root.
     */
    @Override
    public View getView(int i, View convertView, ViewGroup viewParent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(myContext).inflate(R.layout.app_layout, viewParent, false);
        }

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        if (viewHolder == null) {
            viewHolder = new ViewHolder();

            viewHolder.image = (ImageView) convertView.findViewById(R.id.app_icon);
            viewHolder.name = (TextView) convertView.findViewById(R.id.app_name);
            viewHolder.size = (TextView) convertView.findViewById(R.id.app_size);

            convertView.setTag(viewHolder);
        }

        ListApps item = getItem(i);

        viewHolder.image.setImageDrawable(item.getApplicationIcon());
        viewHolder.name.setText(item.getApplicationName());
        viewHolder.size.setText(Formatter.formatShortFileSize(myContext, item.getCacheSize()));
        viewHolder.packageName = item.getPackageName();

        return convertView;
    }
    //item setter method
    public void setItems(List<ListApps> items, SortBy sortBy, String filter) {
        this.items = items;

        myLastSortBy = null;

        if (this.items.size() > 0) {
            sortResults(sortBy, filter);
        } else {
            filteredItems = new ArrayList<>(this.items);

            notifyDataSetChanged();
        }
    }
    //modify to alter sorting behavior for results in list
    public void sortResults(final SortBy sortBy, String filter) {
        if (sortBy != myLastSortBy) {
            myLastSortBy = sortBy;

            ArrayList<ListApps> items = new ArrayList<>(this.items);

            Collections.sort(items, new Comparator<ListApps>() {
                @Override
                public int compare(ListApps lhs, ListApps rhs) {
                    switch (sortBy) {
                        case APP_NAME:
                            return lhs.getApplicationName().compareToIgnoreCase(
                                    rhs.getApplicationName());

                        case CACHE_SIZE:
                            return (int) (rhs.getCacheSize() - lhs.getCacheSize());
                    }

                    return 0;
                }
            });

            this.items = items;
        }

        if (!filter.equals("")) {
            List<ListApps> filteredItems = new ArrayList<>();

           Locale current = myContext.getResources().getConfiguration().locale;

            for (ListApps item : items) {
                if (item.getApplicationName().toLowerCase(current).contains(
                        filter.toLowerCase(current))) {
                    filteredItems.add(item);
                }
            }

            this.filteredItems = filteredItems;
        } else {
            filteredItems = new ArrayList<>(items);
        }

        notifyDataSetChanged();
    }
    //reset sorting
    public void resetFilter() {
        filteredItems = new ArrayList<>(items);

        notifyDataSetChanged();
    }

    /**
     * Callback method to be invoked when an item in this AdapterView has been clicked.
     * Implementers can call getItemAtPosition(position) if they need to access the data
     * associated with the selected item.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        if (viewHolder != null && viewHolder.packageName != null) {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + viewHolder.packageName));

            myContext.startActivity(intent);
        }
    }
}
