package de.rki.coronawarnapp.storage

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import de.rki.coronawarnapp.util.coroutine.AppScope
import de.rki.coronawarnapp.util.coroutine.DispatcherProvider
import de.rki.coronawarnapp.util.di.AppContext
import de.rki.coronawarnapp.util.reset.Resettable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus
import javax.inject.Qualifier
import javax.inject.Singleton

@Module(includes = [StorageModule.ResetModule::class])
object StorageModule {

    @Singleton
    @Provides
    @StorageDataStore
    fun provideDataStore(
        @AppContext context: Context,
        @AppScope appScope: CoroutineScope,
        dispatcherProvider: DispatcherProvider
    ) = PreferenceDataStoreFactory.create(
        scope = appScope + dispatcherProvider.IO,
        produceFile = { context.preferencesDataStoreFile(STORAGE_DATASTORE_NAME) },
        migrations = LEGACY_SHARED_PREFS.map { SharedPreferencesMigration(context, it) }
    )

    @Module
    internal interface ResetModule {
        @Binds
        @IntoSet
        fun bindResettableStorageReset(resettable: StorageReset): Resettable
    }
}

private const val STORAGE_DATASTORE_NAME = "storage_localdata"

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class StorageDataStore

private val LEGACY_SHARED_PREFS
    get() = listOf(
        "test_settings",
        "onboarding_localdata",
        "tracing_settings"
    )
