package com.wdtt.client.network

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log

object SimOperatorDetector {
    private const val TAG = "SimOperatorDetector"

    /**
     * Detects mobile operator preset from SIM card / TelephonyManager.
     * Returns one of: "mts", "beeline", "megafon", "tele2", "sbermobile", "volna", or null if not detected.
     */
    fun detectOperatorPreset(context: Context): String? {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return null

            val simOperatorName = telephonyManager.simOperatorName?.trim() ?: ""
            val networkOperatorName = telephonyManager.networkOperatorName?.trim() ?: ""
            val simOperator = telephonyManager.simOperator?.trim() ?: ""
            val networkOperator = telephonyManager.networkOperator?.trim() ?: ""

            Log.d(TAG, "SIM operator detection: simName='$simOperatorName', netName='$networkOperatorName', simOp='$simOperator', netOp='$networkOperator'")

            val combinedName = "$simOperatorName $networkOperatorName".lowercase()
            val opCode = simOperator.ifEmpty { networkOperator }

            // 1. Check by operator name strings
            when {
                combinedName.contains("mts") || combinedName.contains("мтс") -> return "mts"
                combinedName.contains("beeline") || combinedName.contains("билайн") || combinedName.contains("vimpelcom") -> return "beeline"
                combinedName.contains("megafon") || combinedName.contains("мегафон") -> return "megafon"
                combinedName.contains("tele2") || combinedName.contains("теле2") -> return "tele2"
                combinedName.contains("sber") || combinedName.contains("сбер") -> return "sbermobile"
                combinedName.contains("volna") || combinedName.contains("волна") -> return "volna"
            }

            // 2. Check by MCC + MNC (Russian MCC = 250)
            if (opCode.startsWith("250") && opCode.length >= 5) {
                val mnc = opCode.removePrefix("250")
                when (mnc) {
                    "01", "07" -> return "mts"
                    "20" -> return "tele2"
                    "62" -> return "sbermobile"
                    "30" -> return "volna"
                    "99", "43" -> return "beeline"
                    "02" -> return "megafon"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect SIM operator: ${e.message}")
        }
        return null
    }
}
