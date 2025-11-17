# Шаблон: FeatureModule (Koin DI)

Назначение: DI-модуль фичи для регистрации ViewModel, UseCase-ов и реализаций репозиториев/источников. Совместим с KMP+CMP и правилами проекта (Чистая архитектура, MVI, DTO-изоляция).

См. также:
- docs/llm/file_paths_policy.md
- docs/human/FeatureDevelopmentGuide.md
- docs/human/Architecture.md
- docs/human/CodingStandards.md

Рекомендуемое размещение:
- composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/features/<feature>/di/<Feature>Module.kt

```kotlin
package ru.izhxx.aichallenge.features.<feature>.di

import org.koin.dsl.module
// В проектах с KMP используйте мультиплатформенный DSL для ViewModel, если он подключен.
// Иначе — регистрируйте VM через factory { ... }.
import org.koin.androidx.viewmodel.dsl.viewModel // если доступно в вашем сетапе KMP
// или: import org.koin.dsl.module // и используйте factory для VM при отсутствии viewModel DSL

// Доменные интерфейсы/UseCase
import ru.izhxx.aichallenge.features.<feature>.domain.usecase.<Action><Entity>UseCase
import ru.izhxx.aichallenge.features.<feature>.domain.usecase.<Action><Entity>UseCaseImpl

// (Опционально) Контракты репозиториев фичи — если реализуются локально в фиче
import ru.izhxx.aichallenge.domain.repository.<Entity>Repository
import ru.izhxx.aichallenge.data.repository.<Entity>RepositoryImpl

// ViewModel фичи
import ru.izhxx.aichallenge.features.<feature>.presentation.<Feature>ViewModel

/**
 * DI-модуль фичи <Feature>.
 *
 * Правила:
 * - Регистрируйте зависимости по интерфейсам Domain (DIP).
 * - Реализации Data находятся в shared, либо в фиче (data/) при необходимости.
 * - ViewModel не создаёт зависимости напрямую — только через DI.
 */
val <feature>Module = module {

    // Репозитории и источники (если специфично для фичи; иначе берите из shared DI)
    // factory<<Entity>Repository> { <Entity>RepositoryImpl(get(), get()) }

    // UseCase: интерфейс → реализация
    factory<<Action><Entity>UseCase> { <Action><Entity>UseCaseImpl(get()) }

    // ViewModel
    // Предпочтительно использовать viewModel DSL, если он доступен в вашем мультиплатформенном сетапе:
    viewModel {
        <Feature>ViewModel(
            <action><entity>UseCase = get()
        )
    }
    // Если viewModel DSL недоступен в текущем source set — используйте:
    // factory { <Feature>ViewModel(<action><entity>UseCase = get()) }
}
```

Подключение модуля фичи (пример):
```kotlin
// composeApp/src/commonMain/kotlin/ru/izhxx/aichallenge/di/AppModule.kt

val appModules = listOf(
    // общие модули shared:
    ru.izhxx.aichallenge.di.sharedModule,
    ru.izhxx.aichallenge.di.parsersModule,
    ru.izhxx.aichallenge.di.metricsModule,
    // ...
    // модули фич:
    ru.izhxx.aichallenge.features.chat.di.chatModule,
    ru.izhxx.aichallenge.features.<feature>.di.<feature>Module
)
```

Примечания:
- DTO и низкоуровневые сущности регистрируются только в Data-слое и не попадают в Presentation.
- Если реализация репозитория переиспользуется, держите её в `shared` и регистрируйте в общих DI-модулях.
- Для тяжёлых операций инжектируйте диспетчеры через абстракции/DI по необходимости.
