package de.rki.coronawarnapp.submission

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.protobuf.ProtoConverterFactory
import testhelpers.BaseIOTest
import java.io.File

class SubmissionModuleTest : BaseIOTest() {

    @MockK lateinit var context: Context

    private val testDir = File(IO_TEST_BASEDIR, this::class.java.simpleName)
    private val cacheFiles = File(testDir, "cache")

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        every { context.cacheDir } returns cacheFiles

        testDir.mkdirs()
        testDir.exists() shouldBe true
    }

    @AfterEach
    fun teardown() {
        testDir.deleteRecursively()
    }

    @Test
    fun `client creation uses connection specs`() {
        val specs = listOf(ConnectionSpec.MODERN_TLS)
        val client = OkHttpClient.Builder().build()

        val newClient = SubmissionModule.cdnHttpClient(
            defaultHttpClient = client,
            connectionSpecs = specs
        )

        newClient.apply {
            connectionSpecs shouldBe specs
        }
    }

    @Test
    fun `api uses a cache`() {
        val client = OkHttpClient.Builder().build()

        SubmissionModule.provideSubmissionApi(
            context = context,
            client = client,
            url = "https://testurl",
            gsonConverterFactory = GsonConverterFactory.create(),
            protoConverterFactory = ProtoConverterFactory.create()
        )

        verify { context.cacheDir }
    }
}
