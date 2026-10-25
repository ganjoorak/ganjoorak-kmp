package net.ganjoorak.app.data.remote

import kotlinx.serialization.Serializable

// Wire models for api.ganjoor.net. Every field is optional because the API
// omits or nulls fields depending on the query flags.

@Serializable
data class PoetDto(
    val id: Int = 0,
    val name: String? = null,
    val description: String? = null,
    val fullUrl: String? = null,
    val rootCatId: Int = 0,
    val nickname: String? = null,
    val published: Boolean = true,
    val imageUrl: String? = null,
    val birthYearInLHijri: Int? = null,
    val validBirthDate: Boolean = false,
    val deathYearInLHijri: Int? = null,
    val validDeathDate: Boolean = false,
    val pinOrder: Int = 0,
    val birthPlace: String? = null,
    val deathPlace: String? = null,
)

@Serializable
data class CenturyDto(
    val id: Int = 0,
    val name: String? = null,
    val halfCenturyOrder: Int = 0,
    val startYear: Int = 0,
    val endYear: Int = 0,
    val showInTimeLine: Boolean = true,
    val poets: List<PoetDto> = emptyList(),
)

@Serializable
data class PoemSummaryDto(
    val id: Int = 0,
    val title: String? = null,
    val urlSlug: String? = null,
    val excerpt: String? = null,
)

@Serializable
data class CatDto(
    val id: Int = 0,
    val title: String? = null,
    val urlSlug: String? = null,
    val fullUrl: String? = null,
    val description: String? = null,
    val descriptionHtml: String? = null,
    val children: List<CatDto>? = null,
    val poems: List<PoemSummaryDto>? = null,
    val ancestors: List<CatDto>? = null,
    val next: CatDto? = null,
    val previous: CatDto? = null,
)

@Serializable
data class PoetCompleteDto(
    val poet: PoetDto? = null,
    val cat: CatDto? = null,
)

@Serializable
data class VerseDto(
    val id: Int = 0,
    val vOrder: Int = 0,
    val coupletIndex: Int = 0,
    val versePosition: Int = 0,
    val text: String? = null,
)

@Serializable
data class RecitationDto(
    val id: Int = 0,
    val audioTitle: String? = null,
    val audioArtist: String? = null,
    val mp3Url: String? = null,
    val audioSrc: String? = null,
    val inSyncWithText: Boolean = false,
    val audioOrder: Int = 0,
)

@Serializable
data class PoemDto(
    val id: Int = 0,
    val title: String? = null,
    val fullTitle: String? = null,
    val urlSlug: String? = null,
    val fullUrl: String? = null,
    val plainText: String? = null,
    val htmlText: String? = null,
    val poemSummary: String? = null,
    val category: PoetCompleteDto? = null,
    val next: PoemSummaryDto? = null,
    val previous: PoemSummaryDto? = null,
    val verses: List<VerseDto>? = null,
    val recitations: List<RecitationDto>? = null,
)

@Serializable
data class VerseSyncDto(
    val verseOrder: Int = 0,
    val verseText: String? = null,
    val audioStartMilliseconds: Long = 0,
)

@Serializable
data class PagingHeaderDto(
    val totalCount: Int = 0,
    val pageSize: Int = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val hasPreviousPage: Boolean? = null,
    val hasNextPage: Boolean? = null,
)

@Serializable
data class SemanticSearchRequest(
    val query: String,
    val topK: Int,
    val disableScopeDetection: Boolean = false,
)

@Serializable
data class SemanticVerseDto(
    val vOrder: Int = 0,
    val text: String? = null,
)

@Serializable
data class SemanticResultDto(
    val poemId: Int = 0,
    val id: Int = 0,
    val title: String? = null,
    val fullTitle: String? = null,
    val fullUrl: String? = null,
    val poemSummary: String? = null,
    val verses: List<SemanticVerseDto> = emptyList(),
    val score: Double = 0.0,
)

@Serializable
data class SemanticSearchResponseDto(
    val query: String? = null,
    val results: List<SemanticResultDto> = emptyList(),
    val detectedPoetName: String? = null,
)
