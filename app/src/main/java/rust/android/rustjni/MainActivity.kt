package rust.android.rustjni

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rust.interop.data.*
import rust.interop.logic.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var currentPage by remember { mutableIntStateOf(1) }
            var resultState by remember { mutableStateOf<FilterResponse?>(null) }
            var isLoading by remember { mutableStateOf(false) }

            // Extract total pages dynamically from the response
            val totalPages = resultState?.pagination?.totalPages?.toInt() ?: 1

            LaunchedEffect(currentPage) {
                isLoading = true
                try {
                    val params = FilterParams(null, null, null, null, currentPage.toString(), null)

                    // Switch to IO thread for the Rust JNI call
                    val response = withContext(Dispatchers.IO) {
                        fetchInteroperability(params)
                    }
                    resultState = response
                } catch (e: Exception) {
                    // Log error
                } finally {
                    isLoading = false
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Interoperability FFI", style = MaterialTheme.typography.headlineMedium)
                        Text("Rust + Android Handheld", style = MaterialTheme.typography.headlineSmall)

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (currentPage > 1) currentPage-- },
                                enabled = !isLoading && currentPage > 1
                            ) { Text("Previous") }

                            // Dynamic Status Text
                            Text("Page $currentPage of $totalPages", style = MaterialTheme.typography.bodyLarge)

                            Button(
                                onClick = { if (currentPage < totalPages) currentPage++ },
                                // DYNAMIC DISABLE: No longer hardcoded to 5
                                enabled = !isLoading && currentPage < totalPages
                            ) { Text("Next") }
                        }

                        if (isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                        }

                        resultState?.let { response ->
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(response.data) { item ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                                            Text("Language: ${item.language}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}