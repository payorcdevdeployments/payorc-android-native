package com.payorc.sdk.presentation.embedded.tabby

import ai.tabby.android.data.Product

/**
 * State machine for the embedded Tabby payment button flow.
 *
 * Transitions:
 *   Idle → Loading → LaunchTabby → Authorized
 *                               ↘ Error
 *               ↘ Error
 */
sealed interface EmbeddedTabbyState {

    /** Initial state — button is rendered, no action taken. */
    object Idle : EmbeddedTabbyState

    /**
     * Payment flow in progress.
     * [step] is a short human-readable progress label (e.g. "Initializing...", "Creating session...").
     */
    data class Loading(val step: String = "Initializing...") : EmbeddedTabbyState

    /**
     * Tabby native checkout is ready to launch.
     * The composable should launch [product] via TabbyFactory.
     */
    data class LaunchTabby(val product: Product) : EmbeddedTabbyState

    /**
     * Payment was authorized and confirmed with the PayOrc backend.
     * [tabbyPaymentId] is the Tabby-side payment identifier.
     * [payorcOrderId] is the PayOrc backend order identifier.
     */
    data class Authorized(
        val tabbyPaymentId: String,
        val payorcOrderId: String,
        val merchantResponse: Map<String, Any?> = emptyMap()
    ) : EmbeddedTabbyState

    /**
     * An error occurred during any step of the flow.
     * [message] is a user-facing description.
     * [cause] is the underlying exception when available.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : EmbeddedTabbyState
}
