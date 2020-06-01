package com.inha.makko;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendFragment extends Fragment {

    private RecyclerView friendRecyclerView;
    private FriendRecyclerViewAdapter friendRecyclerViewAdapter;

    public FriendFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FriendFragment.
     */
    // TODO: Rename and change types and number of parameters
    static FriendFragment newInstance() {
        return new FriendFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //friendRecyclerView.setAdapter();

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO : Create adapter!!
        //friendRecyclerViewAdapter = crea

        friendRecyclerView = view.findViewById(R.id.fragment_friends_recycler_view);
        friendRecyclerView.setAdapter(friendRecyclerViewAdapter);

        // TODO : Firebase 와 연동? view model 작성해서
    }
}
