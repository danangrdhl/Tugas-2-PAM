package com.example.newsfeedsimulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF0D47A1),
            secondary = Color(0xFF1976D2),
            background = Color(0xFFF5F5F5),
            surface = Color.White
        )
    ) {
        val viewModel = remember { NewsViewModel() }
        val newsItems by viewModel.newsList.collectAsState()
        val readCount by viewModel.readCount.collectAsState()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                StatsCard(readCount)

                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LIVE FEED",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = RoundedCornerShape(50),
                        color = Color.Red
                    ) {}
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
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
}

@Composable
fun StatsCard(count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Dibaca",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "$count Berita",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun NewsItemCard(news: News, onReadMore: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = when (news.category) {
                    "TEKNOLOGI" -> Color(0xFFE3F2FD)
                    "POLITIK" -> Color(0xFFFFEBEE)
                    else -> Color(0xFFF3E5F5)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = news.category,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = news.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = news.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                maxLines = if (news.isDetailLoaded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (news.isDetailLoaded) {
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "DETAIL LENGKAP",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = news.detailContent,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = {
                            isLoading = true
                            onReadMore()
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Baca Selengkapnya")
                        }
                    }
                }
            }
        }
    }
}