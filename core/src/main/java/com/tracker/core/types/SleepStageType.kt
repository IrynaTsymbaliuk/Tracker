package com.tracker.core.types

/**
 * A stage within a sleep session, mirroring Health Connect's `SleepSessionRecord.STAGE_TYPE_*`
 * constants.
 *
 * Not every source populates stages. Many sleep trackers write only [AWAKE] and a generic
 * [SLEEPING] stage; some write nothing at all (a bare session with only start/end times). Apps
 * that want stage-level detail should treat [DEEP]/[REM]/[LIGHT] as best-effort.
 *
 * @property isAsleep Whether time in this stage counts as actual sleep. [SLEEPING], [LIGHT],
 * [DEEP], and [REM] are asleep; [AWAKE], [AWAKE_IN_BED], [OUT_OF_BED], and [UNKNOWN] are not.
 */
enum class SleepStageType(val isAsleep: Boolean) {
    UNKNOWN(false),
    AWAKE(false),
    AWAKE_IN_BED(false),
    OUT_OF_BED(false),
    SLEEPING(true),
    LIGHT(true),
    DEEP(true),
    REM(true)
}
