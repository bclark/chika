package com.chakra.comicreader.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chakra.comicreader.ComicReaderApp
import com.chakra.comicreader.data.db.ComicEntity
import com.chakra.comicreader.data.library.LibraryRepository
import com.chakra.comicreader.data.settings.AppSettings
import com.chakra.comicreader.data.smb.SmbEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val settings: AppSettings,
) : ViewModel() {

    /** Search query for filtering comics in the library. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val comics: StateFlow<List<ComicEntity>> = combine(repository.comics, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim()
            list.filter { it.title.contains(q, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Global default reading direction applied to newly imported comics. */
    private val _defaultRightToLeft = MutableStateFlow(settings.defaultRightToLeft)
    val defaultRightToLeft: StateFlow<Boolean> = _defaultRightToLeft.asStateFlow()

    /** Number of columns displayed in the library grid. */
    private val _libraryColumns = MutableStateFlow(settings.libraryColumns)
    val libraryColumns: StateFlow<Int> = _libraryColumns.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _smbPath = MutableStateFlow("")
    val smbPath: StateFlow<String> = _smbPath.asStateFlow()

    private val _smbEntries = MutableStateFlow<List<SmbEntry>>(emptyList())
    val smbEntries: StateFlow<List<SmbEntry>> = _smbEntries.asStateFlow()

    private val _smbLoading = MutableStateFlow(false)
    val smbLoading: StateFlow<Boolean> = _smbLoading.asStateFlow()

    val hasSmbConfig: Boolean get() = settings.hasSmbConfig

    fun importComic(uri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            _importProgress.value = 0f
            val result = repository.importComic(uri) { _importProgress.value = it }
            _importing.value = false
            result.exceptionOrNull()?.let {
                _message.value = it.message ?: "Import failed."
            }
        }
    }

    fun loadSmbDirectory(path: String = "") {
        viewModelScope.launch {
            _smbPath.value = path
            _smbLoading.value = true
            val result = repository.listSmbDirectory(path)
            _smbLoading.value = false
            result.onSuccess { _smbEntries.value = it }
                .onFailure { _message.value = "Drive error: ${it.message ?: "Connection failed"}" }
        }
    }

    fun importSmbComic(entry: SmbEntry) {
        viewModelScope.launch {
            _importing.value = true
            _importProgress.value = 0f
            val result = repository.importSmbComic(entry) { _importProgress.value = it }
            _importing.value = false
            result.exceptionOrNull()?.let {
                _message.value = it.message ?: "Import from drive failed."
            }
        }
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun deleteComic(id: Long) {
        viewModelScope.launch { repository.deleteComic(id) }
    }

    /** Flips the global default reading direction for comics imported from now on. */
    fun toggleDefaultDirection() {
        settings.defaultRightToLeft = !settings.defaultRightToLeft
        _defaultRightToLeft.value = settings.defaultRightToLeft
    }

    /** Sets the number of columns in the library grid and persists it. */
    fun setLibraryColumns(columns: Int) {
        settings.libraryColumns = columns
        _libraryColumns.value = columns
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun consumeMessage() {
        _message.value = null
    }

    companion object {
        fun factory(app: ComicReaderApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(app.libraryRepository, app.settings) as T
            }
    }
}
