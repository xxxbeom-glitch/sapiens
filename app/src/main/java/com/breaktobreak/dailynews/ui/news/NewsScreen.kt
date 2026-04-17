package com.breaktobreak.dailynews.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.breaktobreak.dailynews.data.mock.MockData
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.ui.common.ArticleBottomSheet
import com.breaktobreak.dailynews.ui.theme.Background
import com.breaktobreak.dailynews.ui.theme.RowVertical

@Composable
fun NewsScreen() {
    var selectedArticle by remember { mutableStateOf<Article?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = RowVertical)
    ) {
        item {
            NewsFeedList(
                items = MockData.newsFeed,
                onClickItem = { selectedArticle = it }
            )
        }
    }

    selectedArticle?.let { article ->
        ArticleBottomSheet(
            article = article,
            onDismissRequest = { selectedArticle = null }
        )
    }
}
