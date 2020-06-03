package com.inha.makko;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import net.daum.mf.map.api.MapPOIItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;


public class MainActivity extends AppCompatActivity {

    private String[] tabTitleList = {"지도", "친구"};
    private TabLayout mainTabLayout;
    private ViewPager mainViewPager;
    GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainTabLayout = findViewById(R.id.tab_layout_main);
        mainViewPager = findViewById(R.id.viewpager_main);
        if (mainTabLayout.getTabCount() == 0) {
            initTabLayout();
            initViewPager();
        }

        /* Google Sign in Configuration */
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void initTabLayout() {
        for (String s : tabTitleList) {
            TabLayout.Tab tab = mainTabLayout.newTab().setText(s);
            mainTabLayout.addTab(tab);
        }

        mainTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mainViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void initViewPager() {
        MainViewPagerAdapter adapter = new MainViewPagerAdapter(getSupportFragmentManager(), mainTabLayout.getTabCount());
        mainViewPager.setAdapter(adapter);
        mainViewPager.setOffscreenPageLimit(mainTabLayout.getTabCount());
        mainViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mainTabLayout));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_menu_logout) {
            // FireBase auth signOut
            FirebaseAuth.getInstance().signOut();
            // Google SignOut
            mGoogleSignInClient.signOut().addOnCompleteListener(this,
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });
            return true;
        } else if (item.getItemId() == R.id.item_menu_rename) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance().collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                User user = documentSnapshot.toObject(User.class);
                                showRenameDialog(user);
                            }
                        });
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void showRenameDialog(User user) {

        AlertDialog renameDialog = new AlertDialog.Builder(this)
                .setView(R.layout.partial_edittext_in_dialog)
                .setTitle("이름 변경")
                .setPositiveButton("변경", null)
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .create();

        renameDialog.setOnShowListener(dialog -> {
            ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> rename(dialog, user));
        });

        renameDialog.show();

        EditText editText = renameDialog.findViewById(R.id.partial_edit_text);
        if (editText != null) {
            editText.setText(user.name);
            editText.setSelection(editText.length());
            editText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editText.selectAll();
                }
            });
        }
    }

    private void rename(DialogInterface dialog, User user) {
        EditText editText = ((AlertDialog)dialog).findViewById(R.id.partial_edit_text);
        if (editText != null) {
            if (editText.getText() == null || editText.getText().toString().equals("")) {
                Toast.makeText(this, "변경할 이름을 입력하세요", Toast.LENGTH_SHORT).show();
            } else {
                ProgressBar loadingBar = ((AlertDialog)dialog).findViewById(R.id.partial_progress_bar);
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.VISIBLE);
                }

                String newName = editText.getText().toString();

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .whereEqualTo("name", newName)
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                ArrayList<User> sameNameList = (ArrayList<User>) queryDocumentSnapshots.toObjects(User.class);
                                if (sameNameList.size() == 0) {
                                    Map<String, Object> newNameMap = new HashMap<>();
                                    newNameMap.put("name", newName);

                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(user.uid)
                                            .update(newNameMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(MainActivity.this, "변경 성공", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(MainActivity.this, "변경 실패", Toast.LENGTH_SHORT).show();
                                                    dialog.dismiss();
                                                }
                                            });
                                } else {
                                    if (loadingBar != null) {
                                        loadingBar.setVisibility(View.INVISIBLE);
                                    }
                                    Toast.makeText(MainActivity.this, "동일한 이름이 존재합니다", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (loadingBar != null) {
                                    loadingBar.setVisibility(View.INVISIBLE);
                                }
                                Toast.makeText(MainActivity.this, "변경 실패", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
}
