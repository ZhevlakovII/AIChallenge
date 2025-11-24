# RAG: Документный индексатор (KMP)

Мини‑проект для локальной индексации Markdown‑документов:
- Разбивка текста на чанки
- Генерация эмбеддингов (Ollama)
- Сохранение индекса в JSON

Стек: Kotlin Multiplatform (KMP + Compose Multiplatform плагины в билд‑логике), Ktor, Kotlin Coroutines, Kotlinx.Serialization. Хранилище — локальный JSON. Провайдер эмбеддингов — Ollama (Windows/другая ОС).

## Модули

- rag/doc-indexer/core
  - Модель индекса, порты/абстракции, реализация чанкинга и пайплайна
  - Ключевые сущности: DocumentIndex, Document, Chunk
  - Чанкер: CharOverlapChunker (символьное скользящее окно с overlap и простыми markdown‑эвристиками)
  - Пайплайн: IndexBuilder (нормализация → чанкинг → эмбеддинги → сборка индекса)
- rag/doc-indexer/ollama
  - OllamaEmbedder: Ktor‑клиент к /api/embeddings
  - Поддержка разных структур ответа (embedding | embeddings | data[].embedding)
- rag/doc-indexer/fs-jvm
  - FsContentReaderJvm: сканирует/читает .md из файловой системы (JVM)
  - Sha256Hasher: SHA‑256 от полного нормализованного текста файла
  - JsonIndexWriter: сериализация индекса в JSON
- rag/doc-indexer/app
  - CLI на JVM: парсит аргументы, собирает пайплайн, вызывает IndexBuilder и пишет JSON

## Ограничения V1

- Поддерживаются только .md файлы
- Окна чанкинга — по символам (эвристики: двойной перенос строки, заголовки \n#)
- Нет инкрементальности (всегда полная пересборка)
- Нет бэтчинга запросов в Ollama (по одному чанку)
- Индекс — один JSON‑файл

## Схема данных (упрощённо)

```json
{
  "version": "1",
  "builtAt": "2025-11-24T16:30:00Z",
  "source": { "inputDir": "./memory-bank" },
  "model": { "provider": "ollama", "name": "mxbai-embed-large", "endpoint": "http://localhost:11434" },
  "params": {
    "targetTokens": 400, "overlapTokens": 80, "charsPerToken": 3.0,
    "maxChars": 1200, "overlapChars": 240, "concurrency": 4
  },
  "documents": [
    {
      "id": "sessions/2025-11-21.md",
      "path": "sessions/2025-11-21.md",
      "title": "# Session 2025-11-21",
      "sha256": "…",
      "chunks": [
        {
          "id": "sessions/2025-11-21.md::0",
          "index": 0,
          "start": 0,
          "end": 1179,
          "text": "…",
          "embedding": [0.0123, -0.98, …]
        }
      ]
    }
  ],
  "stats": { "docs": 42, "chunks": 390, "avgChunkLen": 1157.3, "elapsedMs": 9321 }
}
```

Поля `start`/`end` — полузакрытый интервал [start, end), `text` — срез нормализованного текста файла.

## Сборка

Проект уже интегрирован в общий Gradle и собирается стандартно.

- Сборка всего проекта:
  ./gradlew build

- Сборка сабпроекта CLI:
  ./gradlew :rag:doc-indexer:app:build

- Тесты ядра:
  ./gradlew :rag:doc-indexer:core:check
  или
  ./gradlew :rag:doc-indexer:core:jvmTest

## Запуск CLI

CLI запускается из модуля app:

```bash
./gradlew :rag:doc-indexer:app:run --args '\
  build \
  --input-dir "./memory-bank" \
  --out "./rag/out/index.json" \
  --target-tokens 400 \
  --overlap-tokens 80 \
  --chars-per-token 3.0 \
  --concurrency 4 \
  --ollama-url "http://localhost:11434" \
  --model "mxbai-embed-large"'
```

Примечание о путях:
- CLI автоматически определяет корень репозитория, поднимаясь от текущей рабочей директории до директории, где есть settings.gradle.kts или .git.
- Все относительные пути (--input-dir, --out) резолвятся относительно корня репозитория, поэтому команды можно вызывать из любого подпроекта.

Параметры:
- --input-dir: корень сканирования .md
- --out: путь до результирующего JSON
- --target-tokens: целевой размер чанка в токенах (используется для расчёта символов)
- --overlap-tokens: перекрытие в токенах
- --chars-per-token: среднее число символов на токен (для RU обычно 2.8–3.5)
- --concurrency: параллелизм запросов к эмбеддеру
- --ollama-url: адрес Ollama (по умолчанию http://localhost:11434)
- --model: имя модели эмбеддингов (например, mxbai-embed-large)
- --retries, --initial-backoff-ms, --verbose: дополнительные опции устойчивости/логирования

Примеры:
- Индексация локальной папки docs:
  ./gradlew :rag:doc-indexer:app:run --args 'build --input-dir "./docs" --out "./rag/out/index.json"'
- Если Ollama работает на Windows‑машине в сети:
  ./gradlew :rag:doc-indexer:app:run --args 'build --input-dir "./memory-bank" --out "./rag/out/index.json" --ollama-url "http://192.168.1.50:11434"'

Важно:
- На хосте с Ollama должно быть выполнено:
  ollama pull mxbai-embed-large
- Убедитесь, что порт 11434 доступен из среды, где запускается Gradle.

## Проверка результата

Быстрая проверка структуры (jq):
- jq '.stats' ./rag/index.json
- jq '.documents[0].chunks[0] | {id,start,end,dim:(.embedding|length)}' ./rag/index.json

Инварианты:
- stats.docs > 0, stats.chunks > 0
- Каждый chunk.embedding — непустой массив чисел одинаковой размерности
- Для чанков в документе:
  - id = "<relativePath>::<index>"
  - 0 <= start < end <= length(normText)
  - Соседние чанки начинаются на (prev.end - overlapChars)

## Тесты

Добавлены базовые unit‑тесты (модуль core):
- CharOverlapChunkerTest — корректность чанкинга, прогресс и overlap, привязка к естественным границам
- IndexBuilderTest — нормализация текста, базовая проверка пайплайна, валидность диапазонов и эмбеддингов

Запуск:
./gradlew :rag:doc-indexer:core:check

## Дальнейшие улучшения

- Кэш эмбеддингов по хэшу чанка (ускорит повторные индексации)
- Бэтчевые запросы к Ollama (если модель/сборка поддержит массив input)
- Шардирование индекса (index_meta.json + shards/*.json) для крупных корпусов
- Поддержка PDF/DOCX (внешний текст‑экстрактор → вход в пайплайн)
