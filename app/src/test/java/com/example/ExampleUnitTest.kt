package com.example

import com.example.data.algorithm.RecommendationEngine
import com.example.data.local.PostEntity
import com.example.data.model.AlgorithmWeights
import com.example.data.model.SearchFilterType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun recommendationEngine_respectsBlockedTagsAndRanksDeterministicAffinity() {
        val likedPost = PostEntity(
            id = 1,
            authorId = "u1",
            authorHandle = "my.ristam",
            authorDisplayName = "حسابي",
            authorAvatar = "img_ristam_icon_fg",
            caption = "تصوير معماري ليلي",
            tagsCsv = "عمارة,ليلي",
            isLiked = true
        )
        val candidateMatch = PostEntity(
            id = 2,
            authorId = "u1",
            authorHandle = "my.ristam",
            authorDisplayName = "حسابي",
            authorAvatar = "img_ristam_icon_fg",
            caption = "إضاءة حديثة",
            tagsCsv = "عمارة,ضوء"
        )
        val blockedPost = PostEntity(
            id = 3,
            authorId = "u1",
            authorHandle = "my.ristam",
            authorDisplayName = "حسابي",
            authorAvatar = "img_ristam_icon_fg",
            caption = "منشور مخفي",
            tagsCsv = "مزعج"
        )

        val ranked = RecommendationEngine.rankPosts(
            allPosts = listOf(likedPost, candidateMatch, blockedPost),
            weights = AlgorithmWeights(),
            blockedTags = listOf("مزعج")
        )
        assertEquals(2, ranked.size)
        assertFalse(ranked.any { it.post.id == 3L })

        val searchResults = RecommendationEngine.smartSearch(
            query = "#عمارة",
            posts = listOf(likedPost, candidateMatch, blockedPost),
            filterType = SearchFilterType.TAGS,
            blockedTags = listOf("مزعج")
        )
        assertEquals(2, searchResults.size)
    }
}
