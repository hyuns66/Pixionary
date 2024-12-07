package com.renovatio.pixionary.domain.usecase

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.renovatio.pixionary.util.VitBackgroundWorker
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class VitWorkManagerUseCase @Inject constructor(
    private val workManager: WorkManager
){
    operator fun invoke() : Flow<WorkInfo> {
        // WorkRequest 생성
        val workRequest = OneTimeWorkRequestBuilder<VitBackgroundWorker>().build()
        // WorkManager에 작업 enqueue
        workManager.enqueue(workRequest)
        return workManager.getWorkInfoByIdFlow(workRequest.id)
    }
}