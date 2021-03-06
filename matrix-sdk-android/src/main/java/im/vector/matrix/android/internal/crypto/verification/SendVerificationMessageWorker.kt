/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.matrix.android.internal.crypto.verification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.squareup.moshi.JsonClass
import im.vector.matrix.android.api.failure.shouldBeRetried
import im.vector.matrix.android.api.session.crypto.CryptoService
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.internal.crypto.tasks.SendVerificationMessageTask
import im.vector.matrix.android.internal.worker.SessionWorkerParams
import im.vector.matrix.android.internal.worker.WorkerParamsFactory
import im.vector.matrix.android.internal.worker.getSessionComponent
import timber.log.Timber
import javax.inject.Inject

/**
 * Possible previous worker: None
 * Possible next worker    : None
 */
internal class SendVerificationMessageWorker(context: Context,
                                             params: WorkerParameters)
    : CoroutineWorker(context, params) {

    @JsonClass(generateAdapter = true)
    internal data class Params(
            override val sessionId: String,
            val event: Event,
            override val lastFailureMessage: String? = null
    ) : SessionWorkerParams

    @Inject
    lateinit var sendVerificationMessageTask: SendVerificationMessageTask

    @Inject
    lateinit var cryptoService: CryptoService

    override suspend fun doWork(): Result {
        val errorOutputData = Data.Builder().putBoolean(OUTPUT_KEY_FAILED, true).build()
        val params = WorkerParamsFactory.fromData<Params>(inputData)
                ?: return Result.success(errorOutputData)

        val sessionComponent = getSessionComponent(params.sessionId)
                ?: return Result.success(errorOutputData).also {
                    // TODO, can this happen? should I update local echo?
                    Timber.e("Unknown Session, cannot send message, sessionId: ${params.sessionId}")
                }
        sessionComponent.inject(this)
        val localId = params.event.eventId ?: ""
        return try {
            val eventId = sendVerificationMessageTask.execute(
                    SendVerificationMessageTask.Params(
                            event = params.event,
                            cryptoService = cryptoService
                    )
            )

            Result.success(Data.Builder().putString(localId, eventId).build())
        } catch (exception: Throwable) {
            if (exception.shouldBeRetried()) {
                Result.retry()
            } else {
                Result.success(errorOutputData)
            }
        }
    }

    companion object {
        private const val OUTPUT_KEY_FAILED = "failed"

        fun hasFailed(outputData: Data): Boolean {
            return outputData.getBoolean(SendVerificationMessageWorker.OUTPUT_KEY_FAILED, false)
        }
    }
}
