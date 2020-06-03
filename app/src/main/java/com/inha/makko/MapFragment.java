package com.inha.makko;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.inha.makko.model.Address;
import com.inha.makko.model.KakaoCoord2AddressResponse;
import com.inha.makko.model.RoadAddress;

import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapView;

import org.joda.time.DateTimeUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment implements View.OnClickListener, ToggleButton.OnCheckedChangeListener, MapView.CurrentLocationEventListener, MapView.POIItemEventListener {
    private ToggleButton currentMyLocButton;
    private ToggleButton friendLocButton;
    private AppCompatImageButton friendSearchButton;
    private MapView mapView;
    private KaKaoLocalAPI kaKaoLocalAPI;
    private Call<KakaoCoord2AddressResponse> callAddress;

    private String myUid;
    private User myInfo;
    private ArrayList<MapPoint> friendPointList = new ArrayList<>();

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapFragment.
     */
    // TODO: Rename and change types and number of parameters
    static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initRetrofit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = view.findViewById(R.id.map_view);
        mapView.setCurrentLocationEventListener(this);
        mapView.setPOIItemEventListener(this);

        // 위치 권한 체크
        if (TedPermission.isGranted(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            // is Granted True
            getMyCurrentLocation(); // 현재 위치 얻기
        } else {
            requestLocationPermission();    // 위치 권한 얻기
        }

        currentMyLocButton = view.findViewById(R.id.current_my_location);
        currentMyLocButton.setOnCheckedChangeListener(this);
        friendLocButton= view.findViewById(R.id.friend_location_button);
        friendLocButton.setOnCheckedChangeListener(this);
        friendSearchButton = view.findViewById(R.id.friend_search_button);
        friendSearchButton.setOnClickListener(this);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            myUid = currentUser.getUid();
        }
    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    /**
     * 현재 위치가 업데이트되면 호출되는 함수
     * MapView.CurrentLocationEventListener
     *
     * @param mapView          현재 지도를 그리고 있는 View
     * @param mapPoint         현재 좌표
     * @param accuracyInMeters 위치 정확도
     */
    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float accuracyInMeters) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.i("info", String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, accuracyInMeters));
        updateMyLocationInfo(mapPointGeo, accuracyInMeters);
        callCoord2Address(mapPointGeo.longitude, mapPointGeo.latitude);
    }

    /**
     * 특정 마커가 선택되었을 때 호출되는 함수
     * MapView.POIItemEventListener
     *
     * @param mapView    현재 지도를 그리고 있는 View
     * @param mapPOIItem 선택된 마커
     */
    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        focusOnTarget(mapPOIItem);
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
        User selectedUser = (User) mapPOIItem.getUserObject();
        String message;
        String addressToShow;

        // 도로명 주소 우선
        if (selectedUser.roadAddress != null && selectedUser.roadAddress.length() != 0) {
            addressToShow = selectedUser.roadAddress;
            message = addressToShow;
        } else if (selectedUser.address != null && selectedUser.address.length() != 0){
            addressToShow = selectedUser.address;
            message = "해당 좌표는 도로명 주소가 존재하지 않습니다.\n\n" + addressToShow;
        } else {
            Toast.makeText(getActivity(), "해당 좌표는 주소로 변환하지 못했습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getActivity() != null) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(selectedUser.name + " 주소 복사")
                    .setMessage(message)
                    .setPositiveButton("복사", (dialog, which) -> {
                        if (getContext() != null) {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", addressToShow);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(getActivity(), "복사되었습니다", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        }
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == R.id.current_my_location) {
            if (isChecked) {
                setOnCurrentLocation();
            } else {
                setOffCurrentLocation();
            }
        } else if (buttonView.getId() == R.id.friend_location_button) {
            if (isChecked) {
                setOnFriendLocation();
            } else {
                setOffFriendLocation();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.friend_search_button) {
            showFriendSearchDialog();
        }
    }

    private void showFriendSearchDialog() {
        if (getActivity() != null) {
            AlertDialog searchDialog = new AlertDialog.Builder(getActivity())
                    .setView(R.layout.partial_edittext_in_dialog)
                    .setTitle("친구 검색")
                    .setPositiveButton("검색", null)
                    .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                    .create();

            searchDialog.setOnShowListener(dialog -> {
                ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> searchFriend(dialog));
            });

            searchDialog.show();

            EditText editText = searchDialog.findViewById(R.id.partial_edit_text);
            if (editText != null) {
                editText.setHint("검색할 친구의 이름을 입력하세요");
            }
        }
    }

    private void searchFriend(DialogInterface dialog) {
        EditText editText = ((AlertDialog)dialog).findViewById(R.id.partial_edit_text);
        if (editText != null) {
            if (editText.getText() == null || editText.getText().toString().equals("")) {
                Toast.makeText(getContext(), "이름을 입력하세요", Toast.LENGTH_SHORT).show();
            } else {
                String friendName = editText.getText().toString();
                MapPOIItem[] itemArray = mapView.findPOIItemByName(friendName);
                if (itemArray != null && itemArray.length > 0) {
                    mapView.selectPOIItem(itemArray[0], false);
                    focusOnTarget(itemArray[0]);
                } else {
                    Toast.makeText(getContext(), "검색 결과 없음", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
            }
        }
    }

    /**
     * 해당 마커의 위치를 지도 중심점으로 설정하고 최대로 Zoom In
     * @param mapPOIItem
     */
    private void focusOnTarget(MapPOIItem mapPOIItem) {
        mapView.setMapCenterPoint(mapPOIItem.getMapPoint(), false);
        mapView.setZoomLevel(1, true);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.kakao_local_api_base_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        kaKaoLocalAPI = retrofit.create(KaKaoLocalAPI.class);
    }

    private void callCoord2Address(Double x, Double y) {
        callAddress = kaKaoLocalAPI.coord2address(x, y);
        callAddress.enqueue(new Callback<KakaoCoord2AddressResponse>() {
            @Override
            public void onResponse(@NonNull Call<KakaoCoord2AddressResponse> call, @NonNull Response<KakaoCoord2AddressResponse> response) {
                KakaoCoord2AddressResponse result = response.body();
                if (result != null && result.getMeta().getTotalCount() == 1) {

                    Map<String, Object> mapPoint = new HashMap<>();

                    Address address = result.getDocuments().get(0).getAddress();
                    if (address != null && address.getAddressName() != null) {
                        mapPoint.put("address", address.getAddressName());
                    } else {
                        mapPoint.put("address", "");
                    }

                    RoadAddress roadAddress = result.getDocuments().get(0).getRoadAddress();
                    if (roadAddress != null && roadAddress.getAddressName() != null) {
                        mapPoint.put("roadAddress", roadAddress.getAddressName());
                    } else {
                        mapPoint.put("roadAddress", "");
                    }

                    if (mapPoint.size() > 0) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            FirebaseFirestore.getInstance().collection("users")
                                    .document(currentUser.getUid())
                                    .update(mapPoint)
                                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "현재 위치 주소 업데이트 실패", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(getActivity(), "현재 좌표 주소 변환 실패", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<KakaoCoord2AddressResponse> call, @NonNull Throwable t) {
                Toast.makeText(getActivity(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setOnCurrentLocation() {
        // 위치 권한 체크
        if (TedPermission.isGranted(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            // is Granted True
            getMyCurrentLocation(); // 현재 위치 얻기
        } else {
            requestLocationPermission();    // 위치 권한 얻기
        }
    }

    private void setOffCurrentLocation() {
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
    }

    private void setOnFriendLocation() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        ArrayList<User> allUserArrayList = (ArrayList<User>) queryDocumentSnapshots.toObjects(User.class);
                        showFriendsLocation(allUserArrayList);
                        currentMyLocButton.setChecked(false);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void setOffFriendLocation() {
        mapView.removeAllPOIItems();
        friendSearchButton.setVisibility(View.INVISIBLE);
    }

    private void updateMyLocationInfo(MapPoint.GeoCoordinate mapPointGeo, float accuracyInMeters) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> mapPoint = new HashMap<>();
            mapPoint.put("latitude", mapPointGeo.latitude);
            mapPoint.put("longitude", mapPointGeo.longitude);
            mapPoint.put("accuracyInMeters", accuracyInMeters);
            mapPoint.put("lastUpdateAt", DateTimeUtils.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .update(mapPoint)
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getActivity(), "현재 위치 업데이트 실패", Toast.LENGTH_SHORT).show();
                        }
                    });

        }
    }

    private void getMyCurrentLocation() {
        mapView.setCurrentLocationRadius(100); // meter
        mapView.setCurrentLocationRadiusFillColor(Color.argb(77, 255, 255, 0));
        mapView.setCurrentLocationRadiusStrokeColor(Color.argb(77, 255, 165, 0));

        MapPOIItem.ImageOffset trackingImageAnchorPointOffset = new MapPOIItem.ImageOffset(28, 28); // 좌하단(0,0) 기준 앵커포인트 오프셋
        MapPOIItem.ImageOffset directionImageAnchorPointOffset = new MapPOIItem.ImageOffset(65, 65);
        MapPOIItem.ImageOffset offImageAnchorPointOffset = new MapPOIItem.ImageOffset(15, 15);
        mapView.setCustomCurrentLocationMarkerTrackingImage(R.drawable.custom_arrow_map_present_tracking, trackingImageAnchorPointOffset);
        mapView.setCustomCurrentLocationMarkerDirectionImage(R.drawable.custom_map_present_direction, directionImageAnchorPointOffset);
        mapView.setCustomCurrentLocationMarkerImage(R.drawable.custom_map_present, offImageAnchorPointOffset);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
        mapView.setZoomLevel(1, true);
    }

    private void requestLocationPermission() {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                getMyCurrentLocation();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(getActivity(), "위치 권한 승인 실패", Toast.LENGTH_SHORT).show();
            }
        };

        if (getActivity() != null) {
            TedPermission.with(getActivity())
                    .setPermissionListener(permissionListener)
                    .setDeniedTitle("위치 접근 권한 필요")
                    .setDeniedMessage("현재 위치를 확인하려면, 위치 접근 권한이 필요합니다.")
                    .setPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
                    .check();
        }
    }

    private void createFriendMarker(User user, MapPoint mapPoint) {
        MapPOIItem friendMarker = new MapPOIItem();
        friendMarker.setItemName(user.name);
        friendMarker.setUserObject(user);
        friendMarker.setMapPoint(mapPoint);
        friendMarker.setMarkerType(MapPOIItem.MarkerType.RedPin);
        friendMarker.setSelectedMarkerType(MapPOIItem.MarkerType.YellowPin);

        mapView.addPOIItem(friendMarker);
    }

    private void showFriendsLocation(ArrayList<User> allUserArrayList) {
        for (User user : allUserArrayList) {
            if (user.uid.equals(myUid)) {
                myInfo = user;
                break;
            }
        }

        friendPointList.clear();
        if (myInfo.friendArray != null) {
            for (String friendId : myInfo.friendArray) {
                for (User user : allUserArrayList) {
                    if (user.uid.equals(friendId)) {
                        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(user.latitude, user.longitude);
                        friendPointList.add(mapPoint);
                        createFriendMarker(user, mapPoint);
                        break;
                    }
                }
            }

            if (friendPointList.size() == 0) {
                Toast.makeText(getContext(), "현재 등록된 친구가 없습니다", Toast.LENGTH_SHORT).show();
                friendLocButton.setOnCheckedChangeListener(null);
                friendLocButton.setChecked(false);
                friendLocButton.setOnCheckedChangeListener(this);
            } else {
                showAll();
                friendSearchButton.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(getContext(), "현재 등록된 친구가 없습니다", Toast.LENGTH_SHORT).show();
            friendLocButton.setOnCheckedChangeListener(null);
            friendLocButton.setChecked(false);
            friendLocButton.setOnCheckedChangeListener(this);
        }
    }

    private void showAll() {
        int padding = 20;
        float minZoomLevel = 7;
        float maxZoomLevel = 10;

        MapPoint myMapPoint = MapPoint.mapPointWithGeoCoord(myInfo.latitude, myInfo.longitude);
        mapView.setMapCenterPoint(myMapPoint, false);

        MapPoint[] friendPointArray = new MapPoint[friendPointList.size()];
        friendPointList.toArray(friendPointArray);
        MapPointBounds bounds = new MapPointBounds(friendPointArray);
        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(bounds, padding, minZoomLevel, maxZoomLevel));
    }


    private static String getSigneture(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            for (int i = 0; i < packageInfo.signatures.length; i++) {
                Signature signature = packageInfo.signatures[i];
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
