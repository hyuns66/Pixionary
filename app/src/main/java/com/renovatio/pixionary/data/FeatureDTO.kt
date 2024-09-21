package com.renovatio.pixionary.data

import com.renovatio.pixionary.domain.model.Feature
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Unique

@Entity
data class FeatureDTO(
    @Unique val path : String = "",
    val feature : FloatArray = FloatArray(0),
    @Id var id: Long = 0
)

fun FeatureDTO.toModel(): Feature {
    return Feature(
        path = this.path,
        feature = this.feature
    )
}

fun Feature.toDTO(): FeatureDTO {
    return FeatureDTO(
        path = this.path,
        feature = this.feature
    )
}