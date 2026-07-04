package com.tracker.core.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round-trip and edge-case coverage for the type-safe metadata bags that ride inside
 * `Evidence.metadata: Map<String, Any>`.
 */
class MetadataMappingTest {

    // --- UsageStatsMetadata ---

    @Test
    fun usageStats_roundTripsThroughMap() {
        val original = UsageStatsMetadata(packageName = "com.calm.android", appName = "Calm")

        val restored = UsageStatsMetadata.fromMap(original.toMap())

        assertEquals(original, restored)
    }

    @Test
    fun usageStats_missingKey_returnsNull() {
        assertNull(UsageStatsMetadata.fromMap(mapOf(UsageStatsMetadata.KEY_PACKAGE_NAME to "com.x")))
    }

    @Test
    fun usageStats_wrongValueType_returnsNull() {
        val map = mapOf(
            UsageStatsMetadata.KEY_PACKAGE_NAME to 42,     // not a String
            UsageStatsMetadata.KEY_APP_NAME to "Name"
        )
        assertNull(UsageStatsMetadata.fromMap(map))
    }

    // --- ExerciseMetadata ---

    @Test
    fun exercise_roundTripsThroughMap() {
        val original = ExerciseMetadata(exerciseTypeId = 56, exerciseType = "running")

        val restored = ExerciseMetadata.fromMap(original.toMap())

        assertEquals(original, restored)
    }

    @Test
    fun exercise_missingTypeId_returnsNull() {
        assertNull(ExerciseMetadata.fromMap(mapOf(ExerciseMetadata.KEY_EXERCISE_TYPE to "running")))
    }

    @Test
    fun exercise_wrongTypeIdType_returnsNull() {
        val map = mapOf(
            ExerciseMetadata.KEY_EXERCISE_TYPE_ID to "56", // String, not Int
            ExerciseMetadata.KEY_EXERCISE_TYPE to "running"
        )
        assertNull(ExerciseMetadata.fromMap(map))
    }

    // --- LetterboxdMetadata ---

    @Test
    fun letterboxd_roundTripsWithAllFields() {
        val original = LetterboxdMetadata(
            title = "The Matrix",
            year = 1999,
            publishedDate = 1_700_000_000_000L,
            watchedDate = 1_699_900_000_000L,
            tmdbId = 603,
            rating = 4.5f,
            review = "Great film",
            posterUrl = "https://example.com/poster.jpg",
            isRewatch = true,
            isLiked = true
        )

        val restored = LetterboxdMetadata.fromMap(original.toMap())

        assertEquals(original, restored)
    }

    @Test
    fun letterboxd_optionalNullsAreOmittedFromMapAndRestoredAsNull() {
        val original = LetterboxdMetadata(
            title = "Untitled",
            year = null,
            publishedDate = 1L,
            watchedDate = 2L,
            tmdbId = null,
            rating = null,
            review = null,
            posterUrl = null
        )

        val map = original.toMap()

        // Optional fields should not be present in the map at all.
        assertFalse(map.containsKey(LetterboxdMetadata.KEY_YEAR))
        assertFalse(map.containsKey(LetterboxdMetadata.KEY_TMDB_ID))
        assertFalse(map.containsKey(LetterboxdMetadata.KEY_RATING))
        assertFalse(map.containsKey(LetterboxdMetadata.KEY_REVIEW))
        assertFalse(map.containsKey(LetterboxdMetadata.KEY_POSTER_URL))

        assertEquals(original, LetterboxdMetadata.fromMap(map))
    }

    @Test
    fun letterboxd_missingRequiredField_returnsNull() {
        // publishedDate missing
        val map = mapOf<String, Any>(
            LetterboxdMetadata.KEY_TITLE to "X",
            LetterboxdMetadata.KEY_WATCHED_DATE to 2L
        )
        assertNull(LetterboxdMetadata.fromMap(map))
    }

    @Test
    fun letterboxd_flagsDefaultToFalseWhenAbsent() {
        val map = mapOf<String, Any>(
            LetterboxdMetadata.KEY_TITLE to "X",
            LetterboxdMetadata.KEY_PUBLISHED_DATE to 1L,
            LetterboxdMetadata.KEY_WATCHED_DATE to 2L
        )

        val restored = LetterboxdMetadata.fromMap(map)!!

        assertFalse(restored.isRewatch)
        assertFalse(restored.isLiked)
    }

    @Test
    fun letterboxd_flagsRestoredWhenPresent() {
        val map = LetterboxdMetadata(
            title = "X", publishedDate = 1L, watchedDate = 2L, isRewatch = true, isLiked = true
        ).toMap()

        val restored = LetterboxdMetadata.fromMap(map)!!

        assertTrue(restored.isRewatch)
        assertTrue(restored.isLiked)
    }
}
