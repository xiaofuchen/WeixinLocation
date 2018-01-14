package com.weixin.location.utils;

import android.text.TextUtils;

import com.amap.api.location.AMapLocation;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.RegeocodeResult;

/**
 * Created by XiaoFu on 2017-08-03 11:35.
 * 注释：数据转换类
 */

public class DataConversionUtils {

    public static AMapLocation changeToAMapLocation(PoiItem poiItem) {
        if (null != poiItem) {
            try {
                AMapLocation aMapLocation = new AMapLocation("lbs");
                aMapLocation.setAdCode(poiItem.getAdCode());
                aMapLocation.setAddress(poiItem.getProvinceName() + poiItem.getCityName() + poiItem.getAdName() + poiItem.getSnippet());
                aMapLocation.setCity(poiItem.getCityName());
                aMapLocation.setCityCode(poiItem.getCityCode());
                aMapLocation.setDistrict(poiItem.getAdName());
                aMapLocation.setLatitude(poiItem.getLatLonPoint().getLatitude());
                aMapLocation.setLongitude(poiItem.getLatLonPoint().getLongitude());
                aMapLocation.setPoiName(poiItem.getTitle());
                aMapLocation.setProvince(poiItem.getProvinceName());
                aMapLocation.setStreet(poiItem.getBusinessArea());
                return aMapLocation;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static PoiItem changeToPoiItem(AMapLocation data) {
        if (null != data) {
            try {
                String title = data.getDescription();
                if (TextUtils.isEmpty(title)) {
                    title = data.getPoiName();
                }
                if (TextUtils.isEmpty(title)) {
                    title = data.getStreet();
                }
                if (TextUtils.isEmpty(title)) {
                    title = "[位置]";
                }

                PoiItem poiItem = new PoiItem(data.getBuildingId(), new LatLonPoint(data.getLatitude(), data.getLongitude()), title, data.getAddress());

                poiItem.setAdCode(data.getAdCode());
                poiItem.setAdName(data.getDistrict());
                poiItem.setBusinessArea(data.getStreet());
                poiItem.setCityCode(data.getCityCode());
                poiItem.setCityName(data.getCity());
                poiItem.setProvinceName(data.getProvince());

                return poiItem;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static PoiItem changeToPoiItem(RegeocodeResult data) {
        if (null != data) {
            try {
                String title = data.getRegeocodeAddress().getBuilding();
                if (TextUtils.isEmpty(title)) {
                    title = data.getRegeocodeAddress().getNeighborhood();
                }
                if (TextUtils.isEmpty(title)) {
                    title = data.getRegeocodeAddress().getTownship();
                }
                if (TextUtils.isEmpty(title)) {
                    title = "[位置]";
                }
                PoiItem poiItem = new PoiItem(data.getRegeocodeAddress().getBuilding(), data.getRegeocodeQuery().getPoint(), title, data.getRegeocodeAddress().getFormatAddress());

                poiItem.setAdCode(data.getRegeocodeAddress().getAdCode());
                poiItem.setAdName(data.getRegeocodeAddress().getDistrict());
                poiItem.setBusinessArea(data.getRegeocodeAddress().getBusinessAreas().get(0).getName());
                poiItem.setCityCode(data.getRegeocodeAddress().getCityCode());
                poiItem.setCityName(data.getRegeocodeAddress().getCity());
                poiItem.setProvinceName(data.getRegeocodeAddress().getProvince());

                return poiItem;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
