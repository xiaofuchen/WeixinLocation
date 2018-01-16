# WeixinLocation
使用高德地图仿最新版微信发送位置实现，相似度高达99.99%
##背景
其实程序猿要开发一个demo的背景，都！一！样！
说什么为了社会进步，为了挑战自我，都！是！瞎！扯！蛋！
无非就是一个背景，产品经理要求实现该功能！！！

![生无可恋](https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=1217559312,2326813221&fm=27&gp=0.jpg>)

废话小说，先上gif为敬！

![这是一个gif动图](https://github.com/xiaofuchen/WeixinLocation/blob/master/gif/device-2018-01-14-170609%20%5B320i%5D.gif?raw=true)

##功能
没什么好说的，用上最新版微信，打开“位置”---“发送位置”，萌萌哒，感觉一个样子有木有，相似度99.99%不是梦

还是循例说一下：
* 定位
* 定位数据地图标志并列表显示
* 移动地图获取最新位置信息并显示
* 提供搜索关键字功能
* 一键发送获取具体地址信息经纬度什么的
* ！@#￥%…………&&**（）

## 步骤
###前期准备
去高德地图开发中心，注册账号，申请key，下载jar包！@#￥%……&*（）
（顺便吐槽一下高德的文档看的真让人蛋！痛！）
###GPS定位
* 使用高德定位获取AMapLocation数据
*  将地图使用动态般丝滑移动到定位数据页面显示
*  绘制出GPS定位到的Marker标记
*  使用PoiSearch将定位附近地标信息获取并显示在RecycleView当中去
###移动地图画布
* 为绑定的AMap添加OnCameraChangeListener监听器
*  移动结束后，来一波Marker标记图案动画
*  利用GeocodeSearch将经纬度逆地址编码变成PoiItem的文字信息
*  将PoiItem显示在RecycleView当中去

貌似看起来也不难吧

![这是一只发春的狗](https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1515933244001&di=8eabe670c8934ebd576a236d5d7880d7&imgtype=jpg&src=http%3A%2F%2Fimg1.imgtn.bdimg.com%2Fit%2Fu%3D1157079631%2C3553318991%26fm%3D214%26gp%3D0.jpg)

##代码
直接代码，都有注释，稳！

####定位主页代码
```
package com.weixin.location;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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

    private MapView mMapView;
    private ImageView mIvBack;
    private ImageView mIvSearch;
    private ImageView mIvLocation;
    private ImageView mIvCenterLocation;
    private Button mBtSend;
    private RecyclerView mRecyclerView;
    private AddressAdapter mAddressAdapter;
    private List<PoiItem> mList;
    private PoiItem userSelectPoiItem;

    private AMap mAMap;
    private Marker mMarker, mLocationGpsMarker, mSelectByListMarker;
    private UiSettings mUiSettings;
    private PoiSearch mPoiSearch;
    private PoiSearch.Query mQuery;
    private boolean isSearchData = false;//是否搜索地址数据
    private int searchAllPageNum;//Poi搜索最大页数，可应用于上拉加载更多
    private int searchNowPageNum;//当前poi搜索页数
    private float zoom = 14;//地图缩放级别

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = new AMapLocationClientOption();
    private AMapLocation location;
    private AMapLocationListener mAMapLocationListener;

    private onPoiSearchLintener mOnPoiSearchListener;
    private View.OnClickListener mOnClickListener;
    private GeocodeSearch.OnGeocodeSearchListener mOnGeocodeSearchListener;

    private Gson gson;

    private ObjectAnimator mTransAnimator;//地图中心标志动态

    private static final int SEARCHREQUESTCODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDatas(savedInstanceState);
        initListener();
        startLocation();
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

}

```

####搜索页代码
```
package com.weixin.location;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.google.gson.Gson;
import com.weixin.location.adapter.SearchAddressAdapter;
import com.weixin.location.utils.DatasKey;
import com.weixin.location.utils.OnItemClickLisenter;
import com.weixin.location.utils.SPUtils;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private ImageView mIvBack;
    private EditText mEtSearch;
    private RecyclerView mRecyclerView;
    private List<PoiItem> mList;
    private SearchAddressAdapter mSearchAddressAdapter;

    private PoiSearch mPoiSearch;
    private PoiSearch.Query mQuery;
    private PoiSearch.OnPoiSearchListener mOnPoiSearchListener;

    private View.OnClickListener mOnClickListener;
    private OnItemClickLisenter mOnItemClickLisenter;

    private Gson gson;
    public AMapLocation location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initView();
        initDatas();
        initListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null == location) {
            String s = SPUtils.getString(this, DatasKey.LOCATION_INFO);//获取保存的定位信息
            if (!TextUtils.isEmpty(s)) {
                try {
                    location = gson.fromJson(s, AMapLocation.class);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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
        if (null != mPoiSearch) {
            mPoiSearch = null;
        }
    }

    private void initView() {
        mIvBack = (ImageView) findViewById(R.id.iv_back);
        mEtSearch = (EditText) findViewById(R.id.et_search);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview);
    }

    private void initDatas() {
        mList = new ArrayList<>();
        mSearchAddressAdapter = new SearchAddressAdapter(this, mList);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mSearchAddressAdapter);

        gson = new Gson();
    }

    private void initListener() {

        mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.iv_back:
                        finish();
                        break;
                }
            }
        };
        mIvBack.setOnClickListener(mOnClickListener);

        mEtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (null != editable) {
                    if (0 == editable.length()) {//没有输入则清空搜索记录
                        mList.clear();
                        mSearchAddressAdapter.setList(mList, "");
                    } else {
                        if (null != location) {
                            doSearchQuery(editable.toString(), location.getCity(), new LatLonPoint(location.getLatitude(), location.getLongitude()));
                        } else {
                            doSearchQuery(editable.toString(), "", null);
                        }
                    }
                }
            }
        });

        mOnItemClickLisenter = new OnItemClickLisenter() {
            @Override
            public void onItemClick(int position) {
                PoiItem poiItem = mList.get(position);
                if (null != poiItem) {//获取信息并回传上一页面
                    Intent intent = new Intent();
                    intent.putExtra(DatasKey.SEARCH_INFO, poiItem);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        };
        mSearchAddressAdapter.setOnItemClickLisenter(mOnItemClickLisenter);

        mOnPoiSearchListener = new PoiSearch.OnPoiSearchListener() {
            @Override
            public void onPoiSearched(PoiResult result, int i) {
                if (i == 1000) {
                    if (result != null && result.getQuery() != null) {// 搜索poi的结果
                        if (result.getQuery().equals(mQuery)) {// 是否是同一条
                            if (null != mList) {
                                mList.clear();
                            }
                            mList.addAll(result.getPois());// 取得第一页的poiitem数据，页数从数字0开始
                            if (null != mSearchAddressAdapter) {
                                mSearchAddressAdapter.setList(mList, mEtSearch.getText().toString().trim());
                                mRecyclerView.smoothScrollToPosition(0);
                            }
                        }
                    }
                }
            }

            @Override
            public void onPoiItemSearched(PoiItem poiItem, int i) {

            }
        };

    }

    /**
     * 开始进行poi搜索
     */
    protected void doSearchQuery(String keyWord, String city, LatLonPoint lpTemp) {
        mQuery = new PoiSearch.Query(keyWord, "", city);//第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        mQuery.setPageSize(20);// 设置每页最多返回多少条poiitem
        mQuery.setPageNum(0);// 设置查第一页


        mPoiSearch = new PoiSearch(this, mQuery);
        mPoiSearch.setOnPoiSearchListener(mOnPoiSearchListener);
        if (lpTemp != null) {
            mPoiSearch.setBound(new PoiSearch.SearchBound(lpTemp, 10000, true));//该范围的中心点-----半径，单位：米-----是否按照距离排序
        }
        mPoiSearch.searchPOIAsyn();// 异步搜索
    }

}

```

##结束
更多详情，请前往我的github去撸
[https://github.com/xiaofuchen/WeixinLocation](https://github.com/xiaofuchen/WeixinLocation)

顺手给Stars是中华人民的美德
谢谢
![送美女一枚](http://img.mp.itc.cn/upload/20161102/d678f74eb6c440ec9dc9ebb1d7906cc8.gif)
