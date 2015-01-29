package org.secfirst.umbrella.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.secfirst.umbrella.MainActivity;
import org.secfirst.umbrella.R;
import org.secfirst.umbrella.adapters.CheckListAdapter;
import org.secfirst.umbrella.adapters.SegmentAdapter;
import org.secfirst.umbrella.models.CheckItem;
import org.secfirst.umbrella.models.Segment;
import org.secfirst.umbrella.util.Global;
import org.secfirst.umbrella.util.UmbrellaUtil;

import java.util.List;
import java.util.Locale;

public class TabbedFragment extends Fragment {

    public static final String ARG_DIFFICULTY_NUMBER = "spinner_number";
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    public static int difficulty;
    public long sectionNumber;

    public static TabbedFragment newInstance(long sectionNumber, int spinnerNumber) {
        TabbedFragment tabbedFragment = new TabbedFragment();
        Bundle args = new Bundle();
        tabbedFragment.sectionNumber = sectionNumber;
        difficulty = spinnerNumber;
        tabbedFragment.setArguments(args);
        return tabbedFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tabbed, container, false);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
        mSectionsPagerAdapter.difficulty = difficulty;
        mViewPager = (ViewPager) v.findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public int difficulty;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment;
            Bundle args = new Bundle();
            if (position==0) {
                fragment = new TabbedContentFragment();
            } else {
                fragment = new CheckItemFragment();
            }
            args.putInt(TabbedFragment.ARG_DIFFICULTY_NUMBER, difficulty + 1);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.section1_tab_title1).toUpperCase(l);
                case 1:
                    return getString(R.string.section1_tab_title3).toUpperCase(l);
            }
            return null;
        }
    }

    public static class TabbedContentFragment extends Fragment {

        private List<Segment> mSegments;
        private TextView textDifficulty;

        public TabbedContentFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_tabbed_content,
                    container, false);

            long drawerItem = ((MainActivity) getActivity()).drawerItem;
            ListView contentBox = (ListView) rootView.findViewById(R.id.content_box);
            textDifficulty = (TextView) rootView.findViewById(R.id.difficulty);

            textDifficulty.setText(UmbrellaUtil.getDifficultyString(getArguments().getInt(ARG_DIFFICULTY_NUMBER, 1)));

            switch (getArguments().getInt(ARG_DIFFICULTY_NUMBER, 1)) {
                case 1:
                    // do the beginner content
                case 2:
                    // do the intermediate content
                case 3:
                    // show expert content
                default:
                    //default to beginner for now
            }
            mSegments = Segment.find(Segment.class, "category = ?", String.valueOf(drawerItem));
            contentBox.setAdapter(new SegmentAdapter(getActivity(), mSegments));
            contentBox.setDivider(null);
            return rootView;
        }

    }

    public static class CheckItemFragment extends Fragment {

        private List<CheckItem> mCheckList;
        private ProgressBar checkBar;
        private TextView checkBarText, textDifficulty;
        private CheckListAdapter cLAdapter;

        public CheckItemFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_check_item,
                    container, false);


            final long drawerItem = ((MainActivity) getActivity()).drawerItem;
            ListView contentBox = (ListView) rootView.findViewById(R.id.content_box);
            checkBar = (ProgressBar) rootView.findViewById(R.id.progress_bar_checked);
            checkBarText = (TextView) rootView.findViewById(R.id.check_bar_text);
            setProgressBarTo(0);
            textDifficulty = (TextView) rootView.findViewById(R.id.difficulty);
            textDifficulty.setText(UmbrellaUtil.getDifficultyString(getArguments().getInt(ARG_DIFFICULTY_NUMBER, 1)));
            ImageButton addItem = (ImageButton) rootView.findViewById(R.id.fab);
            addItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Global global = ((MainActivity) getActivity()).getGlobal();
                    if (!global.hasPasswordSet()) {
                        global.setPassword(getActivity());
                    } else {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setTitle("Add a new check item");
                        alert.setMessage("Set a meaningful message for the check item");
                        final EditText pwInput = new EditText(getActivity());
                        alert.setView(pwInput);
                        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                String pw = pwInput.getText().toString();
                                if (pw.length()>4) {
                                    CheckItem nItem = new CheckItem(pw, (int) drawerItem);
                                    nItem.setCustom(1);
                                    nItem.save();
                                    refreshCheckList(drawerItem);
                                    dialog.dismiss();
                                    Toast.makeText(getActivity(), "You have added a new item.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getActivity(), "The item text has to be longer than that", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        });
                        alert.show();
                    }
                }
            });


            switch (getArguments().getInt(TabbedFragment.ARG_DIFFICULTY_NUMBER, 1)) {
                case 1:
                    // do the beginner check list
                case 2:
                    // do the intermediate check list
                case 3:
                    // show expert check list
                default:
                    //default to beginner for now
            }
            refreshCheckList(drawerItem);
            cLAdapter = new CheckListAdapter(getActivity(), mCheckList, this);
            contentBox.setAdapter(cLAdapter);
            contentBox.setDivider(null);
            return rootView;
        }

        public void refreshCheckList(long category) {
            mCheckList = CheckItem.find(CheckItem.class, "category = ?", String.valueOf(category));
            if (cLAdapter!=null) {
                cLAdapter.updateData(mCheckList);
            }
            if (mCheckList.size() > 0) {
                int selected = 0;
                for (CheckItem checkItem : mCheckList) {
                    if (checkItem.getValue()) selected++;
                }
                setProgressBarTo((int) Math.round(selected * 100.0 / mCheckList.size()));
            }
        }

        public void setProgressBarTo(int percent) {
            if (percent>=0 && percent<=100) {
                checkBar.setProgress(percent);
                checkBarText.setText(percent + "% done.");
            }
        }
    }

}