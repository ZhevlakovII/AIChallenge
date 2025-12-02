package ru.izhxx.aichallenge.features.pranalyzer.impl.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.AnalysisReportUi
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.AnalysisStage
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.CodeIssueUi
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.DocumentationRefUi
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerEffect
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerIntent
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrInfoUi
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.RecommendationUi
import kotlin.time.ExperimentalTime

@ExperimentalTime
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrAnalyzerScreen(
    modifier: Modifier = Modifier,
    viewModel: PrAnalyzerViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    // Handle one-off effects
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PrAnalyzerEffect.OpenUrl -> {
                    runCatching { uriHandler.openUri(effect.url) }
                }

                is PrAnalyzerEffect.ShowMessage -> {
                    // На текущем этапе просто выводим в лог/консоль.
                    // Можно заменить на Snackbar при необходимости.
                    println("PrAnalyzer: ${effect.message}")
                }

                is PrAnalyzerEffect.ExportReport -> {
                    // Экспорт в рамках KMP потребует платформенной реализации.
                    // Пока просто логируем.
                    println("Export report: ${effect.fileName}\n${effect.content.take(200)}...")
                }

                PrAnalyzerEffect.NavigateBack -> {}
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("PR Analyzer") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // URL input
            OutlinedTextField(
                value = state.prUrl,
                onValueChange = { viewModel.accept(PrAnalyzerIntent.PrUrlChanged(it)) },
                label = { Text("GitHub PR URL") },
                placeholder = { Text("https://github.com/owner/repo/pull/123") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.accept(PrAnalyzerIntent.StartAnalysis) },
                    enabled = state.canAnalyze && !state.isLoading
                ) {
                    Text("Анализ")
                }

                if (state.isLoading) {
                    OutlinedButton(
                        onClick = { viewModel.accept(PrAnalyzerIntent.RetryAnalysis) }
                    ) {
                        Text("Повторить")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Error
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = "Ошибка",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Row {
                            OutlinedButton(onClick = { viewModel.accept(PrAnalyzerIntent.RetryAnalysis) }) {
                                Text("Повторить")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Progress
            if (state.isLoading) {
                Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(progress = { state.progress })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Стадия: ${stageTitle(state.loadingStage)} (${(state.progress * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // PR Info
                state.prInfo?.let { info ->
                    item {
                        SectionHeader("Информация о PR")
                    }
                    item {
                        PrInfoCard(info)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Report
                state.analysisReport?.let { report ->
                    item { SectionHeader("Отчет анализа") }
                    item {
                        ReportSummaryCard(report)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (report.issues.isNotEmpty()) {
                        item { SectionHeader("Найденные проблемы") }
                        items(
                            items = report.issues,
                            key = { it.id }
                        ) { issue ->
                            IssueItem(
                                issue = issue,
                                expanded = issue.id in state.expandedIssueIds,
                                onToggle = { viewModel.accept(PrAnalyzerIntent.ExpandIssue(issue.id)) }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    if (report.recommendations.isNotEmpty()) {
                        item { SectionHeader("Рекомендации") }
                        items(
                            items = report.recommendations,
                            key = { it.id }
                        ) { rec ->
                            RecommendationItem(rec)
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    if (report.documentationRefs.isNotEmpty()) {
                        item { SectionHeader("Документация") }
                        items(
                            items = report.documentationRefs,
                            key = { it.id }
                        ) { ref ->
                            DocumentationItem(
                                ref = ref,
                                onOpen = { url ->
                                    if (!url.isNullOrBlank()) {
                                        viewModel.accept(PrAnalyzerIntent.OpenDocumentation(url))
                                    }
                                }
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.accept(PrAnalyzerIntent.ExportReport) }
                                ) {
                                    Text("Экспорт отчета")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Divider()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PrInfoCard(info: PrInfoUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("#${info.number} — ${info.title}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Автор: ${info.author}")
            Text("Ветки: ${info.branch} → ${info.baseBranch}")
            Text("Статус: ${info.status}")
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Файлов: ${info.filesChanged}")
                Text("Добавлено: ${info.linesAdded}")
                Text("Удалено: ${info.linesDeleted}")
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(report: AnalysisReportUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Сводка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(report.summary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Общая: ${report.overallScore}")
                Text("Читаемость: ${report.readabilityScore}")
                Text("Поддерживаемость: ${report.maintainabilityScore}")
                Text("Безопасность: ${report.securityScore}")
            }
            Spacer(Modifier.height(8.dp))
            if (report.strengths.isNotEmpty()) {
                Text("Сильные стороны:", fontWeight = FontWeight.SemiBold)
                report.strengths.forEach { Text("• $it") }
                Spacer(Modifier.height(4.dp))
            }
            if (report.weaknesses.isNotEmpty()) {
                Text("Слабые стороны:", fontWeight = FontWeight.SemiBold)
                report.weaknesses.forEach { Text("• $it") }
            }
            Spacer(Modifier.height(8.dp))
            Text("Сгенерировано: ${report.generatedAt}")
        }
    }
}

@Composable
private fun IssueItem(
    issue: CodeIssueUi,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${issue.severity} • ${issue.category}: ${issue.title}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                OutlinedButton(onClick = onToggle) {
                    Text(if (expanded) "Свернуть" else "Подробнее")
                }
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                issue.description.takeIf { it.isNotBlank() }?.let { Text(it) }
                issue.file?.let { Text("Файл: $it") }
                issue.lineNumber?.let { Text("Строка: $it") }
                issue.codeSnippet?.let {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                issue.suggestion?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("Рекомендация: $it", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RecommendationItem(rec: RecommendationUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                rec.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(rec.description)
            Spacer(Modifier.height(6.dp))
            Text("Приоритет: ${rec.priority}")
            if (rec.rationale.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Обоснование: ${rec.rationale}")
            }
            if (rec.relatedFiles.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Файлы:")
                rec.relatedFiles.forEach { Text("• $it") }
            }
            if (rec.actionableSteps.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Шаги:")
                rec.actionableSteps.forEach { Text("• $it") }
            }
        }
    }
}

@Composable
private fun DocumentationItem(
    ref: DocumentationRefUi,
    onOpen: (String?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                ref.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            ref.section?.let {
                Spacer(Modifier.height(2.dp))
                Text("Раздел: $it", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(ref.excerpt)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Релевантность: ${"%.2f".format(ref.relevanceScore)}")
                OutlinedButton(onClick = { onOpen(ref.url) }, enabled = !ref.url.isNullOrBlank()) {
                    Text("Открыть")
                }
            }
        }
    }
}

private fun stageTitle(stage: AnalysisStage): String = when (stage) {
    AnalysisStage.IDLE -> "Ожидание"
    AnalysisStage.FETCHING_PR_INFO -> "Получение информации о PR"
    AnalysisStage.FETCHING_DIFF -> "Загрузка diff"
    AnalysisStage.SEARCHING_DOCS -> "Поиск документации"
    AnalysisStage.ANALYZING_WITH_LLM -> "LLM-анализ"
    AnalysisStage.GENERATING_REPORT -> "Генерация отчета"
    AnalysisStage.COMPLETED -> "Готово"
}
