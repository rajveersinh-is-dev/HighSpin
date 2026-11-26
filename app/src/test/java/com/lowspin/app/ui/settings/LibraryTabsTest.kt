package com.lowspin.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryTabsTest {

    private val defaultTabs = listOf("songs", "albums", "artists", "folders", "playlists")

    @Test
    fun libraryTabs_serializationRoundTrip() {
        val serialized = defaultTabs.joinToString(",")
        assertEquals("songs,albums,artists,folders,playlists", serialized)

        val deserialized = serialized.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        assertEquals(defaultTabs, deserialized)
    }

    @Test
    fun libraryTabs_filteringHiddenTabs() {
        val hiddenTabs = setOf("folders", "artists")
        val visibleTabs = defaultTabs.filter { it !in hiddenTabs }

        assertEquals(listOf("songs", "albums", "playlists"), visibleTabs)
        assertFalse(visibleTabs.contains("folders"))
        assertFalse(visibleTabs.contains("artists"))
    }

    @Test
    fun libraryTabs_reorderingLogic() {
        val mutable = defaultTabs.toMutableList()
        // Move "playlists" (index 4) up to index 3
        val item = mutable.removeAt(4)
        mutable.add(3, item)

        assertEquals(listOf("songs", "albums", "artists", "playlists", "folders"), mutable)
    }

    @Test
    fun libraryTabs_defaultTabFallbackWhenHidden() {
        val visibleTabs = listOf("albums", "artists", "playlists")
        val preferredDefault = "songs" // "songs" is hidden

        val actualDefault = if (preferredDefault in visibleTabs) {
            preferredDefault
        } else {
            visibleTabs.firstOrNull() ?: "songs"
        }

        assertEquals("albums", actualDefault)
    }

    @Test
    fun libraryTabs_emptyStringDeserializationReturnsEmpty() {
        val emptyString = ""
        val result = emptyString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(result.isEmpty())
    }
}
