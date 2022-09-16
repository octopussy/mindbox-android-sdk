package cloud.mindbox.mobile_sdk.inapp.domain

sealed class InAppType {
    object NoInApp : InAppType()

    class SimpleImage(val imageUrl: String, val redirectUrl: String, val intentData: String) :
        InAppType()
}