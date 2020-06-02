package com.inha.makko;


import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class FriendFragment extends Fragment implements View.OnClickListener {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView friendRecyclerView;
    private FriendRecyclerViewAdapter friendRecyclerViewAdapter = null;
    private FloatingActionButton friendAddBtn;
    private User myInfo = null;
    private ArrayList<User> friendArrayList = new ArrayList<>();

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_friend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout = view.findViewById(R.id.fragment_friends_swipe_refresh_layout);
        friendRecyclerView = view.findViewById(R.id.fragment_friends_recycler_view);
        friendAddBtn = view.findViewById(R.id.fragment_friends_floating_btn);
        friendAddBtn.setOnClickListener(this);

        initializeSwipeRefreshLayout();
        getFriendList();
    }

    private void initializeSwipeRefreshLayout() {
        if (getContext() != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    swipeRefreshLayout.setRefreshing(true);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            friendArrayList.clear();
                            getFriendList();
                        }
                    }, 2000);
                }
            });
        }
    }

    private void getFriendList() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        ArrayList<User> allUserArrayList = (ArrayList<User>) queryDocumentSnapshots.toObjects(User.class);
                        myInfo = null;

                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            for (User user : allUserArrayList) {
                                if (user.uid.equals(currentUser.getUid())) {
                                    myInfo = user;
                                    break;
                                }
                            }

                            if (myInfo != null) {
                                for (String friendId : myInfo.friendArray) {
                                    for (User user : allUserArrayList) {
                                        if (user.uid.equals(friendId)) {
                                            friendArrayList.add(user);
                                            break;
                                        }
                                    }
                                }
                            }

                            if (friendRecyclerViewAdapter == null) {
                                initializeRecyclerView(friendArrayList);
                            } else {
                                updateRecyclerView(friendArrayList);
                            }
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void initializeRecyclerView(ArrayList<User> allUserArrayList) {
        if (getActivity() != null) {
            friendRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            friendRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
            friendRecyclerViewAdapter = new FriendRecyclerViewAdapter(allUserArrayList);
            friendRecyclerView.setAdapter(friendRecyclerViewAdapter);
        }
    }

    private void updateRecyclerView(ArrayList<User> friendArrayList) {
        friendRecyclerViewAdapter.refreshList(friendArrayList);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fragment_friends_floating_btn) {
            showFriendAddDialog();
        }
    }

    private void showFriendAddDialog() {
        if (getActivity() != null) {
            AlertDialog searchDialog = new AlertDialog.Builder(getActivity())
                    .setView(R.layout.partial_edittext_in_dialog)
                    .setTitle("친구 등록")
                    .setPositiveButton("등록", null)
                    .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                    .create();

            searchDialog.setOnShowListener(dialog -> {
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> addFriend(dialog));
            });

            searchDialog.show();

            EditText editText = searchDialog.findViewById(R.id.partial_edit_text);
            if (editText != null) {
                editText.setHint("등록할 친구의 이름을 입력하세요");
            }
        }
    }

    private void addFriend(DialogInterface dialog) {
        EditText editText = ((AlertDialog)dialog).findViewById(R.id.partial_edit_text);
        if (editText != null) {
            if (editText.getText() == null || editText.getText().toString().equals("")) {
                Toast.makeText(getContext(), "이름을 입력하세요", Toast.LENGTH_SHORT).show();
            } else {
                ProgressBar loadingBar = ((AlertDialog)dialog).findViewById(R.id.partial_progress_bar);
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.VISIBLE);
                }

                String friendName = editText.getText().toString();

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .whereEqualTo("name", friendName)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                ArrayList<User> userArrayList = (ArrayList<User>) queryDocumentSnapshots.toObjects(User.class);

                                if (userArrayList.size() == 0) {
                                    if (loadingBar != null) {
                                        loadingBar.setVisibility(View.INVISIBLE);
                                    }
                                    Toast.makeText(getContext(), "검색 결과 없음", Toast.LENGTH_SHORT).show();
                                } else {
                                    for (User user : userArrayList) {
                                        friendRecyclerViewAdapter.addItem(user);
                                        myInfo.friendArray.add(user.uid);
                                    }

                                    Map<String, Object> friendMap = new HashMap<>();
                                    friendMap.put("friendArray", myInfo.friendArray);

                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(myInfo.uid)
                                            .update(friendMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    if (loadingBar != null) {
                                                        loadingBar.setVisibility(View.INVISIBLE);
                                                    }
                                                    Toast.makeText(getContext(), "친구 추가 완료", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    if (loadingBar != null) {
                                                        loadingBar.setVisibility(View.INVISIBLE);
                                                    }
                                                    Toast.makeText(getContext(), "친구 추가 실패", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                }
                                            });
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (loadingBar != null) {
                                    loadingBar.setVisibility(View.INVISIBLE);
                                }
                                Toast.makeText(getContext(), "오류 발생. 잠시 후 재시도 바랍니다.", Toast.LENGTH_SHORT).show();
                            }
                        });


            }
        }
    }
}
