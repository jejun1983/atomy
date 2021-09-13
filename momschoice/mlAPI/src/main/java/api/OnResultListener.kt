package api

/**
 * API Result를 받기 위한 Listener
 * @param <T>
</T> */
interface OnResultListener<Any> {
    fun onResult(result: Any, flag: Int)
    fun onFail(error: Any, flag: Int)
}

