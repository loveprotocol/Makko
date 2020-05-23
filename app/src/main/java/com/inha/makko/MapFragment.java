package com.inha.makko;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment implements View.OnClickListener, MapView.CurrentLocationEventListener{
    private AppCompatImageButton currentMyLocButton;
    private MapView mapView;
    private MapPOIItem mDefaultMarker;

    private static final MapPoint DEFAULT_MARKER_POINT = MapPoint.mapPointWithGeoCoord(37.4020737, 127.1086766);

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

        // 위치 권한 체크
        if (TedPermission.isGranted(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            // is Granted True
            getMyCurrentLocation(); // 현재 위치 얻기
        } else {
            requestLocationPermission();    // 위치 권한 얻기
        }

       // createDefaultMarker(mapView);

        currentMyLocButton = view.findViewById(R.id.current_my_location);
        currentMyLocButton.setOnClickListener(this);
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

    public static String getSigneture(Context context){
        PackageManager pm = context.getPackageManager();
        try{
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            for(int i = 0; i < packageInfo.signatures.length; i++){
                Signature signature = packageInfo.signatures[i];
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA");
                    md.update(signature.toByteArray());
                    return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

            }

        }catch(PackageManager.NameNotFoundException e){
            e.printStackTrace();
        }
        return null;
    }

    private void createDefaultMarker(MapView mapView) {
        mDefaultMarker = new MapPOIItem();
        String name = "Default Marker";
        mDefaultMarker.setItemName(name);
        mDefaultMarker.setTag(0);
        mDefaultMarker.setMapPoint(DEFAULT_MARKER_POINT);
        mDefaultMarker.setMarkerType(MapPOIItem.MarkerType.BluePin);
        mDefaultMarker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);

        mapView.addPOIItem(mDefaultMarker);
        mapView.selectPOIItem(mDefaultMarker, true);
        mapView.setMapCenterPoint(DEFAULT_MARKER_POINT, false);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.current_my_location) {
            clickCurrentMyLocButton();
        }
    }

    private void clickCurrentMyLocButton() {
        // 위치 권한 체크
        if (TedPermission.isGranted(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            // is Granted True
            getMyCurrentLocation(); // 현재 위치 얻기
        } else {
            requestLocationPermission();    // 위치 권한 얻기
        }
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float accuracyInMeters) {
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        Log.i("info", String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, accuracyInMeters));
        updateMyLocationInfo(mapPointGeo, accuracyInMeters);
    }

    private void updateMyLocationInfo(MapPoint.GeoCoordinate mapPointGeo, float accuracyInMeters) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Map<String, Object> mapPoint = new HashMap<>();
            mapPoint.put("latitude", mapPointGeo.latitude);
            mapPoint.put("longitude", mapPointGeo.longitude);
            mapPoint.put("accuracyInMeters", accuracyInMeters);

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

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }
}
