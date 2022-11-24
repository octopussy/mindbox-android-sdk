package cloud.mindbox.mobile_sdk.inapp.presentation

import android.util.Log
import app.cash.turbine.test
import cloud.mindbox.mobile_sdk.Mindbox
import cloud.mindbox.mobile_sdk.MindboxConfiguration
import cloud.mindbox.mobile_sdk.inapp.domain.InAppInteractor
import cloud.mindbox.mobile_sdk.inapp.domain.InAppMessageViewDisplayer
import cloud.mindbox.mobile_sdk.inapp.domain.InAppType
import cloud.mindbox.mobile_sdk.logger.MindboxLoggerImpl
import cloud.mindbox.mobile_sdk.repository.MindboxPreferences
import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Modifier

internal class InAppMessageManagerTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var mindboxConfiguration: MindboxConfiguration

    @MockK
    private lateinit var inAppMessageInteractor: InAppInteractor

    @MockK
    private lateinit var inAppMessageViewDisplayer: InAppMessageViewDisplayer

    @InjectMockKs
    private lateinit var inAppMessageManager: InAppMessageManagerImpl

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun onTestStart() {
        Dispatchers.setMain(mainThreadSurrogate)
        coEvery {
            inAppMessageViewDisplayer.showInAppMessage(any(), any(), any())
        } just runs
        mockkObject(MindboxPreferences)
        mockkObject(MindboxLoggerImpl)
        mockkStatic(Log::class)
        every {
            Log.isLoggable(any(), any())
        }.answers {
            true
        }
    }

    @After
    fun onTestFinish() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
    }

    @Test
    fun `in app config is being fetched`() {
        runTest {
            coEvery {
                inAppMessageInteractor.fetchInAppConfig(mindboxConfiguration)

            } just runs
            inAppMessageManager.requestConfig(mindboxConfiguration);
            {
                coVerify(exactly = 1)
                {
                    inAppMessageInteractor.fetchInAppConfig(mindboxConfiguration)
                }
            }.shouldNotThrow()
        }
    }

    @Test
    fun `in-app config throws non network error`() {
        coEvery {
            inAppMessageInteractor.fetchInAppConfig(mindboxConfiguration)
        }.throws(Error())
        inAppMessageManager.requestConfig(mindboxConfiguration)
        verify {
            MindboxPreferences setProperty MindboxPreferences::inAppConfig.name value ""
        }
    }

    @Test
    fun `in-app config throws network error non 404`() {
        mockkConstructor(NetworkResponse::class)
        val networkResponse = mockk<NetworkResponse>()
        NetworkResponse::class.java.declaredFields[0].apply {
            isAccessible = true
            val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(this, modifiers and Modifier.FINAL.inv())

        }.setInt(networkResponse,
            403)
        every {
            MindboxPreferences getProperty MindboxPreferences::inAppConfig.name
        }.answers {
            "test"
        }
        coEvery {
            inAppMessageInteractor.fetchInAppConfig(mindboxConfiguration)
        }.throws(VolleyError(networkResponse))
        inAppMessageManager.requestConfig(mindboxConfiguration)
        verify(exactly = 1) {
            MindboxPreferences setProperty MindboxPreferences::inAppConfig.name value "test"
        }
    }

    @Test
    fun `in app config throws network error 404`() {
        mockkConstructor(NetworkResponse::class)
        val networkResponse = mockk<NetworkResponse>()
        NetworkResponse::class.java.declaredFields[0].apply {
            isAccessible = true
            val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(this, modifiers and Modifier.FINAL.inv())

        }.setInt(networkResponse,
            404)
        coEvery {
            inAppMessageInteractor.fetchInAppConfig(mindboxConfiguration)
        }.throws(VolleyError(networkResponse))
        inAppMessageManager.requestConfig(mindboxConfiguration)
        verify(exactly = 1) {
            MindboxPreferences setProperty MindboxPreferences::inAppConfig.name value ""
        }
    }

    @Test
    fun `in app messages success message`() {
        every {
            inAppMessageInteractor.processEventAndConfig(mindboxConfiguration)
        }.answers {
            flow {
                emit(InAppType.SimpleImage(inAppId = "123",
                    imageUrl = "",
                    redirectUrl = "",
                    intentData = ""))
            }
        }
        inAppMessageManager.listenEventAndInApp(mindboxConfiguration)
        runBlocking {
            inAppMessageInteractor.processEventAndConfig(mindboxConfiguration).test {
                awaitItem()
                coVerify(exactly = 1) {
                    inAppMessageViewDisplayer.showInAppMessage(any(), any(), any())
                }
                awaitComplete()
            }
        }
    }

    @Test
    fun `in app messages error message`() {
        runTest(StandardTestDispatcher()) {
            every {
                inAppMessageInteractor.processEventAndConfig(mindboxConfiguration)
            }.answers {
                flow {
                    error("test error")
                }
            }
            every {
                MindboxLoggerImpl.e(any(), any(), any())
            } just runs
            inAppMessageManager.listenEventAndInApp(mindboxConfiguration)
            advanceUntilIdle()
            inAppMessageInteractor.processEventAndConfig(mindboxConfiguration).test {
                awaitError()
                verify(exactly = 1) {
                    MindboxLoggerImpl.e(Mindbox, "Mindbox caught unhandled error", any())
                }
            }
        }
    }

    private fun (() -> Any?).shouldNotThrow() = try {
        invoke()
    } catch (ex: Exception) {
        throw Error("expected not to throw!", ex)
    }
}

