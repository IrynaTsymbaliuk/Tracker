package com.tracker.core.collector

/**
 * Base exception for collector failures.
 */
sealed class CollectorException(message: String) : Exception(message)

/**
 * Thrown when required permission is not granted.
 */
class PermissionDeniedException(
    val permission: String
) : CollectorException("Permission denied: $permission")

/**
 * Thrown when a required system service is unavailable.
 */
class SystemServiceUnavailableException(
    val serviceName: String
) : CollectorException("System service unavailable: $serviceName")

/**
 * Thrown when no monitorable apps are installed.
 */
class NoMonitorableAppsException : CollectorException(
    "No known apps are installed"
)

/**
 * Thrown when PackageManager operations fail.
 */
class PackageManagerException(
    cause: Throwable
) : CollectorException("Failed to query installed applications: ${cause.message}")
