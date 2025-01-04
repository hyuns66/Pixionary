package com.renovatio.pixionary.data

import android.net.Uri
import com.renovatio.pixionary.domain.model.Feature
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique

@Entity
data class FeatureDTO(
    @Unique val uriString : String = "",
    val feature : FloatArray = FloatArray(0),
    @Id var id: Long = 0
)

fun FeatureDTO.toModel(): Feature {
    return Feature(
        uri = Uri.parse(this.uriString),
        feature = this.feature
    )
}

fun Feature.toDTO(): FeatureDTO {
    return FeatureDTO(
        uriString = this.uri.toString(),
        feature = this.feature
    )
}