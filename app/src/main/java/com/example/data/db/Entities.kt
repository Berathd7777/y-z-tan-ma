package com.example.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val socialMedia: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "friend_photos",
    foreignKeys = [
        ForeignKey(
            entity = Friend::class,
            parentColumns = ["id"],
            childColumns = ["friendId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("friendId")]
)
data class FriendPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: Long,
    val photoBytes: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FriendPhoto
        if (id != other.id) return false
        if (friendId != other.friendId) return false
        if (!photoBytes.contentEquals(other.photoBytes)) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + friendId.hashCode()
        result = 31 * result + photoBytes.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

data class FriendWithPhotos(
    @Embedded val friend: Friend,
    @Relation(
        parentColumn = "id",
        entityColumn = "friendId"
    )
    val photos: List<FriendPhoto>
)
