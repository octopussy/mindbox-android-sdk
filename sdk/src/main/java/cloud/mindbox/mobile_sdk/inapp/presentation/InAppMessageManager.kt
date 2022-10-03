package cloud.mindbox.mobile_sdk.inapp.presentation

import android.app.Activity
import android.content.Context
import cloud.mindbox.mobile_sdk.Mindbox
import cloud.mindbox.mobile_sdk.MindboxConfiguration
import cloud.mindbox.mobile_sdk.inapp.domain.InAppInteractor
import cloud.mindbox.mobile_sdk.inapp.domain.InAppMessageViewDisplayer
import cloud.mindbox.mobile_sdk.managers.GatewayManager
import cloud.mindbox.mobile_sdk.managers.MindboxEventManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

internal class InAppMessageManager {

    private val inAppMessageViewDisplayer: InAppMessageViewDisplayer by inject(
        InAppMessageViewDisplayerImpl::class.java)
    private val inAppInteractor: InAppInteractor by inject(InAppInteractor::class.java)


    fun initInAppMessages(context: Context, configuration: MindboxConfiguration) {
        Mindbox.mindboxScope.launch {
            inAppInteractor.processEventAndConfig(context, configuration).collect { inAppMessage ->
                withContext(Dispatchers.Main)
                {
                    if (InAppMessageViewDisplayerImpl.isInAppMessageActive.not()) {
                        inAppMessageViewDisplayer.showInAppMessage(inAppMessage)
                    }
                }
            }
        }
        inAppInteractor.fetchInAppConfig(context, configuration)
    }

    fun onPauseCurrentActivity(activity: Activity) {
        inAppMessageViewDisplayer.onPauseCurrentActivity(activity)
    }

    fun onResumeCurrentActivity(activity: Activity, shouldUseBlur: Boolean) {
        inAppMessageViewDisplayer.onResumeCurrentActivity(activity, shouldUseBlur)
    }

}