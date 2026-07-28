package com.example.data.repository

import com.example.data.db.Friend
import com.example.data.db.FriendDao
import com.example.data.db.FriendPhoto
import com.example.data.db.FriendWithPhotos
import kotlinx.coroutines.flow.Flow

class FriendRepository(private val friendDao: FriendDao) {
    val allFriends: Flow<List<FriendWithPhotos>> = friendDao.getAllFriendsWithPhotos()

    suspend fun getFriendById(id: Long): FriendWithPhotos? {
        return friendDao.getFriendWithPhotosById(id)
    }

    suspend fun insertFriend(name: String, socialMedia: String, photoBytesList: List<ByteArray>): Long {
        val friend = Friend(name = name, socialMedia = socialMedia)
        val friendId = friendDao.insertFriend(friend)
        for (bytes in photoBytesList) {
            friendDao.insertFriendPhoto(FriendPhoto(friendId = friendId, photoBytes = bytes))
        }
        return friendId
    }

    suspend fun addPhotoToFriend(friendId: Long, photoBytes: ByteArray): Long {
        return friendDao.insertFriendPhoto(FriendPhoto(friendId = friendId, photoBytes = photoBytes))
    }

    suspend fun deleteFriend(id: Long) {
        friendDao.deleteFriendById(id)
    }

    suspend fun deletePhoto(photoId: Long) {
        friendDao.deletePhotoById(photoId)
    }
}
