package cloud.mindbox.mobile_sdk.inapp.presentation

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import cloud.mindbox.mobile_sdk.R
import cloud.mindbox.mobile_sdk.inapp.domain.models.InAppType
import cloud.mindbox.mobile_sdk.inapp.domain.models.InAppTypeWrapper
import cloud.mindbox.mobile_sdk.inapp.presentation.view.InAppConstraintLayout
import cloud.mindbox.mobile_sdk.logger.MindboxLoggerImpl
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.util.*


internal class InAppMessageViewDisplayerImpl(private val picasso: Picasso) :
    InAppMessageViewDisplayer {

    private var currentRoot: ViewGroup? = null
    private var currentBlur: View? = null
    private var currentDialog: InAppConstraintLayout? = null
    private var currentActivity: Activity? = null
    private var inAppCallback: InAppCallback? = null
    private var currentInAppId: String? = null
    private val inAppQueue = LinkedList<InAppTypeWrapper>()


    private fun isUiPresent(): Boolean =
        (currentRoot != null) && (currentDialog != null) && (currentBlur != null)


    override fun onResumeCurrentActivity(activity: Activity, shouldUseBlur: Boolean) {
        MindboxLoggerImpl.d(this, "onResumeCurrentActivity: ${activity.hashCode()}")
        currentRoot = activity.window.decorView.rootView as ViewGroup
        currentBlur = if (shouldUseBlur) {
            MindboxLoggerImpl.i(InAppMessageViewDisplayerImpl, "Enable blur")
            LayoutInflater.from(activity).inflate(
                R.layout.blur_layout,
                currentRoot, false
            )
        } else {
            MindboxLoggerImpl.i(InAppMessageViewDisplayerImpl, "Disable blur")
            LayoutInflater.from(activity).inflate(
                R.layout.blur_layout,
                currentRoot, false
            ).apply {
                setBackgroundColor(
                    ContextCompat.getColor(
                        activity,
                        android.R.color.transparent
                    )
                )
            }
        }
        currentActivity = activity
        currentDialog = LayoutInflater.from(activity).inflate(
            R.layout.default_inapp_layout,
            currentRoot, false
        ) as InAppConstraintLayout

        if (inAppQueue.isNotEmpty() && !isInAppMessageActive) {
            with(inAppQueue.pop()) {
                MindboxLoggerImpl.d(
                    this,
                    "trying to show in-app with id ${inAppType.inAppId} from queue"
                )
                showInAppMessage(
                    inAppType = inAppType,
                    onInAppClick = onInAppClick,
                    onInAppShown = onInAppShown
                )
            }
        }
    }

    override fun registerCurrentActivity(activity: Activity, shouldUseBlur: Boolean) {
        MindboxLoggerImpl.d(this, "registerCurrentActivity: ${activity.hashCode()}")
        currentRoot = activity.window.decorView.rootView as ViewGroup
        currentBlur = if (shouldUseBlur) {
            MindboxLoggerImpl.i(InAppMessageViewDisplayerImpl, "Enable blur")
            LayoutInflater.from(activity).inflate(
                R.layout.blur_layout,
                currentRoot, false
            )
        } else {
            MindboxLoggerImpl.i(InAppMessageViewDisplayerImpl, "Disable blur")
            LayoutInflater.from(activity).inflate(
                R.layout.blur_layout,
                currentRoot, false
            ).apply {
                setBackgroundColor(
                    ContextCompat.getColor(
                        activity,
                        android.R.color.transparent
                    )
                )
            }
        }
        currentActivity = activity
        currentDialog = LayoutInflater.from(activity).inflate(
            R.layout.default_inapp_layout,
            currentRoot, false
        ) as InAppConstraintLayout

        if (inAppQueue.isNotEmpty() && !isInAppMessageActive) {
            with(inAppQueue.pop()) {
                MindboxLoggerImpl.d(
                    this,
                    "trying to show in-app with id ${inAppType.inAppId} from queue"
                )
                showInAppMessage(
                    inAppType = inAppType,
                    onInAppClick = onInAppClick,
                    onInAppShown = onInAppShown
                )
            }
        }
    }

    override fun registerInAppCallback(inAppCallback: InAppCallback) {
        this.inAppCallback = inAppCallback
    }

    override fun onPauseCurrentActivity(activity: Activity) {
        MindboxLoggerImpl.d(this, "onPauseCurrentActivity: ${activity.hashCode()}")
        if (currentActivity == activity) {
            currentActivity = null
        }
        clearUI()
    }

    override fun tryShowInAppMessage(
        inAppType: InAppType,
        onInAppClick: () -> Unit,
        onInAppShown: () -> Unit,
    ) {
        if (isUiPresent()) {
            MindboxLoggerImpl.d(
                this,
                "In-app with id ${inAppType.inAppId} is going to be shown immediately"
            )
            showInAppMessage(inAppType, onInAppClick, onInAppShown)

        } else {
            addToInAppQueue(inAppType, onInAppClick, onInAppShown)
            MindboxLoggerImpl.d(
                this,
                "In-app with id ${inAppType.inAppId} is added to showing queue and will be shown later"
            )
        }
    }

    private fun clearUI() {
        if (isInAppMessageActive) {
            currentInAppId?.let { id ->
                inAppCallback?.onInAppDismissed(id)
                MindboxLoggerImpl.d(this, "In-app dismissed")
            }
        }
        isInAppMessageActive = false
        currentRoot?.removeView(currentBlur)
        currentRoot?.removeView(currentDialog)
        currentRoot = null
        currentDialog = null
        currentBlur = null
    }

    private fun addToInAppQueue(
        inAppType: InAppType,
        onInAppClick: () -> Unit,
        onInAppShown: () -> Unit,
    ) {
        inAppQueue.add(InAppTypeWrapper(inAppType, onInAppClick, onInAppShown))
    }


    private fun showInAppMessage(
        inAppType: InAppType,
        onInAppClick: () -> Unit,
        onInAppShown: () -> Unit,
    ) {
        when (inAppType) {
            is InAppType.SimpleImage -> {
                if (inAppType.imageUrl.isNotBlank()) {
                    isInAppMessageActive = true

                    if (currentRoot == null) {
                        MindboxLoggerImpl.e(this, "failed to show inapp: currentRoot is null")
                        isInAppMessageActive = false
                    }
                    currentRoot?.addView(currentBlur)
                    currentRoot?.addView(currentDialog)
                    currentDialog?.requestFocus()

                    with(currentRoot?.findViewById<ImageView>(R.id.iv_content)) {
                        MindboxLoggerImpl.d(
                            this@InAppMessageViewDisplayerImpl,
                            "try to show inapp with id ${inAppType.inAppId}"
                        )

                        Glide
                            .with(currentActivity!!.applicationContext)
                            .load("https://mindbox-pushok.umbrellait.tech:444/?image=mindbox.png&broken=true&error=wait&speed=2")
                            .timeout(3000)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    MindboxLoggerImpl.e(
                                        parent = this@InAppMessageViewDisplayerImpl,
                                        message = "Failed to load inapp image",
                                        exception = e
                                            ?: RuntimeException("Failed to load inapp image")
                                    )
                                    currentRoot?.removeView(currentDialog)
                                    currentRoot?.removeView(currentBlur)
                                    isInAppMessageActive = false
                                    this@with?.isVisible = false
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    this@with?.isVisible = true
                                    currentInAppId = inAppType.inAppId
                                    currentRoot?.findViewById<ImageView>(R.id.iv_close)?.apply {
                                        setOnClickListener {
                                            inAppCallback?.onInAppDismissed(inAppType.inAppId)
                                            MindboxLoggerImpl.d(this, "In-app dismissed")
                                            currentRoot?.removeView(currentDialog)
                                            currentRoot?.removeView(currentBlur)
                                            isInAppMessageActive = false
                                        }
                                        isVisible = true
                                    }
                                    currentDialog?.setOnClickListener {
                                        currentDialog?.isEnabled = false
                                        onInAppClick()
                                        inAppCallback?.onInAppClick(
                                            inAppType.inAppId,
                                            inAppType.redirectUrl,
                                            inAppType.intentData
                                        )
                                        if (inAppType.redirectUrl.isNotBlank() || inAppType.intentData.isNotBlank()) {
                                            currentRoot?.removeView(currentDialog)
                                            currentRoot?.removeView(currentBlur)
                                            isInAppMessageActive = false
                                        }
                                    }
                                    currentDialog?.setDismissListener {
                                        inAppCallback?.onInAppDismissed(inAppType.inAppId)
                                        MindboxLoggerImpl.d(this, "In-app dismissed")
                                        currentRoot?.removeView(currentDialog)
                                        currentRoot?.removeView(currentBlur)
                                        isInAppMessageActive = false
                                    }
                                    currentBlur?.setOnClickListener {
                                        inAppCallback?.onInAppDismissed(inAppType.inAppId)
                                        MindboxLoggerImpl.d(this, "In-app dismissed")
                                        currentRoot?.removeView(currentDialog)
                                        currentRoot?.removeView(currentBlur)
                                        isInAppMessageActive = false
                                    }
                                    currentBlur?.isVisible = true
                                    MindboxLoggerImpl.d(
                                        this@InAppMessageViewDisplayerImpl,
                                        "inapp shown"
                                    )
                                    onInAppShown()
                                    return false
                                }
                            })
                            .centerCrop()
                            .into(this!!)
//
//                        picasso
//                            .load(inAppType.imageUrl)
//                            .fit()
//                            .centerCrop()
//                            .into(this, object : Callback {
//                                override fun onSuccess() {
//                                    this@with?.isVisible = true
//                                    currentInAppId = inAppType.inAppId
//                                    currentRoot?.findViewById<ImageView>(R.id.iv_close)?.apply {
//                                        setOnClickListener {
//                                            inAppCallback?.onInAppDismissed(inAppType.inAppId)
//                                            MindboxLoggerImpl.d(this, "In-app dismissed")
//                                            currentRoot?.removeView(currentDialog)
//                                            currentRoot?.removeView(currentBlur)
//                                            isInAppMessageActive = false
//                                        }
//                                        isVisible = true
//                                    }
//                                    currentDialog?.setOnClickListener {
//                                        currentDialog?.isEnabled = false
//                                        onInAppClick()
//                                        inAppCallback?.onInAppClick(
//                                            inAppType.inAppId,
//                                            inAppType.redirectUrl,
//                                            inAppType.intentData
//                                        )
//                                        if (inAppType.redirectUrl.isNotBlank() || inAppType.intentData.isNotBlank()) {
//                                            currentRoot?.removeView(currentDialog)
//                                            currentRoot?.removeView(currentBlur)
//                                            isInAppMessageActive = false
//                                        }
//                                    }
//                                    currentDialog?.setDismissListener {
//                                        inAppCallback?.onInAppDismissed(inAppType.inAppId)
//                                        MindboxLoggerImpl.d(this, "In-app dismissed")
//                                        currentRoot?.removeView(currentDialog)
//                                        currentRoot?.removeView(currentBlur)
//                                        isInAppMessageActive = false
//                                    }
//                                    currentBlur?.setOnClickListener {
//                                        inAppCallback?.onInAppDismissed(inAppType.inAppId)
//                                        MindboxLoggerImpl.d(this, "In-app dismissed")
//                                        currentRoot?.removeView(currentDialog)
//                                        currentRoot?.removeView(currentBlur)
//                                        isInAppMessageActive = false
//                                    }
//                                    currentBlur?.isVisible = true
//                                    MindboxLoggerImpl.d(
//                                        this@InAppMessageViewDisplayerImpl,
//                                        "inapp shown"
//                                    )
//                                    onInAppShown()
//                                }
//
//                                override fun onError(e: Exception?) {
//                                    MindboxLoggerImpl.e(
//                                        parent = this@InAppMessageViewDisplayerImpl,
//                                        message = "Failed to load inapp image",
//                                        exception = e
//                                            ?: RuntimeException("Failed to load inapp image")
//                                    )
//                                    currentRoot?.removeView(currentDialog)
//                                    currentRoot?.removeView(currentBlur)
//                                    isInAppMessageActive = false
//                                    this@with?.isVisible = false
//                                }
//                            })
                    }
                } else {
                    MindboxLoggerImpl.d(this, "in-app image url is blank")
                }
            }
        }
    }

    companion object {
        var isInAppMessageActive = false
    }
}

