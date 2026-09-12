package com.donnie1337.clanplus;

public enum ClanRole {
    LEADER,
    MODERATOR,
    MEMBER;

    public boolean canManage() {
        return this == LEADER || this == MODERATOR;
    }

    public boolean canPromote() {
        return this == LEADER;
    }
}
