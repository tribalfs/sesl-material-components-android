package com.google.android.material.oneui.responsivepane.helper.concept

/**
 * Visual endpoints interpolated as a drawer opens.
 *
 * @property minConcept settings at slide ratio 0, normally the closed state.
 * @property maxConcept settings at slide ratio 1, normally the opened state.
 */
data class ElevationConceptRange(
	val minConcept: ElevationConcept,
	val maxConcept: ElevationConcept
)
