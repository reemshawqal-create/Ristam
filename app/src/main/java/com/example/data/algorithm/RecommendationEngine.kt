package com.example.data.algorithm

import com.example.data.local.PostEntity
import com.example.data.model.AlgorithmWeights
import com.example.data.model.MediaType
import com.example.data.model.RankedPost
import com.example.data.model.SearchFilterType
import kotlin.math.exp

/**
 * 100% Deterministic, Non-AI Content Recommendation & Local Smart Search Engine.
 * Zero machine learning, zero cloud profiling — purely transparent mathematical scoring
 * controlled manually by the user's sliders and local interactions.
 */
object RecommendationEngine {

    fun rankPosts(
        allPosts: List<PostEntity>,
        weights: AlgorithmWeights,
        blockedTags: List<String>
    ): List<RankedPost> {
        val blockedSet = blockedTags.map { it.lowercase().removePrefix("#") }.toSet()

        // 1. Build manual tag affinity & creator affinity from liked/bookmarked posts
        val tagAffinity = mutableMapOf<String, Double>()
        val creatorAffinity = mutableMapOf<String, Double>()

        for (post in allPosts) {
            val weightBoost = when {
                post.isLiked && post.isBookmarked -> 2.5
                post.isBookmarked -> 1.8
                post.isLiked -> 1.2
                else -> 0.0
            }
            if (weightBoost > 0.0) {
                creatorAffinity[post.authorHandle] = (creatorAffinity[post.authorHandle] ?: 0.0) + weightBoost
                parseTags(post.tagsCsv).forEach { tag ->
                    tagAffinity[tag] = (tagAffinity[tag] ?: 0.0) + weightBoost
                }
            }
        }

        val maxTagAffinity = tagAffinity.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val maxCreatorAffinity = creatorAffinity.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val now = System.currentTimeMillis()

        return allPosts
            .filter { post ->
                val tags = parseTags(post.tagsCsv)
                tags.none { it in blockedSet }
            }
            .map { post ->
                val reasons = mutableListOf<String>()
                val tags = parseTags(post.tagsCsv)

                // Recency decay (half-life ~24h)
                val ageHours = ((now - post.createdAt).coerceAtLeast(0L)) / 3_600_000.0
                val recencyScore = exp(-ageHours / 24.0)

                // Tag matching
                val matchedTags = tags.filter { (tagAffinity[it] ?: 0.0) > 0.0 }
                val rawTagScore = matchedTags.sumOf { tagAffinity[it] ?: 0.0 } / maxTagAffinity
                val tagScore = rawTagScore.coerceIn(0.0, 1.0)
                if (matchedTags.isNotEmpty()) {
                    reasons.add("تطابق وسوم: ${matchedTags.take(2).joinToString(" • ") { "#$it" }}")
                }

                // Creator affinity
                val creatorScore = ((creatorAffinity[post.authorHandle] ?: 0.0) / maxCreatorAffinity).coerceIn(0.0, 1.0)
                if (creatorScore > 0.3) {
                    reasons.add("تفاعل متكرر مع @${post.authorHandle}")
                }

                // Media balance boost
                val videoBoost = if (post.mediaType == MediaType.VIDEO.name) {
                    weights.videoPreferenceBoost.toDouble()
                } else {
                    0.0
                }
                if (videoBoost > 0.15) {
                    reasons.add("أولوية للمقاطع")
                }

                if (post.isPinned) {
                    reasons.add("منشور مثبت يدوياً")
                }

                if (reasons.isEmpty()) {
                    reasons.add("ترتيب زمني حديث")
                }

                val totalScore = (
                    recencyScore * weights.recencyWeight +
                        tagScore * weights.tagAffinityWeight +
                        creatorScore * weights.creatorPriorityWeight +
                        videoBoost +
                        (if (post.isPinned) 0.4 else 0.0)
                    )

                RankedPost(post = post, score = totalScore, matchReasons = reasons)
            }
            .sortedByDescending { it.score }
    }

    fun smartSearch(
        query: String,
        posts: List<PostEntity>,
        filterType: SearchFilterType,
        blockedTags: List<String>
    ): List<PostEntity> {
        val blockedSet = blockedTags.map { it.lowercase().removePrefix("#") }.toSet()
        val typeFiltered = posts.filter { post ->
            val tags = parseTags(post.tagsCsv)
            if (tags.any { it in blockedSet }) return@filter false
            when (filterType) {
                SearchFilterType.ALL -> true
                SearchFilterType.PHOTOS -> post.mediaType == MediaType.PHOTO.name
                SearchFilterType.VIDEOS -> post.mediaType == MediaType.VIDEO.name
                SearchFilterType.TAGS -> post.tagsCsv.isNotBlank()
                SearchFilterType.ACCOUNTS -> true
            }
        }

        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return typeFiltered

        val tokens = cleanQuery
            .split(" ", "،", ",")
            .map { it.trim().removePrefix("#").removePrefix("@") }
            .filter { it.isNotEmpty() }

        return typeFiltered.mapNotNull { post ->
            var relevance = 0
            val captionLower = post.caption.lowercase()
            val handleLower = post.authorHandle.lowercase()
            val nameLower = post.authorDisplayName.lowercase()
            val locationLower = post.location.lowercase()
            val tags = parseTags(post.tagsCsv)

            for (token in tokens) {
                when (filterType) {
                    SearchFilterType.TAGS -> {
                        if (tags.any { it.contains(token) }) relevance += 10
                    }
                    SearchFilterType.ACCOUNTS -> {
                        if (handleLower.contains(token) || nameLower.contains(token)) relevance += 10
                    }
                    else -> {
                        if (tags.any { it == token }) relevance += 12
                        else if (tags.any { it.contains(token) }) relevance += 8
                        if (handleLower.contains(token) || nameLower.contains(token)) relevance += 9
                        if (captionLower.contains(token)) relevance += 6
                        if (locationLower.contains(token)) relevance += 5
                    }
                }
            }
            if (relevance > 0) post to relevance else null
        }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun parseTags(tagsCsv: String): List<String> {
        return tagsCsv.split(",", "،", " ")
            .map { it.trim().removePrefix("#").lowercase() }
            .filter { it.isNotEmpty() }
    }
}
