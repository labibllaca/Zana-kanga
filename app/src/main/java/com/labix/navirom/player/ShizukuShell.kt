package com.labix.navirom.player

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

/**
 * Runs privileged shell commands through Shizuku (ADB-level privileges without root).
 * Everything is best-effort: when Shizuku is missing or unprivileged the callers fall
 * back to their unprivileged paths.
 */
object ShizukuShell {
    private const val TAG = "ShizukuShell"

    fun isBinderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        isBinderAlive() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun requestPermission(requestCode: Int) {
        try {
            if (isBinderAlive()) Shizuku.requestPermission(requestCode)
        } catch (t: Throwable) {
            Log.w(TAG, "requestPermission failed: ${t.message}")
        }
    }

    /** Runs [command] as the Shizuku shell user and reports whether it exited cleanly. */
    fun run(command: String, timeoutMs: Long = 6_000L): Boolean {
        if (!hasPermission()) return false
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val finished = waitFor(process, timeoutMs)
            val exit = if (finished) process.exitValue() else -1
            if (!finished) process.destroy()
            exit == 0
        } catch (t: Throwable) {
            Log.w(TAG, "command failed ($command): ${t.message}")
            false
        }
    }

    private fun waitFor(process: Process, timeoutMs: Long): Boolean = try {
        val worker = Thread { try { process.waitFor() } catch (_: Throwable) {} }
        worker.start()
        worker.join(timeoutMs)
        !worker.isAlive
    } catch (_: Throwable) {
        false
    }
}
