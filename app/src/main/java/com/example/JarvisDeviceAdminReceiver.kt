package com.example

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Receiver for Device Administration events in J.A.R.V.I.S.
 * Grants administrative privileges for device control, lock screen management,
 * and persistent background execution.
 */
class JarvisDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Administrator enabled for J.A.R.V.I.S.")
        Toast.makeText(context, "J.A.R.V.I.S. Device Administrator Activated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Administrator disabled for J.A.R.V.I.S.")
        Toast.makeText(context, "J.A.R.V.I.S. Device Administrator Deactivated", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling J.A.R.V.I.S. Device Admin will restrict automated phone actions and screen management privileges."
    }

    companion object {
        private const val TAG = "JarvisDeviceAdmin"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext, JarvisDeviceAdminReceiver::class.java)
        }

        fun isDeviceAdminActive(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val componentName = getComponentName(context)
            return dpm?.isAdminActive(componentName) == true
        }

        fun getAddDeviceAdminIntent(context: Context): Intent {
            val componentName = getComponentName(context)
            return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Activate J.A.R.V.I.S. Device Administrator to allow full system management, lock screen controls, and 24/7 background execution."
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
