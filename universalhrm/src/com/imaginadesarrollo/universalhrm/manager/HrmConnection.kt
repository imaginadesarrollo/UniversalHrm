package com.imaginadesarrollo.universalhrm.manager

import android.widget.ArrayAdapter


/**
 * Created by Kike Bodi (ebp@trifork.com) on 2019-08-27.
 * Copyright by Trifork eHealth. All rights reserved.
 */
interface HrmConnection {
  fun getAdapter(): ArrayAdapter<*>
  fun disconnect()
  fun addAlertDialogCallback(callback: AlertDialogCallback)

  interface AlertDialogCallback{
    fun close()
  }
}