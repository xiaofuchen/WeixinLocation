package com.weixin.location;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.google.gson.Gson;
import com.weixin.location.adapter.AddressAdapter;
import com.weixin.location.utils.DataConversionUtils;
import com.weixin.location.utils.DatasKey;
import com.weixin.location.utils.OnItemClickLisenter;
import com.weixin.location.utils.SPUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView        mMapView;
    private ImageView      mIvBack;
    private ImageView      mIvSearch;
    private ImageView      mIvLocation;
    private ImageView      mIvCenterLocation;
    private Button         mBtSend;
    private RecyclerView   mRecyclerView;
    private AddressAdapter mAddressAdapter;
    private List<PoiItem>  mList;
    private PoiItem        userSelectPoiItem;

    private AMap   mAMap;
    private Marker mMarker, mLocationGpsMarker, mSelectByListMarker;
    private UiSettings      mUiSettings;
    private PoiSearch       mPoiSearch;
    private PoiSearch.Query mQuery;
    private boolean         isSearchData = false;//是否搜索地址数据
    private int             searchAllPageNum;//Poi搜索最大页数，可应用于上拉加载更多
    private int             searchNowPageNum;//当前poi搜索页数
    private float           zoom         = 14;//地图缩放级别

    private AMapLocationClient       locationClient = null;
    private AMapLocationClientOption locationOption = new AMapLocationClientOption();
    private AMapLocation             location;
    private AMapLocationListener     mAMapLocationListener;

    private onPoiSearchLintener                   mOnPoiSearchListener;
    private View.OnClickListener                  mOnClickListener;
    private GeocodeSearch.OnGeocodeSearchListener mOnGeocodeSearchListener;

    private Gson gson;

    private ObjectAnimator mTransAnimator;//地图中心标志动态

    private static final int SEARCHREQUESTCODE = 1001;

    // 要申请的权限
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDatas(savedInstanceState);
        initListener();
        initPermission();
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (null != data && SEARCHREQUESTCODE == requestCode) {
            try {
                userSelectPoiItem = (PoiItem) data.getParcelableExtra(DatasKey.SEARCH_INFO);
                if (null != userSelectPoiItem) {
                    isSearchData = false;
                    doSearchQuery(true, "", location.getCity(), userSelectPoiItem.getLatLonPoint());
                    moveMapCamera(userSelectPoiItem.getLatLonPoint().getLatitude(), userSelectPoiItem.getLatLonPoint().getLongitude());
//                    refleshMark(userSelectPoiItem.getLatLonPoint().getLatitude(), userSelectPoiItem.getLatLonPoint().getLongitude());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocation();
        mMapView.onDestroy();
        if (null != mPoiSearch) {
            mPoiSearch = null;
        }
        if (null != gson) {
            gson = null;
        }
        if (null != locationClient) {
            locationClient.onDestroy();
        }
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.map);
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mIvSearch = (ImageView) findViewById(R.id.iv_search);
        mIvLocation = (ImageView) findViewById(R.id.iv_location);
        mIvCenterLocation = (ImageView) findViewById(R.id.iv_center_location);
        mBtSend = (Button) findViewById(R.id.bt_send);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);

    }

    private void initListener() {
        //监测地图画面的移动
        mAMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {
                if (null != location && null != cameraPosition && isSearchData) {
                    mIvLocation.setImageResource(R.mipmap.location_gps_black);
                    zoom = cameraPosition.zoom;
                    if (null != mSelectByListMarker) {
                        mSelectByListMarker.setVisible(false);
                    }
                    getAddressInfoByLatLong(cameraPosition.target.latitude, cameraPosition.target.longitude);
                    startTransAnimator();
//                    doSearchQuery(true, "", location.getCity(), new LatLonPoint(cameraPosition.target.latitude, cameraPosition.target.longitude));
                }
                if (!isSearchData) {
                    isSearchData = true;
                }
            }

            @Override
            public void onCameraChange(CameraPosition cameraPosition) {

            }
        });

        //设置触摸地图监听器
        mAMap.setOnMapClickListener(new AMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                isSearchData = true;
            }
        });

        //Poi搜索监听器
        mOnPoiSearchListener = new onPoiSearchLintener();

        //逆地址搜索监听器
        mOnGeocodeSearchListener = new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                if (i == 1000) {
                    if (regeocodeResult != null) {
                        userSelectPoiItem = DataConversionUtils.changeToPoiItem(regeocodeResult);
                        if (null != mList) {
                            mList.clear();
                        }
                        mList.addAll(regeocodeResult.getRegeocodeAddress().getPois());
                        if (null != userSelectPoiItem) {
                            mList.add(0, userSelectPoiItem);
                        }
                        mAddressAdapter.setList(mList);
                        mRecyclerView.smoothScrollToPosition(0);
                    }
                }

            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

            }
        };

        //gps定位监听器
        mAMapLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation loc) {
                try {
                    if (null != loc) {
                        stopLocation();
                        if (loc.getErrorCode() == 0) {//可在其中解析amapLocation获取相应内容。
                            location = loc;
                            SPUtils.putString(MainActivity.this, DatasKey.LOCATION_INFO, gson.toJson(location));
                            doWhenLocationSucess();
                        } else {
                            //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                            showToastWithErrorInfo(loc.getErrorCode());
                            Log.e("AmapError", "location Error, ErrCode:"
                                    + loc.getErrorCode() + ", errInfo:"
                                    + loc.getErrorInfo());

                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        //recycleview列表监听器
        mAddressAdapter.setOnItemClickLisenter(new OnItemClickLisenter() {
            @Override
            public void onItemClick(int position) {
                try {
                    isSearchData = false;
                    mIvLocation.setImageResource(R.mipmap.location_gps_black);
                    moveMapCamera(mList.get(position).getLatLonPoint().getLatitude(), mList.get(position).getLatLonPoint().getLongitude());
                    refleshSelectByListMark(mList.get(position).getLatLonPoint().getLatitude(), mList.get(position).getLatLonPoint().getLongitude());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        //控件点击监听器
        mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.iv_back:
                        finish();
                        break;
                    case R.id.iv_search:
//                        Toast.makeText(MainActivity.this, "搜索", Toast.LENGTH_SHORT).show();
                        startActivityForResult(new Intent(MainActivity.this, SearchActivity.class), SEARCHREQUESTCODE);
                        break;
                    case R.id.iv_location:
//                        Toast.makeText(MainActivity.this, "定位", Toast.LENGTH_SHORT).show();
                        mIvLocation.setImageResource(R.mipmap.location_gps_green);
                        if (null != mSelectByListMarker) {
                            mSelectByListMarker.setVisible(false);
                        }
                        if (null == location) {
                            startLocation();
                        } else {
                            doWhenLocationSucess();
                        }
                        break;
                    case R.id.bt_send:
                        if (null != mList && 0 < mList.size() && null != mAddressAdapter) {
                            int position = mAddressAdapter.getSelectPositon();
                            if (position < 0) {
                                position = 0;
                            } else if (position > mList.size()) {
                                position = mList.size();
                            }
                            PoiItem poiItem = mList.get(position);
                            Toast.makeText(MainActivity.this, "发送：" + poiItem.getTitle() + "  " + poiItem.getSnippet() + "  " + "纬度：" + poiItem.getLatLonPoint().getLatitude() + "  " + "经度：" + poiItem.getLatLonPoint().getLongitude(), Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };

        mIvBack.setOnClickListener(mOnClickListener);
        mIvSearch.setOnClickListener(mOnClickListener);
        mIvLocation.setOnClickListener(mOnClickListener);
        mBtSend.setOnClickListener(mOnClickListener);

    }

    private void initDatas(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);// 此方法必须重写
        mAMap = mMapView.getMap();

        mUiSettings = mAMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(false);//是否显示地图中放大缩小按钮
        mUiSettings.setMyLocationButtonEnabled(false); // 是否显示默认的定位按钮
        mUiSettings.setScaleControlsEnabled(true);//是否显示缩放级别
        mAMap.setMyLocationEnabled(false);// 是否可触发定位并显示定位层

        mList = new ArrayList<>();
        mAddressAdapter = new AddressAdapter(this, mList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAddressAdapter);

        gson = new Gson();

        mTransAnimator = ObjectAnimator.ofFloat(mIvCenterLocation, "translationY", 0f, -80f, 0f);
        mTransAnimator.setDuration(800);
//        mTransAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        if (null == locationClient) {
            //初始化client
            locationClient = new AMapLocationClient(this.getApplicationContext());
            //设置定位参数
            locationClient.setLocationOption(getDefaultOption());
            // 设置定位监听
            locationClient.setLocationListener(mAMapLocationListener);
        }
    }

    /**
     * 默认的定位参数
     */
    private AMapLocationClientOption getDefaultOption() {
        AMapLocationClientOption mOption = new AMapLocationClientOption();
        mOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        mOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        mOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        mOption.setInterval(2000);//可选，设置定位间隔。默认为2秒
        mOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        mOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        mOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        mOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        mOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        mOption.setMockEnable(true);//如果您希望位置被模拟，请通过setMockEnable(true);方法开启允许位置模拟
        return mOption;
    }

    private void initPermission() {

        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            int i = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[0]);
            int l = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[1]);
            int m = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[2]);
            int n = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[3]);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            if (i != PackageManager.PERMISSION_GRANTED || l != PackageManager.PERMISSION_GRANTED || m != PackageManager.PERMISSION_GRANTED ||
                    n != PackageManager.PERMISSION_GRANTED) {
                // 如果没有授予该权限，就去提示用户请求
                ActivityCompat.requestPermissions(this, permissions, 321);
            } else {
                startLocation();
            }
        }
    }

    /**
     * 开始定位
     */
    public void startLocation() {
        initLocation();
        // 设置定位参数
        locationClient.setLocationOption(locationOption);
        // 启动定位
        locationClient.startLocation();
    }

    /**
     * 停止定位
     */
    private void stopLocation() {
        if (null != locationClient) {
            locationClient.stopLocation();
        }
    }

    /**
     * 当定位成功需要做的事情
     */
    private void doWhenLocationSucess() {
        isSearchData = false;
        userSelectPoiItem = DataConversionUtils.changeToPoiItem(location);
        doSearchQuery(true, "", location.getCity(), new LatLonPoint(location.getLatitude(), location.getLongitude()));
        moveMapCamera(location.getLatitude(), location.getLongitude());
        refleshLocationMark(location.getLatitude(), location.getLongitude());
    }


    /**
     * 移动动画
     */
    private void startTransAnimator() {
        if (null != mTransAnimator && !mTransAnimator.isRunning()) {
            mTransAnimator.start();
        }
    }

    /**
     * 把地图画面移动到定位地点(使用moveCamera方法没有动画效果)
     *
     * @param latitude
     * @param longitude
     */
    private void moveMapCamera(double latitude, double longitude) {
        if (null != mAMap) {
            mAMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom));
        }
    }

    /**
     * 刷新地图标志物位置
     *
     * @param latitude
     * @param longitude
     */
    private void refleshMark(double latitude, double longitude) {
        if (mMarker == null) {
            mMarker = mAMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                            .decodeResource(getResources(), android.R.color.transparent)))
                    .draggable(true));
        }
        mMarker.setPosition(new LatLng(latitude, longitude));
        mAMap.invalidate();

    }

    /**
     * 刷新地图标志物gps定位位置
     *
     * @param latitude
     * @param longitude
     */
    private void refleshLocationMark(double latitude, double longitude) {
        if (mLocationGpsMarker == null) {
            mLocationGpsMarker = mAMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                            .decodeResource(getResources(), R.mipmap.location_blue)))
                    .draggable(true));
        }
        mLocationGpsMarker.setPosition(new LatLng(latitude, longitude));
        mAMap.invalidate();

    }

    /**
     * 刷新地图标志物选中列表的位置
     *
     * @param latitude
     * @param longitude
     */
    private void refleshSelectByListMark(double latitude, double longitude) {
        if (mSelectByListMarker == null) {
            mSelectByListMarker = mAMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                            .decodeResource(getResources(), R.mipmap.location_red)))
                    .draggable(true));
        }
        mSelectByListMarker.setPosition(new LatLng(latitude, longitude));
        if (!mSelectByListMarker.isVisible()) {
            mSelectByListMarker.setVisible(true);
        }
        mAMap.invalidate();

    }

    /**
     * 开始进行poi搜索
     *
     * @param isReflsh 是否为刷新数据
     * @param keyWord
     * @param city
     * @param lpTemp
     */
    protected void doSearchQuery(boolean isReflsh, String keyWord, String city, LatLonPoint lpTemp) {
        mQuery = new PoiSearch.Query(keyWord, "", city);//第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        mQuery.setPageSize(30);// 设置每页最多返回多少条poiitem
        if (isReflsh) {
            searchNowPageNum = 0;
        } else {
            searchNowPageNum++;
        }
        if (searchNowPageNum > searchAllPageNum) {
            return;
        }
        mQuery.setPageNum(searchNowPageNum);// 设置查第一页


        mPoiSearch = new PoiSearch(this, mQuery);
        mOnPoiSearchListener.IsReflsh(isReflsh);
        mPoiSearch.setOnPoiSearchListener(mOnPoiSearchListener);
        if (lpTemp != null) {
            mPoiSearch.setBound(new PoiSearch.SearchBound(lpTemp, 10000, true));//该范围的中心点-----半径，单位：米-----是否按照距离排序
        }
        mPoiSearch.searchPOIAsyn();// 异步搜索
    }


    /**
     * 通过经纬度获取当前地址详细信息，逆地址编码
     *
     * @param latitude
     * @param longitude
     */
    private void getAddressInfoByLatLong(double latitude, double longitude) {
        GeocodeSearch geocodeSearch = new GeocodeSearch(this);
        /*
        point - 要进行逆地理编码的地理坐标点。
        radius - 查找范围。默认值为1000，取值范围1-3000，单位米。
        latLonType - 输入参数坐标类型。包含GPS坐标和高德坐标。 可以参考RegeocodeQuery.setLatLonType(String)
        */
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(latitude, longitude), 3000, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
        geocodeSearch.setOnGeocodeSearchListener(mOnGeocodeSearchListener);
    }

    private void showToastWithErrorInfo(int error) {
        String tips = "定位错误码：" + error;
        switch (error) {
            case 4:
                tips = "请检查设备网络是否通畅，检查通过接口设置的网络访问超时时间，建议采用默认的30秒。";
                break;
            case 7:
                tips = "请仔细检查key绑定的sha1值与apk签名sha1值是否对应。";
                break;
            case 12:
                tips = "请在设备的设置中开启app的定位权限。";
                break;
            case 18:
                tips = "建议手机关闭飞行模式，并打开WIFI开关";
                break;
            case 19:
                tips = "建议手机插上sim卡，打开WIFI开关";
                break;
        }
        Toast.makeText(MainActivity.this.getApplicationContext(), tips, Toast.LENGTH_LONG).show();
    }

    //重写Poi搜索监听器，可扩展上拉加载数据，下拉刷新
    class onPoiSearchLintener implements PoiSearch.OnPoiSearchListener {
        private boolean isReflsh;//是为下拉刷新，否为上拉加载更多

        public void IsReflsh(boolean isReflsh) {
            this.isReflsh = isReflsh;
        }

        @Override
        public void onPoiSearched(PoiResult result, int i) {
            if (i == 1000) {
                if (result != null && result.getQuery() != null) {// 搜索poi的结果
                    searchAllPageNum = result.getPageCount();
                    if (result.getQuery().equals(mQuery)) {// 是否是同一条
                        if (isReflsh && null != mList) {
                            mList.clear();
                            if (null != userSelectPoiItem) {
                                mList.add(0, userSelectPoiItem);
                            }
                        }
                        mList.addAll(result.getPois());// 取得第一页的poiitem数据，页数从数字0开始
                        if (null != mAddressAdapter) {
                            mAddressAdapter.setList(mList);
                            mRecyclerView.smoothScrollToPosition(0);
                        }
                    }
                }
            }
        }

        @Override
        public void onPoiItemSearched(PoiItem poiItem, int i) {

        }
    }

    /**
     * 用户权限 申请 的回调方法
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 321) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    //如果没有获取权限，那么可以提示用户去设置界面--->应用权限开启权限
                    Toast toast = Toast.makeText(this, "请开启权限", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                } else {
                    //获取权限成功
                    startLocation();
                }
            }
        }
    }
}
