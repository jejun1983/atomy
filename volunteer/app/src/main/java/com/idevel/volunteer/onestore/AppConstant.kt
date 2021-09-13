package com.idevel.volunteer.onestore

/**
 * Lucky ONE - 게임 동작에 필요한 상수 값
 * Custom pid : 허용 가능한 문자 셋 (1-9, a-z, .(점), _(밑줄))
 */
class AppConstant private constructor() {
    companion object {

        const val PRODUCT_AUTO_SUBSCRIBE_MONTH_001  = "vip.inapp.001"

        const val PRODUCT_INAPP_SINGLE_MONTH_002    = "lucky.inapp.002"
        const val PRODUCT_INAPP_SINGLE_MONTH_003    = "bottle.inapp.003"
        const val PRODUCT_INAPP_SINGLE_MONTH_004    = "bottle.inapp.004"
        const val PRODUCT_INAPP_SINGLE_MONTH_005    = "bottle.inapp.005"
        const val PRODUCT_INAPP_SINGLE_MONTH_006    = "bottle.inapp.006"
        const val PRODUCT_INAPP_SINGLE_MONTH_007    = "bottle.inapp.007"

        // Lucky ONE Shared Preference Key
//        const val KEY_MODE_MONTHLY = "MODE_MONTHLY"
//        const val KEY_TOTAL_COIN = "TOTAL_COIN"
        const val KEY_PAYLOAD = "PAYLOAD"
//        const val KEY_IS_FIRST = "IS_FIRST"
    }

    init {
        throw IllegalAccessError("Utility class")
    }
}