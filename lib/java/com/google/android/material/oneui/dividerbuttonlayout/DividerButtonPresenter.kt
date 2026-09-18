package com.google.android.material.oneui.dividerbuttonlayout

import android.content.Context
import android.os.Parcelable
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.appcompat.view.menu.MenuPresenter
import androidx.appcompat.view.menu.MenuView
import androidx.appcompat.view.menu.SubMenuBuilder

class DividerButtonPresenter : MenuPresenter {
    private var menu: MenuBuilder? = null
    private var menuView: DividerButtonLayout? = null
    var isUpdateSuspended: Boolean = false

    override fun initForMenu(context: Context, menu: MenuBuilder?) {
        this.menu = menu
    }

    override fun getMenuView(root: ViewGroup?): MenuView? {
        return menuView
    }

    fun setMenuView(menuView: DividerButtonLayout) {
        this.menuView = menuView
    }

    override fun updateMenuView(cleared: Boolean) {
        if (!isUpdateSuspended) {
            menuView?.updateMenuView()
        }
    }

    override fun setCallback(cb: MenuPresenter.Callback?) {}
    override fun onSubMenuSelected(subMenu: SubMenuBuilder?): Boolean = false
    override fun onCloseMenu(menu: MenuBuilder?, allMenusAreClosing: Boolean) {}
    override fun flagActionItems(): Boolean = false
    override fun expandItemActionView(menu: MenuBuilder?, item: MenuItemImpl?): Boolean = false
    override fun collapseItemActionView(menu: MenuBuilder?, item: MenuItemImpl?): Boolean = false
    override fun getId(): Int = 0
    override fun onSaveInstanceState(): Parcelable? = null
    override fun onRestoreInstanceState(state: Parcelable?) {}
}
