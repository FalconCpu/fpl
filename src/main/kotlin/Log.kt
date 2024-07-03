
object Log {
    val allErrors = mutableListOf<String>()
    var testMode = false

    fun error(message:String) {
        if (!testMode)
            println(message)
        allErrors += message
    }

    fun error(location: Location, message: String) = error("$location: $message")

    fun setTestMode() {
        testMode = true
        allErrors.clear()
    }
}