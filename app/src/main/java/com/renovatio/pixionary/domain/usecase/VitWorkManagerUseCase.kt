package com.renovatio.pixionary.domain.usecase

import androidx.lifecycle.LiveData
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.renovatio.pixionary.util.VitBackgroundRunner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class VitWorkManagerUseCase @Inject constructor(
    private val workManager: WorkManager
){
    operator fun invoke() : Flow<WorkInfo> {
        // WorkRequest 생성
        val workRequest = OneTimeWorkRequestBuilder<VitBackgroundRunner>().build()
        // WorkManager에 작업 enqueue
        workManager.enqueue(workRequest)
        return workManager.getWorkInfoByIdFlow(workRequest.id)
    }
}