package com.renovatio.pixionary.domain.model

import android.net.Uri
import io.objectbox.annotation.Unique

data class Document (
    val uri : Uri,
    val text : String,
)