package com.andre.shakeflashlight

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log

class TorchController(context: Context) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = findBackCameraWithFlash()

    @Volatile
    private var isOn = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == this@TorchController.cameraId) isOn = enabled
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == this@TorchController.cameraId) isOn = false
        }
    }

    init {
        cameraManager.registerTorchCallback(torchCallback, null)
    }

    fun hasFlash(): Boolean = cameraId != null

    fun isOn(): Boolean = isOn

    /** Flips the current torch state.  Off -> On, On -> Off. */
    fun toggle(): Boolean {
        val id = cameraId ?: return false
        return try {
            val next = !isOn
            cameraManager.setTorchMode(id, next)
            isOn = next
            true
        } catch (e: CameraAccessException) {
            Log.w(TAG, "toggle failed", e)
            false
        }
    }

    fun turnOff() {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, false)
            isOn = false
        } catch (e: CameraAccessException) {
            Log.w(TAG, "turnOff failed", e)
        }
    }

    fun release() {
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (_: Exception) { /* ignore */ }
    }

    private fun findBackCameraWithFlash(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val c = cameraManager.getCameraCharacteristics(id)
                val hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = c.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: CameraAccessException) {
            Log.w(TAG, "camera enumeration failed", e)
            null
        }
    }

    companion object {
        private const val TAG = "TorchController"
    }
}
