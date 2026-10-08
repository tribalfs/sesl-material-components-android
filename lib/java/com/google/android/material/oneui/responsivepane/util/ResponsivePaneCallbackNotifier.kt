package com.google.android.material.oneui.responsivepane.util

import android.view.View
import com.google.android.material.oneui.common.internal.debug
import com.google.android.material.oneui.responsivepane.ResponsivePaneLayout
import com.google.android.material.oneui.responsivepane.ResponsivePaneTag

/**
 * Forwards drawer notifications to registered listeners and delegates mutable-list operations.
 *
 * Callbacks run synchronously; avoid modifying the listener list during dispatch.
 *
 * @property callbacks backing listener list used for registration and dispatch.
 */
class ResponsivePaneCallbackNotifier(
	val callbacks: MutableList<ResponsivePaneLayout.ResponsivePaneListener>
) : MutableList<ResponsivePaneLayout.ResponsivePaneListener> by callbacks,
	ResponsivePaneLayout.ResponsivePaneListener, ResponsivePaneTag {

	/**
	 * Tag used for listener dispatch log messages.
	 */
	override val logTag: String = "ResponsivePaneCallbackNotifier"

	/**
	 * Synchronously forwards a closed-state notification for [drawer] to each registered listener.
	 */
	override fun onDrawerClosed(drawer: View) {
		debug("onDrawerClosed drawer=$drawer")
		callbacks.forEach { it.onDrawerClosed(drawer) }
	}

	/**
	 * Synchronously forwards an opened-state notification for [drawer] to each registered listener.
	 */
	override fun onDrawerOpened(drawer: View) {
		debug("onDrawerOpened drawer=$drawer")
		callbacks.forEach { it.onDrawerOpened(drawer) }
	}

	/**
	 * Synchronously forwards slide progress for [drawer] to each registered listener.
	 *
	 * @param slideRatio normalized progress, with 0 closed and 1 opened.
	 */
	override fun onDrawerSlide(drawer: View, slideRatio: Float) {
		debug("onDrawerSlide drawer=$drawer, slideRatio=$slideRatio")
		callbacks.forEach { it.onDrawerSlide(drawer, slideRatio) }
	}

	/**
	 * Returns the registered listener list for logging.
	 */
	override fun toString(): String {
		return callbacks.toString()
	}
}
