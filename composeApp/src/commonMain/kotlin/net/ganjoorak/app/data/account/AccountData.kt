package net.ganjoorak.app.data.account

import kotlinx.serialization.Serializable
import net.ganjoorak.app.data.PoemRef
import net.ganjoorak.app.data.Poet
import net.ganjoorak.app.data.PoetSource
import net.ganjoorak.app.data.SavedPoem
import net.ganjoorak.app.data.local.FollowedPoet

/** Caps and key rules match the website's Go accounts service. */
const val MAX_FAVORITES = 200
const val MAX_HISTORY = 30
const val MAX_COLLECTIONS = 100
const val MAX_PREFERRED_POETS = 500
const val MAX_INTERESTS = 50

private val PREFERRED_POET_KEY = Regex("^[a-z]+:[\\p{L}\\p{N}_.\\-]+$")
private val INTEREST_KEY = Regex("^[a-z]{1,32}$")

@Serializable
data class LibraryEntryDto(
    val key: String,
    val id: Long,
    val source: String = "ganjoor",
    val poetSlug: String = "",
    val poetName: String = "",
    val title: String = "",
    val excerpt: String = "",
    val href: String = "",
    val savedAt: Long = 0,
)

@Serializable
data class CollectionEntryDto(
    val key: String,
    val source: String = "ganjoor",
    val categoryId: Long = 0,
    val poetSlug: String = "",
    val poetName: String = "",
    val title: String = "",
    val href: String = "",
    val savedAt: Long = 0,
)

/**
 * Account library as stored by PUT /api/account/data.
 *
 * [followedPoetKeys] is only read: older rows and clients used that name.
 * New writes go out as [preferredPoetKeys].
 */
@Serializable
data class AccountDataDto(
    val favorites: List<LibraryEntryDto> = emptyList(),
    val history: List<LibraryEntryDto> = emptyList(),
    val lovedCollections: List<CollectionEntryDto> = emptyList(),
    val preferredPoetKeys: List<String> = emptyList(),
    val interestKeys: List<String> = emptyList(),
    val followedPoetKeys: List<String> = emptyList(),
) {
    /** Preferred poets, with the legacy field folded in the way the server still does. */
    val allPreferredPoetKeys: List<String>
        get() = (preferredPoetKeys + followedPoetKeys).distinct()
}

/**
 * Fields the app has no UI for yet, plus preferred-poet keys we could not
 * resolve to a local [FollowedPoet]. They are sent back on every replace-push
 * so a later [merge] equals false does not wipe the account.
 */
@Serializable
data class AccountSyncExtras(
    val username: String = "",
    val lovedCollections: List<CollectionEntryDto> = emptyList(),
    val interestKeys: List<String> = emptyList(),
    val unresolvedPreferredKeys: List<String> = emptyList(),
)

data class AppliedAccountData(
    val favorites: List<SavedPoem>,
    val history: List<SavedPoem>,
    val followed: List<FollowedPoet>,
    val extras: AccountSyncExtras,
)

/** Looks up poets so app keys (`ganjoor:<id>`, `local:<slug>`) can become website `source:slug` keys. */
class PoetDirectory(poets: List<Poet>) {
    private val byAppKey = poets.associateBy { it.key }
    private val byPreferredKey = poets.associateBy { it.preferredPoetKey() }

    fun preferredKey(followed: FollowedPoet): String? {
        val slug = followed.slug.ifBlank {
            val parsed = parseAppPoetKey(followed.key) ?: return null
            when (parsed.source) {
                PoetSource.Ganjoor -> byAppKey[followed.key]?.slug.orEmpty()
                PoetSource.Echolalia, PoetSource.Local -> parsed.ref
            }
        }
        if (slug.isBlank()) return null
        val source = websiteSource(parseAppPoetKey(followed.key)?.source ?: return null)
        return preferredPoetKey(source, slug)
    }

    fun toFollowed(preferredKey: String): FollowedPoet? {
        byPreferredKey[preferredKey]?.let { poet ->
            return FollowedPoet(poet.key, poet.name, poet.slug)
        }
        val parsed = parsePreferredPoetKey(preferredKey) ?: return null
        // Ganjoor feed lookup still needs a numeric id; without a directory hit we keep the key aside.
        if (parsed.source == "ganjoor") return null
        val source = appSource(parsed.source)
        val key = net.ganjoorak.app.data.PoetKey(source, parsed.slug).value
        val name = byAppKey[key]?.name ?: parsed.slug
        return FollowedPoet(key, name, parsed.slug)
    }
}

fun Poet.preferredPoetKey(): String =
    preferredPoetKey(websiteSource(source), slug) ?: "${websiteSource(source)}:$slug"

fun websiteSource(source: PoetSource): String = when (source) {
    PoetSource.Ganjoor -> "ganjoor"
    PoetSource.Echolalia -> "echolalia"
    PoetSource.Local -> "custom"
}

fun appSource(website: String): PoetSource = when (website) {
    "echolalia" -> PoetSource.Echolalia
    "custom" -> PoetSource.Local
    else -> PoetSource.Ganjoor
}

fun preferredPoetKey(source: String, slug: String): String? {
    val clean = slug.trim().trim('/')
    if (clean.isEmpty()) return null
    val key = "${source.ifBlank { "ganjoor" }}:$clean"
    return key.takeIf { PREFERRED_POET_KEY.matches(it) && it.length <= 120 }
}

fun parsePreferredPoetKey(key: String): PreferredPoetRef? {
    val separator = key.indexOf(':')
    val source = if (separator >= 0) key.take(separator).ifBlank { "ganjoor" } else "ganjoor"
    val slug = if (separator >= 0) key.substring(separator + 1) else key
    if (slug.isEmpty()) return null
    val normalized = when (source) {
        "custom", "echolalia", "ganjoor" -> source
        else -> "ganjoor"
    }
    return PreferredPoetRef(normalized, slug)
}

data class PreferredPoetRef(val source: String, val slug: String)

private fun parseAppPoetKey(key: String) = net.ganjoorak.app.data.PoetKey.parse(key)

fun poemHref(ref: PoemRef): String = when (ref.source) {
    PoetSource.Ganjoor -> "/poem/${ref.id}"
    else -> "/${ref.poetSlug.ifBlank { "unknown" }}/${ref.id}"
}

fun isSafeHref(href: String): Boolean {
    val value = href.trim()
    return value.startsWith("/") && !value.startsWith("//")
}

fun SavedPoem.toLibraryEntry(now: Long): LibraryEntryDto = LibraryEntryDto(
    key = poemLibraryKey(ref),
    id = id.toLong(),
    source = websiteSource(source),
    poetSlug = poetSlug,
    poetName = poetName,
    title = title,
    excerpt = excerpt,
    href = poemHref(ref),
    savedAt = savedAt.takeIf { it > 0 } ?: now,
)

fun LibraryEntryDto.toSavedPoem(): SavedPoem? {
    val ref = parsePoemLibraryKey(key) ?: run {
        if (id < 1) return null
        val slug = poetSlug.ifBlank { return null }
        PoemRef(appSource(source), id.toInt(), slug)
    }
    return SavedPoem(
        id = ref.id,
        title = title,
        poetName = poetName,
        excerpt = excerpt,
        source = ref.source,
        poetSlug = ref.poetSlug,
        savedAt = savedAt,
    )
}

fun sanitizeLibraryEntries(entries: List<LibraryEntryDto>, limit: Int): List<LibraryEntryDto> {
    val clean = entries.mapNotNull { entry ->
        val source = if (entry.source in setOf("ganjoor", "custom", "echolalia")) entry.source else "ganjoor"
        val href = entry.href.trim()
        if (entry.key.isBlank() || entry.id <= 0 || entry.savedAt <= 0 || !isSafeHref(href)) {
            null
        } else {
            entry.copy(source = source, href = href)
        }
    }
    return clean.sortedByDescending { it.savedAt }.distinctBy { it.key }.take(limit)
}

fun sanitizePreferredPoetKeys(keys: List<String>): List<String> {
    val out = ArrayList<String>(keys.size.coerceAtMost(MAX_PREFERRED_POETS))
    val seen = HashSet<String>()
    for (key in keys) {
        if (key.length > 120 || !PREFERRED_POET_KEY.matches(key) || !seen.add(key)) continue
        out += key
        if (out.size == MAX_PREFERRED_POETS) break
    }
    return out
}

fun sanitizeInterestKeys(keys: List<String>): List<String> {
    val out = ArrayList<String>(keys.size.coerceAtMost(MAX_INTERESTS))
    val seen = HashSet<String>()
    for (key in keys) {
        if (!INTEREST_KEY.matches(key) || !seen.add(key)) continue
        out += key
        if (out.size == MAX_INTERESTS) break
    }
    return out
}

fun sanitizeCollections(entries: List<CollectionEntryDto>): List<CollectionEntryDto> {
    val clean = entries.filter { entry ->
        entry.key.isNotBlank() && entry.title.isNotBlank() && entry.savedAt > 0 && isSafeHref(entry.href)
    }
    return clean.sortedByDescending { it.savedAt }.distinctBy { it.key }.take(MAX_COLLECTIONS)
}

fun buildAccountData(
    favorites: List<SavedPoem>,
    history: List<SavedPoem>,
    followed: List<FollowedPoet>,
    extras: AccountSyncExtras,
    directory: PoetDirectory,
    now: Long,
): AccountDataDto {
    val preferred = sanitizePreferredPoetKeys(
        followed.mapNotNull { directory.preferredKey(it) } + extras.unresolvedPreferredKeys,
    )
    return AccountDataDto(
        favorites = sanitizeLibraryEntries(favorites.map { it.toLibraryEntry(now) }, MAX_FAVORITES),
        history = sanitizeLibraryEntries(history.map { it.toLibraryEntry(now) }, MAX_HISTORY),
        lovedCollections = sanitizeCollections(extras.lovedCollections),
        preferredPoetKeys = preferred,
        interestKeys = sanitizeInterestKeys(extras.interestKeys),
    )
}

fun applyAccountData(
    data: AccountDataDto,
    directory: PoetDirectory,
    username: String,
): AppliedAccountData {
    val followed = ArrayList<FollowedPoet>()
    val unresolved = ArrayList<String>()
    val seenKeys = HashSet<String>()
    for (key in sanitizePreferredPoetKeys(data.allPreferredPoetKeys)) {
        val poet = directory.toFollowed(key)
        if (poet != null) {
            if (seenKeys.add(poet.key)) followed += poet
        } else {
            unresolved += key
        }
    }
    return AppliedAccountData(
        favorites = sanitizeLibraryEntries(data.favorites, MAX_FAVORITES).mapNotNull { it.toSavedPoem() },
        history = sanitizeLibraryEntries(data.history, MAX_HISTORY).mapNotNull { it.toSavedPoem() },
        followed = followed,
        extras = AccountSyncExtras(
            username = username,
            lovedCollections = sanitizeCollections(data.lovedCollections),
            interestKeys = sanitizeInterestKeys(data.interestKeys),
            unresolvedPreferredKeys = unresolved,
        ),
    )
}
