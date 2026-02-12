package com.example.newsfeedsimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.random.Random

data class News(
    val id: Int,
    val title: String,
    val category: String,
    val summary: String,
    var isDetailLoaded: Boolean = false,
    var detailContent: String = ""
)

class NewsRepository {
    private val categories = listOf("Teknologi", "Olahraga", "Politik", "Hiburan")

    fun getNewsStream(): Flow<News> = flow {
        var idCounter = 1
        while (true) {
            val randomCategory = categories.random()
            val news = News(
                id = idCounter,
                title = "Breaking News #$idCounter",
                category = randomCategory,
                summary = "Ringkasan kejadian penting seputar $randomCategory..."
            )
            emit(news)
            idCounter++
            delay(2000)
        }
    }

    suspend fun fetchNewsDetail(newsId: Int): String {
        delay(1500)
        return "Ini adalah detail lengkap dan mendalam untuk berita #$newsId. " +
                "Data ini diambil secara asynchronous tanpa memblokir UI utama."
    }
}

class NewsViewModel {
    private val repository = NewsRepository()
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _newsList = MutableStateFlow<List<News>>(emptyList())
    val newsList: StateFlow<List<News>> = _newsList.asStateFlow()

    private val _readCount = MutableStateFlow(0)
    val readCount: StateFlow<Int> = _readCount.asStateFlow()

    init {
        startNewsStream()
    }

    private fun startNewsStream() {
        viewModelScope.launch {
            repository.getNewsStream()
                .map { originalNews ->
                    originalNews.copy(category = originalNews.category.uppercase())
                }
                .collect { newNews ->
                    _newsList.update { currentList ->
                        listOf(newNews) + currentList
                    }
                }
        }
    }

    fun loadDetail(newsId: Int) {
        viewModelScope.launch {
            val detail = repository.fetchNewsDetail(newsId)
            _newsList.update { currentList ->
                currentList.map {
                    if (it.id == newsId) {
                        it.copy(isDetailLoaded = true, detailContent = detail)
                    } else it
                }
            }

            incrementReadCount()
        }
    }

    private fun incrementReadCount() {
        _readCount.value += 1
    }
}


@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel = remember { NewsViewModel() }
        val newsItems by viewModel.newsList.collectAsState()
        val readCount by viewModel.readCount.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            StatsCard(readCount)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Live News Feed (Update 2s)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(newsItems, key = { it.id }) { news ->
                    NewsItemCard(
                        news = news,
                        onReadMore = { viewModel.loadDetail(news.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard(count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Status Pembaca", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$count Berita Dibaca",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun NewsItemCard(news: News, onReadMore: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = if (news.category == "TEKNOLOGI") Color.Blue.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = news.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = news.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = news.summary, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(8.dp))

            if (news.isDetailLoaded) {
                Column(
                    modifier = Modifier
                        .background(Color.LightGray.copy(alpha = 0.2f))
                        .padding(8.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Detail:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(text = news.detailContent, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        onReadMore()
                    },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memuat...")
                    } else {
                        Text("Baca Selengkapnya")
                    }
                }
            }
        }
    }
}