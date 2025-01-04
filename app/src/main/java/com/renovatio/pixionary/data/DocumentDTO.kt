package com.renovatio.pixionary.data

import android.net.Uri
import com.renovatio.pixionary.domain.model.Document
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique

@Entity
data class DocumentDTO (
    @Unique val uriString : String,
    val text : String,
    @Id var id: Long = 0
)

fun DocumentDTO.toModel() : Document {
    return Document(
        uri = Uri.parse(this.uriString),
        text = this.text
    )
}