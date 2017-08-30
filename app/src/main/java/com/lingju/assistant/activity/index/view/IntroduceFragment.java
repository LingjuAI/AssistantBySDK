package com.lingju.assistant.activity.index.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;

import com.lingju.assistant.R;
import com.lingju.assistant.activity.index.model.ExpandableListAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Dyy on 2017/1/16.
 */
public class  IntroduceFragment extends Fragment {
    private View rootView;
    @BindView(R.id.introduce_list)
    ExpandableListView listView;
    private BaseExpandableListAdapter listAdapter;
    private LayoutInflater inflater;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.frag_introduce_list, container, false);
        ButterKnife.bind(this, rootView);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        inflater=LayoutInflater.from(getActivity());
        listAdapter=new ExpandableListAdapter(getActivity());
        listView.setAdapter(listAdapter);
        listView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

            @Override
            public void onGroupExpand(int groupPosition) {
                for(int i = 0; i < listAdapter.getGroupCount(); i++){
                    if(i != groupPosition && listView.isGroupExpanded(i)){
                        listView.collapseGroup(i);
                    }
                }
            }
        });
    }

}
