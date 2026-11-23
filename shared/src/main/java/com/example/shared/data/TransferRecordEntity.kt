package com.example.shared.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_records")
data class TransferRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePathOrUri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val direction: String, // "SENT" or "RECEIVED"
    val peerName: String,
    val averageSpeedBytesPerSec: Long,
    val timestampMs: Long,
    val isVideo: Boolean
)
