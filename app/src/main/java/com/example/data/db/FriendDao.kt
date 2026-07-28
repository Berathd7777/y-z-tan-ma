package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {
    @Transaction
    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun getAllFriendsWithPhotos(): Flow<List<FriendWithPhotos>>

    @Transaction
    @Query("SELECT * FROM friends WHERE id = :id LIMIT 1")
    suspend fun getFriendWithPhotosById(id: Long): FriendWithPhotos?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendPhoto(photo: FriendPhoto): Long

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteFriendById(id: Long)

    @Query("DELETE FROM friend_photos WHERE id = :id")
    suspend fun deletePhotoById(id: Long)
}
