package com.mobichill.justconcentration.view.language

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LanguageModel(
    var languageName: String = "English",
    var secondTitle: String = "English",
    var isoLanguage: String = "en",
    var isCheck: Boolean = false,
    var image: Int? = null,
    var isOp1 : Boolean = false,
    var isOp2 : Boolean = false,
    var isOp3 : Boolean = false,
    var posLanguage : Int = 0,
    var isCollapse : Boolean = false,
    var isType: Int = 0
) : Parcelable