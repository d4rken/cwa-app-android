package de.rki.coronawarnapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import de.rki.coronawarnapp.exception.NoRegistrationTokenSetException
import de.rki.coronawarnapp.notification.GeneralNotifications
import de.rki.coronawarnapp.notification.NotificationConstants
import de.rki.coronawarnapp.notification.TestResultAvailableNotificationService
import de.rki.coronawarnapp.service.submission.SubmissionService
import de.rki.coronawarnapp.storage.TracingSettings
import de.rki.coronawarnapp.submission.SubmissionSettings
import de.rki.coronawarnapp.util.TimeAndDateExtensions
import de.rki.coronawarnapp.util.TimeStamper
import de.rki.coronawarnapp.util.formatter.TestResult
import de.rki.coronawarnapp.util.worker.InjectedWorkerFactory
import timber.log.Timber

/**
 * Diagnosis test result retrieval by periodic polling
 *
 * @see BackgroundWorkScheduler
 */
class DiagnosisTestResultRetrievalPeriodicWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val testResultAvailableNotificationService: TestResultAvailableNotificationService,
    private val notificationHelper: GeneralNotifications,
    private val submissionSettings: SubmissionSettings,
    private val submissionService: SubmissionService,
    private val timeStamper: TimeStamper,
    private val tracingSettings: TracingSettings,
    private val backgroundWorkScheduler: BackgroundWorkScheduler,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("$id: doWork() started. Run attempt: $runAttemptCount")

        if (runAttemptCount > BackgroundConstants.WORKER_RETRY_COUNT_THRESHOLD) {
            Timber.tag(TAG).d("$id doWork() failed after $runAttemptCount attempts. Rescheduling")

            backgroundWorkScheduler.scheduleDiagnosisTestResultPeriodicWork()
            Timber.tag(TAG).d("$id Rescheduled background worker")

            return Result.failure()
        }
        var result = Result.success()
        try {

            if (abortConditionsMet(timeStamper.nowUTC.millis)) {
                Timber.tag(TAG).d(" $id Stopping worker.")
                stopWorker()
            } else {
                Timber.tag(TAG).d(" $id Running worker.")

                val registrationToken =
                    submissionSettings.registrationToken.value ?: throw NoRegistrationTokenSetException()
                val testResult = submissionService.asyncRequestTestResult(registrationToken)
                Timber.tag(TAG).d("$id: Test Result retrieved is $testResult")

                if (testResult == TestResult.NEGATIVE ||
                    testResult == TestResult.POSITIVE ||
                    testResult == TestResult.INVALID
                ) {
                    sendTestResultAvailableNotification(testResult)
                    cancelRiskLevelScoreNotification()
                    Timber.tag(TAG)
                        .d("$id: Test Result available - notification sent & risk level notification canceled")
                    stopWorker()
                }
            }
        } catch (ex: Exception) {
            result = Result.retry()
        }

        Timber.tag(TAG).d("$id: doWork() finished with %s", result)

        return result
    }

    private fun abortConditionsMet(currentMillis: Long): Boolean {
        if (tracingSettings.isTestResultAvailableNotificationSent) {
            Timber.tag(TAG).d("$id: Notification already sent.")
            return true
        }
        if (submissionSettings.hasViewedTestResult.value) {
            Timber.tag(TAG).d("$id: Test result has already been viewed.")
            return true
        }

        val calculateDays = TimeAndDateExtensions.calculateDays(
            tracingSettings.initialPollingForTestResultTimeStamp,
            currentMillis
        )
        if (calculateDays >= BackgroundConstants.POLLING_VALIDITY_MAX_DAYS) {
            Timber.tag(TAG)
                .d(" $id Maximum of ${BackgroundConstants.POLLING_VALIDITY_MAX_DAYS} days for polling exceeded.")
            return true
        }

        return false
    }

    private suspend fun sendTestResultAvailableNotification(testResult: TestResult) {
        testResultAvailableNotificationService.showTestResultAvailableNotification(testResult)
        tracingSettings.isTestResultAvailableNotificationSent = true
    }

    private fun cancelRiskLevelScoreNotification() {
        notificationHelper.cancelCurrentNotification(
            NotificationConstants.NEW_MESSAGE_RISK_LEVEL_SCORE_NOTIFICATION_ID
        )
    }

    private fun stopWorker() {
        tracingSettings.initialPollingForTestResultTimeStamp = 0L
        backgroundWorkScheduler.stopDiagnosisTestResultPeriodicWork()
        Timber.tag(TAG).d("$id: Background worker stopped")
    }

    @AssistedFactory
    interface Factory : InjectedWorkerFactory<DiagnosisTestResultRetrievalPeriodicWorker>

    companion object {
        private val TAG = DiagnosisTestResultRetrievalPeriodicWorker::class.java.simpleName
    }
}
