package com.betadata.collect.bean;

/**
 * Author: 李巷阳
 * Date: 2019/4/17
 * Version: V2.0.0
 * Part:
 * Description:
 */
public class SendReturnMessageBean {


    /**
     * success : true
     * data : {"id":8,"track_id":"2019080317075810154515"}
     * timestamp : 1564823278
     */

    private boolean success;
    private DataBean data;
    private int timestamp;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public DataBean getData() {
        return data;
    }

    public void setData(DataBean data) {
        this.data = data;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public static class DataBean {
        /**
         * id : 8
         * track_id : 2019080317075810154515
         */

        private int id;
        private String track_id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTrack_id() {
            return track_id;
        }

        public void setTrack_id(String track_id) {
            this.track_id = track_id;
        }

        @Override
        public String toString() {
            return "DataBean{" +
                    "id=" + id +
                    ", track_id='" + track_id + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SendReturnMessageBean{" +
                "success=" + success +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}
