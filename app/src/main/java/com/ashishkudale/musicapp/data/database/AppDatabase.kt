package com.ashishkudale.musicapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ashishkudale.musicapp.data.database.dao.PlaylistDao
import com.ashishkudale.musicapp.data.database.dao.PlaylistSongDao
import com.ashishkudale.musicapp.data.database.entities.Playlist
import com.ashishkudale.musicapp.data.database.entities.PlaylistSong

@Database(
    entities = [Playlist::class, PlaylistSong::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongDao(): PlaylistSongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music_player_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
