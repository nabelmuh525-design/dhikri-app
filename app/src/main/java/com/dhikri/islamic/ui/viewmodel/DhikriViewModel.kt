package com.dhikri.islamic.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dhikri.islamic.data.local.entity.*
import com.dhikri.islamic.data.preferences.UserSettings
import com.dhikri.islamic.data.preferences.UserSettingsRepository
import android.content.Context
import com.dhikri.islamic.data.repository.DhikriRepository
import com.dhikri.islamic.data.update.AppUpdateInfo
import com.dhikri.islamic.data.update.AppUpdater
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DhikriViewModel(
    private val repository: DhikriRepository,
    private val settingsRepository: UserSettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val surahs: StateFlow<List<SurahEntity>> = repository.observeAllSurahs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val juzs: StateFlow<List<JuzMetadataEntity>> = repository.observeAllJuzs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val hizbQuarters: StateFlow<List<HizbQuarterEntity>> = repository.observeAllHizbQuarters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sajdahs: StateFlow<List<AyahEntity>> = repository.observeSajdahAyahs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lastRead: StateFlow<ReadingHistoryEntity?> = repository.observeLastRead()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.observeBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteAyahs: StateFlow<List<FavoriteAyahEntity>> = repository.observeFavoriteAyahs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tafsirBooks: StateFlow<List<TafsirBookEntity>> = repository.observeTafsirBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val adhkarCategories: StateFlow<List<AdhkarCategoryEntity>> = repository.observeAdhkarCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favoriteAdhkar: StateFlow<List<AdhkarItemEntity>> = repository.observeFavoriteAdhkar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stories: StateFlow<List<StoryEntity>> = repository.observeStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasbihPresets: StateFlow<List<TasbihPresetEntity>> = repository.observeTasbihPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasbihDailyLogs: StateFlow<List<TasbihDailyLogEntity>> = repository.observeTasbihDailyLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayTasbihTotal: StateFlow<Int> = repository.observeTodayTasbihTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val downloadPackages: StateFlow<List<DownloadPackageEntity>> = repository.observeDownloadPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _searchResults = MutableStateFlow<List<AyahEntity>>(emptyList())
    val searchResults: StateFlow<List<AyahEntity>> = _searchResults.asStateFlow()

    private val _tafsirSearchResults = MutableStateFlow<List<TafsirEntryEntity>>(emptyList())
    val tafsirSearchResults: StateFlow<List<TafsirEntryEntity>> = _tafsirSearchResults.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private val _updateProgress = MutableStateFlow<Int?>(null)
    val updateProgress: StateFlow<Int?> = _updateProgress.asStateFlow()

    private val _updateStatusMsg = MutableStateFlow<String?>(null)
    val updateStatusMsg: StateFlow<String?> = _updateStatusMsg.asStateFlow()

    init {
        checkForAppUpdate(silent = true)
    }

    fun checkForAppUpdate(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _updateStatusMsg.value = "جاري التحقق من وجود تحديثات..."
            val info = AppUpdater.checkForUpdate()
            _availableUpdate.value = info
            if (!silent) {
                _updateStatusMsg.value = if (info != null) {
                    "يتوفر تحديث جديد (${info.latestVersion})"
                } else {
                    "أنت تستخدم أحدث إصدار من تطبيق ذِكري"
                }
            }
        }
    }

    fun triggerAutoUpdateFromNotification(context: Context) {
        if (_updateProgress.value != null) return
        viewModelScope.launch {
            val info = _availableUpdate.value ?: AppUpdater.checkForUpdate().also {
                _availableUpdate.value = it
            }
            if (info != null) {
                installAppUpdate(context)
            }
        }
    }

    fun installAppUpdate(context: Context) {
        val info = _availableUpdate.value ?: return
        if (_updateProgress.value != null) return
        viewModelScope.launch {
            _updateProgress.value = 0
            _updateStatusMsg.value = "جاري تحميل التحديث (${info.latestVersion})..."
            val res = AppUpdater.downloadAndInstall(context, info.apkUrl) { pct ->
                _updateProgress.value = pct
            }
            _updateProgress.value = null
            if (res.isFailure) {
                _updateStatusMsg.value = "تعذّر تحميل التحديث — تأكد من الاتصال بالإنترنت"
            }
        }
    }

    fun observePageAyahs(pageNumber: Int): Flow<List<AyahEntity>> =
        repository.observeAyahsByPage(pageNumber.coerceIn(1, 604))

    fun observeAdhkarItems(categoryId: Int): Flow<List<AdhkarItemEntity>> =
        repository.observeAdhkarItems(categoryId)

    suspend fun getTafsirForAyah(surahId: Int, ayahNumber: Int): List<TafsirEntryEntity> =
        repository.getTafsirForAyah(surahId, ayahNumber)

    fun searchQuran(query: String) {
        viewModelScope.launch {
            _searchResults.value = repository.searchQuran(query)
        }
    }

    fun searchTafsir(query: String) {
        viewModelScope.launch {
            _tafsirSearchResults.value = repository.searchTafsir(query)
        }
    }

    fun saveLastRead(surahId: Int, ayahNumber: Int, pageNumber: Int) {
        viewModelScope.launch {
            repository.saveLastRead(surahId, ayahNumber, pageNumber)
        }
    }

    fun addBookmark(surahId: Int, ayahNumber: Int, pageNumber: Int, label: String) {
        viewModelScope.launch {
            repository.addBookmark(surahId, ayahNumber, pageNumber, label)
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch { repository.deleteBookmark(bookmark) }
    }

    fun toggleFavoriteAyah(ayah: AyahEntity, isCurrentlyFav: Boolean) {
        viewModelScope.launch { repository.toggleFavoriteAyah(ayah, isCurrentlyFav) }
    }

    fun toggleAdhkarFavorite(itemId: Int, isFavorite: Boolean) {
        viewModelScope.launch { repository.toggleAdhkarFavorite(itemId, isFavorite) }
    }

    fun toggleStoryFavorite(storyId: Int, isFavorite: Boolean) {
        viewModelScope.launch { repository.toggleStoryFavorite(storyId, isFavorite) }
    }

    fun incrementTasbih(preset: TasbihPresetEntity) {
        viewModelScope.launch {
            settingsRepository.setLastTasbihPresetId(preset.id)
            repository.incrementTasbih(preset)
        }
    }

    fun decrementTasbih(preset: TasbihPresetEntity) {
        viewModelScope.launch { repository.decrementTasbih(preset) }
    }

    fun setDailyTasbihGoal(goal: Int) = viewModelScope.launch { settingsRepository.setDailyTasbihGoal(goal) }

    fun resetTasbih(preset: TasbihPresetEntity) {
        viewModelScope.launch { repository.resetTasbihCounter(preset) }
    }

    fun addCustomTasbih(phrase: String, target: Int) {
        viewModelScope.launch { repository.addCustomTasbih(phrase, target) }
    }

    fun deleteCustomTasbih(preset: TasbihPresetEntity) {
        viewModelScope.launch { repository.deleteCustomTasbih(preset) }
    }

    fun startPackageDownload(bookKey: String) = repository.enqueueTafsirDownload(bookKey)
    fun pauseOrCancelDownload(bookKey: String) = viewModelScope.launch { repository.pauseDownload(bookKey) }
    fun deleteOptionalPackage(packageKey: String) {
        viewModelScope.launch { repository.deleteOptionalPackage(packageKey) }
    }

    // Settings
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDarkMode(enabled) }
    fun setNightReadingMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setNightReadingMode(enabled) }
    fun setReadingTheme(themeKey: String) = viewModelScope.launch { settingsRepository.setReadingTheme(themeKey) }
    fun setMushafFontSize(sizeSp: Float) = viewModelScope.launch { settingsRepository.setMushafFontSize(sizeSp) }
    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setVibrationEnabled(enabled) }
    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setSoundEnabled(enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
    fun setLastTasbihPresetId(id: Int) = viewModelScope.launch { settingsRepository.setLastTasbihPresetId(id) }
    fun startKhatmah(days: Int) = viewModelScope.launch { settingsRepository.startKhatmah(days, 1) }
    fun updateKhatmahPage(page: Int) = viewModelScope.launch { settingsRepository.updateKhatmahPage(page) }
    fun resetKhatmah() = viewModelScope.launch { settingsRepository.resetKhatmah() }

    class Factory(
        private val repository: DhikriRepository,
        private val settingsRepository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DhikriViewModel(repository, settingsRepository) as T
    }
}
