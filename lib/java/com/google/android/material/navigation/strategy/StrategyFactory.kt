package com.google.android.material.navigation.strategy

//sesl9
object StrategyFactory {
    fun createStrategy(type: Int, isFloatingType: Boolean): ViewTypeStrategy {
        return when (type) {
            1 -> if (isFloatingType) ViewTypeStrategy.FloatingIconLabelType() else ViewTypeStrategy.IconLabelType()
            2 -> if (isFloatingType) ViewTypeStrategy.FloatingIconOnlyType() else ViewTypeStrategy.IconOnlyType()
            3 -> ViewTypeStrategy.LabelOnlyType()
            else -> ViewTypeStrategy.IconLabelType()
        }
    }
}
