package cloud.mindbox.mobile_sdk.monitoring

internal interface MonitoringRepository {

    fun getRequestIds(): HashSet<String>
    fun saveRequestId(id: String)

    suspend fun saveLog(timestamp: Long, message: String)

    suspend fun getLogs(): List<LogResponse>
    suspend fun sendLogs(
        monitoringStatus: String,
        requestId: String,
        logs: List<LogResponse>,
    )
}