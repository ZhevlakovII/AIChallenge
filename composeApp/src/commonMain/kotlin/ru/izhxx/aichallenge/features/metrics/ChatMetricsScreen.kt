package ru.izhxx.aichallenge.features.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.features.chat.presentation.ChatViewModel

/**
 * Экран для отображения метрик чата
 * Показывает информацию об использовании токенов и эффективности сжатия истории
 */
@Composable
fun ChatMetricsScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = koinViewModel()
) {
    val metrics by viewModel.metrics.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Метрики использования",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        MetricRow("Всего запросов:", "${metrics.responseCount}")
        MetricRow("Всего сообщений:", "${metrics.messageCount}")
        MetricRow("Проведено сжатий:", "${metrics.compressionCount}")
        MetricRow("Токены запросов:", "${metrics.totalPromptTokens}")
        MetricRow("Токены ответов:", "${metrics.totalCompletionTokens}")
        MetricRow("Всего токенов:", "${metrics.totalTokens}")
        MetricRow("Среднее время ответа:", "${metrics.averageResponseTime} мс")

        if (metrics.compressionCount > 0) {
//            Text(
//                text = "Эффективность сжатия",
//                style = MaterialTheme.typography.headlineSmall,
//                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
//            )
//
//            // Примерный расчет эффективности
//            val tokensPerMessage = if (metrics.messageCount > 0)
//                metrics.totalTokens / metrics.messageCount else 0
//
//            val estimatedSavings = metrics.compressionCount * 5 * tokensPerMessage -
//                    metrics.compressionCount * tokensPerMessage
//
//            val savingsPercent = if (metrics.totalTokens > 0)
//                (estimatedSavings * 100 / metrics.totalTokens) else 0
//
//            MetricRow("Примерная экономия токенов:", "$estimatedSavings")
//            MetricRow("Экономия в %:", "$savingsPercent%")

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Пояснение к метрикам",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = """
                    Механизм сжатия истории диалога работает следующим образом:
                    
                    1. Когда количество сообщений достигает порога (5 сообщений), 
                       создается суммаризация всей истории
                    
                    2. Эта суммаризация заменяет исходные сообщения, сохраняя контекст диалога
                    
                    3. При достижении следующего порога, создается новая суммаризация, 
                       которая учитывает предыдущую и новые сообщения
                    
                    Это позволяет значительно экономить токены при сохранении контекста диалога.
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Строка с метрикой
 */
@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
