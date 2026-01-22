package com.cvc953.localplayer.model

import android.net.Uri
import kotlin.time.Duration

data class Song (
  val id: Long,
  val title: String,
  val artist: String,
  val album: String,
  val year: Int?,
  val uri: Uri,
  val duration: Long,
  //val albumArt: ByteArray?
)
