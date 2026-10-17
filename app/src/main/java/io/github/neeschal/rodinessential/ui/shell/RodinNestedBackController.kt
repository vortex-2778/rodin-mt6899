package io.github.neeschal.rodinessential.ui.shell

/**
 * A nested detail (a colour-mode overlay, an easter egg) owns Back before the
 * shell navigates. Ownership is explicit and detached on dispose.
 */
object RodinNestedBackController {
    private var owner: Any? = null
    private var handler: (() -> Boolean)? = null
    private var canPreviewHandler: (() -> Boolean)? = null

    val canPreview: Boolean get() = canPreviewHandler?.invoke() ?: false

    fun attach(
        owner: Any,
        handler: () -> Boolean,
        canPreview: (() -> Boolean)? = null,
    ) {
        this.owner = owner
        this.handler = handler
        canPreviewHandler = canPreview
    }

    fun detach(owner: Any) {
        if (this.owner !== owner) return
        this.owner = null
        handler = null
        canPreviewHandler = null
    }

    fun handleBack(): Boolean = handler?.invoke() ?: false
}
