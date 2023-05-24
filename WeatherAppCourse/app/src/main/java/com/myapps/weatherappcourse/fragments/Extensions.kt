package com.myapps.weatherappcourse.fragments

import androidx.fragment.app.Fragment
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

fun Fragment.isPermissionGranted(p: String): Boolean{
    return ContextCompat.checkSelfPermission(activity as AppCompatActivity, p) == PackageManager.PERMISSION_GRANTED
}