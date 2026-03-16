package com.dawnshade.biteforce.bitecrates.state.userdata;

import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.HashMap;
import java.util.Map;

public class CrateData {
    @BsonProperty
    public Long lastOpen;
    @BsonProperty
    public Integer openCount;
    @BsonProperty
    public Map<String, RewardLimitData> rewards;

    public CrateData(Long lastOpen, Integer openCount, Map<String, RewardLimitData> rewards) {
        this.lastOpen = lastOpen;
        this.openCount = openCount;
        this.rewards = rewards;
    }

    public CrateData() {}

    public Map<String, RewardLimitData> getRewards() {
        if (rewards == null) {
            rewards = new HashMap<>();
        }

        return rewards;
    }

    @Override
    public String toString() {
        return "CrateData{" +
                "lastOpen=" + lastOpen +
                ", openCount=" + openCount +
                ", rewards=" + rewards +
                '}';
    }
}
