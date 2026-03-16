package com.dawnshade.biteforce.bitecrates.feature.key

enum class KeyDuplicateAlert(val message: String) {
    MISSING_UUID("Player %player% (%player_uuid%) used a unique key (%key_id%) that has an empty unique id"),
    STACKED("Player %player% (%player_uuid%) used a unique key (%key_id%) that is stacked %amount%x: %uuid%"),
    INVALID_UUID("Player %player% (%player_uuid%) used a unique key (%key_id%) that contains an invalid unique id: %uuid%"),
    ALREADY_USED("Player %player% (%player_uuid%) used a unique key (%key_id%) that has an already used unique id: %uuid%");
}