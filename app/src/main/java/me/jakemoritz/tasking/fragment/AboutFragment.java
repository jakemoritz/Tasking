package me.jakemoritz.tasking.fragment;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import me.jakemoritz.tasking.R;
import me.jakemoritz.tasking.activity.MainActivity;

public class AboutFragment extends Fragment {

    private MainActivity mainActivity;

    public AboutFragment() {

    }

    public static AboutFragment newInstance(MainActivity mainActivity){
        AboutFragment fragment = new AboutFragment();
        fragment.setRetainInstance(true);
        fragment.mainActivity = mainActivity;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View aboutFragmentLayout = inflater.inflate(R.layout.fragment_about, container, false);

        ListView libraryListView = (ListView) aboutFragmentLayout.findViewById(R.id.library_list);
        libraryListView.setAdapter(new LibraryAdapter(getActivity()));

        return aboutFragmentLayout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mainActivity.enableUpNavigation(true);
    }
}
