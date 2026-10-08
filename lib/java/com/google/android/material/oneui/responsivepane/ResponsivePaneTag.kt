package com.google.android.material.oneui.responsivepane

import com.google.android.material.oneui.common.internal.MaterialLogTag

/**
 * Logging contract shared by responsive pane components.
 */
interface ResponsivePaneTag : MaterialLogTag {
	/**
	 * Common prefix for responsive pane log messages.
	 */
	override val prefix: String
		get() = "ResponsivePane"
}
