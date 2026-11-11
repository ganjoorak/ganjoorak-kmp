package net.ganjoorak.app.data.account

import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.SavedPoem
import net.ganjoorak.app.data.local.FollowedPoet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountDataTest {
    private val hafez = poet(2, "حافظ", "hafez", PoetSource.Ganjoor)
    private val forough = poet(1, "فروغ فرخزاد", "farrokhzad", PoetSource.Local)
    private val directory = PoetDirectory(listOf(hafez, forough))

    @Test
    fun localPoemMapsToCustomKeyAndSafeHref() {
        val saved = SavedPoem(
            id = 12,
            title = "تولدی دیگر",
            poetName = "فروغ فرخزاد",
            excerpt = "همه هستی من آیه تاریکی است",
            source = PoetSource.Local,
            poetSlug = "farrokhzad",
            savedAt = 50,
        )
        val entry = saved.toLibraryEntry(now = 99)
        assertEquals("custom:farrokhzad:12", entry.key)
        assertEquals("custom", entry.source)
        assertEquals("/farrokhzad/12", entry.href)
        assertTrue(isSafeHref(entry.href))
        assertEquals(50, entry.savedAt)
        val back = entry.toSavedPoem()!!
        assertEquals(PoemRef(PoetSource.Local, 12, "farrokhzad"), back.ref)
        assertEquals(50, back.savedAt)
    }

    @Test
    fun ganjoorHrefUsesPoemIdPath() {
        val saved = SavedPoem(2130, "غزل ۱", "حافظ", "الا یا ایها الساقی", PoetSource.Ganjoor, "hafez", savedAt = 10)
        val entry = saved.toLibraryEntry(now = 99)
        assertEquals("ganjoor:hafez:2130", entry.key)
        assertEquals("/poem/2130", entry.href)
    }

    @Test
    fun missingSavedAtIsFilledWhenEncoding() {
        val saved = SavedPoem(1, "عنوان", "حافظ", "بیت", PoetSource.Ganjoor, "hafez", savedAt = 0)
        assertEquals(1_700_000_000_000, saved.toLibraryEntry(now = 1_700_000_000_000).savedAt)
    }

    @Test
    fun unsafeHrefAndMissingFieldsAreDropped() {
        val kept = LibraryEntryDto("ganjoor:hafez:1", 1, href = "/poem/1", savedAt = 20)
        val dropped = listOf(
            LibraryEntryDto("ganjoor:hafez:2", 2, href = "//evil.example/poem/2", savedAt = 30),
            LibraryEntryDto("ganjoor:hafez:3", 3, href = "/poem/3", savedAt = 0),
            LibraryEntryDto("", 4, href = "/poem/4", savedAt = 40),
            LibraryEntryDto("ganjoor:hafez:5", 0, href = "/poem/5", savedAt = 50),
        )
        val clean = sanitizeLibraryEntries(dropped + kept, limit = 10)
        assertEquals(listOf("ganjoor:hafez:1"), clean.map { it.key })
    }

    @Test
    fun sanitizeKeepsNewestCopyOfEachKeyAndCaps() {
        val entries = listOf(
            LibraryEntryDto("ganjoor:hafez:1", 1, href = "/poem/1", savedAt = 10),
            LibraryEntryDto("ganjoor:hafez:2", 2, href = "/poem/2", savedAt = 30),
            LibraryEntryDto("ganjoor:hafez:1", 1, href = "/poem/1", title = "newer", savedAt = 40),
            LibraryEntryDto("ganjoor:hafez:3", 3, href = "/poem/3", savedAt = 20),
        )
        val clean = sanitizeLibraryEntries(entries, limit = 2)
        assertEquals(listOf("ganjoor:hafez:1", "ganjoor:hafez:2"), clean.map { it.key })
        assertEquals("newer", clean.first().title)
    }

    @Test
    fun followedGanjoorIdMapsToWebsiteSlugKey() {
        val followed = FollowedPoet("ganjoor:2", "حافظ", slug = "")
        assertEquals("ganjoor:hafez", directory.preferredKey(followed))
        assertEquals(FollowedPoet("ganjoor:2", "حافظ", "hafez"), directory.toFollowed("ganjoor:hafez"))
    }

    @Test
    fun localFollowedPoetMapsToCustomKeyAndBack() {
        val followed = FollowedPoet("local:farrokhzad", "فروغ فرخزاد", "farrokhzad")
        assertEquals("custom:farrokhzad", directory.preferredKey(followed))
        assertEquals(followed, directory.toFollowed("custom:farrokhzad"))
    }

    @Test
    fun unknownGanjoorSlugStaysUnresolvedSoItIsNotDroppedOnPush() {
        assertNull(directory.toFollowed("ganjoor:saadi"))
        val extras = AccountSyncExtras(username = "hadi", unresolvedPreferredKeys = listOf("ganjoor:saadi"))
        val data = buildAccountData(
            favorites = emptyList(),
            history = emptyList(),
            followed = listOf(FollowedPoet("ganjoor:2", "حافظ")),
            extras = extras,
            directory = directory,
            now = 1,
        )
        assertEquals(listOf("ganjoor:hafez", "ganjoor:saadi"), data.preferredPoetKeys)
        assertTrue(data.followedPoetKeys.isEmpty())
    }

    @Test
    fun mergeApplyCarriesLovedCollectionsAndInterestKeys() {
        val incoming = AccountDataDto(
            favorites = listOf(
                LibraryEntryDto("custom:farrokhzad:12", 12, source = "custom", poetSlug = "farrokhzad", poetName = "فروغ", title = "تولدی دیگر", href = "/farrokhzad/12", savedAt = 8),
                LibraryEntryDto("ganjoor:hafez:2130", 2130, poetSlug = "hafez", poetName = "حافظ", title = "غزل ۱", href = "/poem/2130", savedAt = 9),
            ),
            history = listOf(
                LibraryEntryDto("ganjoor:hafez:2130", 2130, poetSlug = "hafez", poetName = "حافظ", title = "غزل ۱", href = "/poem/2130", savedAt = 11),
            ),
            lovedCollections = listOf(
                CollectionEntryDto("ganjoor:hafez:cat:24", source = "ganjoor", categoryId = 24, poetSlug = "hafez", poetName = "حافظ", title = "غزلیات", href = "/hafez/cat/24", savedAt = 5),
            ),
            preferredPoetKeys = listOf("ganjoor:hafez", "custom:farrokhzad"),
            interestKeys = listOf("ghazal"),
            followedPoetKeys = listOf("ganjoor:saadi"),
        )
        val applied = applyAccountData(incoming, directory, "hadi")
        assertEquals(listOf("ganjoor:hafez:2130", "custom:farrokhzad:12"), applied.favorites.map { poemLibraryKey(it.ref) })
        assertEquals(PoetSource.Local, applied.favorites.single { it.poetSlug == "farrokhzad" }.source)
        assertEquals(listOf("ganjoor:2", "local:farrokhzad"), applied.followed.map { it.key })
        assertEquals(listOf("ganjoor:saadi"), applied.extras.unresolvedPreferredKeys)
        assertEquals("غزلیات", applied.extras.lovedCollections.single().title)
        assertEquals(listOf("ghazal"), applied.extras.interestKeys)
        assertEquals("hadi", applied.extras.username)

        val pushed = buildAccountData(
            favorites = applied.favorites,
            history = applied.history,
            followed = applied.followed,
            extras = applied.extras,
            directory = directory,
            now = 99,
        )
        assertEquals(listOf("ganjoor:hafez:cat:24"), pushed.lovedCollections.map { it.key })
        assertEquals(listOf("ghazal"), pushed.interestKeys)
        assertEquals(listOf("ganjoor:hafez", "custom:farrokhzad", "ganjoor:saadi"), pushed.preferredPoetKeys)
    }

    @Test
    fun legacyFollowedPoetKeysAreFoldedIn() {
        val data = AccountDataDto(followedPoetKeys = listOf("ganjoor:hafez"))
        assertEquals(listOf("ganjoor:hafez"), data.allPreferredPoetKeys)
    }

    private fun poet(id: Int, name: String, slug: String, source: PoetSource) = Poet(
        id = id,
        name = name,
        slug = slug,
        nickname = null,
        description = null,
        imageUrl = "",
        rootCatId = 0,
        birthYear = null,
        deathYear = null,
        birthPlace = null,
        deathPlace = null,
        source = source,
    )
}
