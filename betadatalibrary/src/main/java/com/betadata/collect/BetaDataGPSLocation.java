
package com.betadata.collect;

public class BetaDataGPSLocation {
    /**
     * 纬度
     */
    private double latitude;

    /**
     * 经度
     */
    private double longitude;
    /**
     * 国家
     */
    private String country;
    /**
     * 省份
     */
    private String province;
    /**
     * 城市
     */
    private String city;
    /**
     * 详细地址
     */
    private String housing_estate;


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getHousing_estate() {
        return housing_estate;
    }

    public void setHousing_estate(String housing_estate) {
        this.housing_estate = housing_estate;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
