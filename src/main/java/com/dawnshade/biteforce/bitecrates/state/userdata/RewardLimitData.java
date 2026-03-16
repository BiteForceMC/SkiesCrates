package com.dawnshade.biteforce.bitecrates.state.userdata;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class RewardLimitData {
    @BsonProperty
    public Integer claimed = 0;
    @BsonProperty
    public Long time = 0L;

    public RewardLimitData(Integer claimed, Long time) {
        this.claimed = claimed;
        this.time = time;
    }

    public RewardLimitData() {}

    @Override
    public String toString() {
        return "RewardLimitData{" +
                "claimed=" + claimed +
                ", time=" + time +
                '}';
    }
}
