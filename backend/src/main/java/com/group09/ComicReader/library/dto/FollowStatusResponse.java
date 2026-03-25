package com.group09.ComicReader.library.dto;

public class FollowStatusResponse {

    private boolean followed;

    public FollowStatusResponse() {
    }

    public FollowStatusResponse(boolean followed) {
        this.followed = followed;
    }

    public boolean isFollowed() {
        return followed;
    }

    public void setFollowed(boolean followed) {
        this.followed = followed;
    }
}
