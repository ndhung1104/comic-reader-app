package com.group09.ComicReader.model;

public class VipStatusResponse {
    private boolean vip;
    private String plan;
    private String startDate;
    private String endDate;
    private String status;

    public boolean isVip() {
        return vip;
    }

    public String getPlan() {
        return plan;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }
}
