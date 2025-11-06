package ru.izhxx.aichallenge.di

import org.koin.dsl.module

/**
 * Основной модуль приложения, объединяющий все остальные модули
 */
val appModule = module {
    // Включаем viewModelModule из текущего пакета
    includes(viewModelModule)
    
    // Включаем модули из shared
    // apiModule и dataModule будут доступны через Koin
    includes(sharedModule)
}
