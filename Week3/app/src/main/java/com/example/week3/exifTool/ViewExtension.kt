/**
 * Photo EXIF Toolkit for Android.
 *
 * Copyright (C) 2017 Ángel Iván Gladín García
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.example.week3.exifTool

import androidx.annotation.StringRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar


/**
 * Created on 12/22/16.
 */
fun CoordinatorLayout.showSnackbar(@StringRes text: Int) {
    val snackbar = Snackbar.make(this, context.getText(text), Snackbar.LENGTH_LONG)
    snackbar.show()
}

fun CoordinatorLayout.showSnackbar(text: String) {
    val snackbar = Snackbar.make(this, text, Snackbar.LENGTH_LONG)
    snackbar.show()
}