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
