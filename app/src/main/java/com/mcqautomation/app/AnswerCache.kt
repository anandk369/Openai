
package com.mcqautomation.app

import android.content.Context
import androidx.room.*
import androidx.room.Room

@Entity(tableName = "cached_answers")
data class CachedAnswer(
    @PrimaryKey val questionHash: String,
    val answer: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface AnswerDao {
    @Query("SELECT * FROM cached_answers WHERE questionHash = :questionHash")
    suspend fun getAnswer(questionHash: String): CachedAnswer?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: CachedAnswer)
    
    @Query("DELETE FROM cached_answers WHERE timestamp < :cutoffTime")
    suspend fun deleteOldAnswers(cutoffTime: Long)
}

@Database(
    entities = [CachedAnswer::class],
    version = 1,
    exportSchema = false
)
abstract class AnswerDatabase : RoomDatabase() {
    abstract fun answerDao(): AnswerDao
    
    companion object {
        @Volatile
        private var INSTANCE: AnswerDatabase? = null
        
        fun getDatabase(context: Context): AnswerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnswerDatabase::class.java,
                    "answer_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class AnswerCache(context: Context) {
    private val database = AnswerDatabase.getDatabase(context)
    private val answerDao = database.answerDao()
    
    suspend fun getAnswer(questionHash: String): String? {
        return answerDao.getAnswer(questionHash)?.answer
    }
    
    suspend fun saveAnswer(questionHash: String, answer: String) {
        answerDao.insertAnswer(CachedAnswer(questionHash, answer))
    }
    
    suspend fun cleanOldAnswers() {
        val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        answerDao.deleteOldAnswers(oneWeekAgo)
    }
}
