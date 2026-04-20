package com.mobichill.justconcentration.view.language

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.constants.Constants
import com.mobichill.justconcentration.utils.LanguageUtil
import com.mobichill.justconcentration.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class LanguageViewModelFactory(
    private val application: Application,
    private val preferenceRepository: PreferenceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LanguageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LanguageViewModel(application, preferenceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class LanguageViewModel(
    application: Application,
    private val preferenceRepository: PreferenceRepository
) : AndroidViewModel(application) {

    private val _languageList = MutableLiveData<List<LanguageModel>>()
    val languageList: LiveData<List<LanguageModel>> = _languageList

    private val _currentlyViewedLanguage = MutableLiveData<LanguageModel?>()
    val currentlyViewedLanguage: LiveData<LanguageModel?> = _currentlyViewedLanguage

    private val _showLoading = MutableLiveData<Boolean>()
    val showLoading: LiveData<Boolean> = _showLoading

    private val _navigateToNextScreen = MutableLiveData<Event<Boolean>>()
    val navigateToNextScreen: LiveData<Event<Boolean>> = _navigateToNextScreen

    var isSettingMode: Boolean = false
        private set

    private var _initialSavedLanguage: LanguageModel? = null

    init {
        loadInitialLanguages()
    }

    fun setSettingMode(isSetting: Boolean) {
        isSettingMode = isSetting
    }

    private fun loadInitialLanguages() {
        val app = getApplication<Application>()
        val initialList = mutableListOf(
            LanguageModel(
                app.getString(R.string.english),
                app.getString(R.string.english),
                Constants.ISO_LANGUAGE_EN,
                false,
                R.drawable.ic_language_english
            ),
            LanguageModel(
                app.getString(R.string.spanish),
                app.getString(R.string.espanol),
                Constants.ISO_LANGUAGE_ES,
                false,
                R.drawable.ic_language_spanish
            ),
            LanguageModel(
                app.getString(R.string.portuguese),
                app.getString(R.string.portugues),
                Constants.ISO_LANGUAGE_PT,
                false,
                R.drawable.ic_language_portugal
            ),
            LanguageModel(
                app.getString(R.string.hindi),
                app.getString(R.string.espanol),
                Constants.ISO_LANGUAGE_HI,
                false,
                R.drawable.ic_language_hindi
            ),
            LanguageModel(
                app.getString(R.string.france),
                app.getString(R.string.french),
                Constants.ISO_LANGUAGE_FR,
                false,
                R.drawable.ic_language_france
            ),
            LanguageModel(
                app.getString(R.string.german),
                app.getString(R.string.espanol),
                Constants.ISO_LANGUAGE_DE,
                false,
                R.drawable.ic_language_german
            ),
            LanguageModel(
                app.getString(R.string.indonesian),
                app.getString(R.string.indonesian), // Corrected: should be Indonesian for Indonesian
                Constants.ISO_LANGUAGE_IN,
                false,
                R.drawable.ic_language_indonesian
            ),
        )
        _languageList.value = initialList
        setActiveLanguageFromPrefs()
    }

    // This method is called when an item is clicked.
    fun onLanguageSelected(model: LanguageModel) {
        val targetLanguagesWithSubOptions = setOf("en", "es", "pt")

        // Update the currently viewed language model
        val updatedList = _languageList.value?.map { lang ->
            lang.copy(
                isCheck = (lang.isoLanguage == model.isoLanguage),
                isType = if (lang.isoLanguage == model.isoLanguage) model.isType else 0,
                isOp1 = lang.isoLanguage == model.isoLanguage && model.isType == 1,
                isOp2 = lang.isoLanguage == model.isoLanguage && model.isType == 2,
                isOp3 = lang.isoLanguage == model.isoLanguage && model.isType == 3,
                isCollapse = lang.isCollapse
            )
        }
        _languageList.value = updatedList ?: emptyList()
        _currentlyViewedLanguage.value = model

        // Show loading
        viewModelScope.launch {
            _showLoading.value = true
            delay(1000)
            _showLoading.value = false
        }
    }

    fun onToggleCollapse(isoLanguage: String) {
        val updatedList = _languageList.value?.map { lang ->
            if (lang.isoLanguage == isoLanguage) {
                // Toggle the collapse state for the clicked language
                lang.copy(isCollapse = !lang.isCollapse)
            } else {
                // Collapse all other languages
                lang.copy(isCollapse = false)
            }
        }
        _languageList.value = updatedList ?: emptyList()
    }

    private fun setActiveLanguageFromPrefs() {
        var (savedLangIso, savedLangType) = preferenceRepository.getAppLanguageAndType()
        val app = getApplication<Application>()
        val isDefaultEnglish = (savedLangIso == Constants.ISO_LANGUAGE_EN && savedLangType == 0)
        if (isDefaultEnglish) {
            savedLangType = 1
        }
        val targetLanguagesWithSubOptions = setOf("en", "es", "pt")

        val updatedList = _languageList.value?.map { language ->
            if (language.isoLanguage == savedLangIso) {
                val newModel = language.copy(isCheck = true, isType = savedLangType)
                if (language.isoLanguage in targetLanguagesWithSubOptions) {
                    newModel.copy(
                        isOp1 = (savedLangType == 1),
                        isOp2 = (savedLangType == 2),
                        isOp3 = (savedLangType == 3),
                        isCollapse = true
                    )
                } else {
                    newModel.copy(isType = 0)
                }
            } else {
                language.copy(isCheck = false, isCollapse = false, isOp1 = false, isOp2 = false, isOp3 = false)
            }
        }
        _languageList.value = updatedList ?: emptyList()

        // Set _currentlyViewedLanguage AND _initialSavedLanguage based on preferences
        val selected = updatedList?.find { it.isoLanguage == savedLangIso }?.let {
            it.copy(
                languageName = Utils.getLanguageDisplayName(it.isoLanguage),
                image = it.image // Keep original image
            )
        } ?: _languageList.value?.firstOrNull()?.copy(isCheck = true, isType = 0) // Default to first if nothing saved/found

        _currentlyViewedLanguage.value = selected
        _initialSavedLanguage = selected // Store the truly saved language
    }

    fun onConfirmSelectionClicked() {
        _currentlyViewedLanguage.value?.let { confirmed ->
            // ONLY on confirmation do we save to preferences
            preferenceRepository.setAppLanguageAndType(confirmed.isoLanguage, confirmed.isType)

            val app = getApplication<Application>()
            LanguageUtil.saveLocale(app, confirmed.isoLanguage)
            LanguageUtil.setPreLanguage(app, confirmed.isoLanguage)
            LanguageUtil.setLocale(app)

            _navigateToNextScreen.value = Event(isSettingMode)
        }
    }

    // If user exit without clicking tick btn
    fun revertToInitialSavedLanguage() {
        _initialSavedLanguage?.let { initial ->
            // Re-apply the initial saved language to the UI
            val updatedList = _languageList.value?.map { language ->
                if (language.isoLanguage == initial.isoLanguage) {
                    val newModel = language.copy(isCheck = true, isType = initial.isType)
                    if (language.isoLanguage in setOf("en", "es", "pt")) {
                        newModel.copy(
                            isOp1 = (initial.isType == 1),
                            isOp2 = (initial.isType == 2),
                            isOp3 = (initial.isType == 3),
                            isCollapse = true
                        )
                    } else {
                        newModel.copy(isType = 0)
                    }
                } else {
                    language.copy(isCheck = false, isCollapse = false, isOp1 = false, isOp2 = false, isOp3 = false)
                }
            }
            _languageList.value = updatedList ?: emptyList()
            _currentlyViewedLanguage.value = initial
        }
    }
}