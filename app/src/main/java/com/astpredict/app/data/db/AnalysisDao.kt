package com.astpredict.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {
    @Query("SELECT * FROM analysis_history ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analysis_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAnalyses(limit: Int): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM analysis_history WHERE id = :id")
    suspend fun getAnalysisById(id: Long): AnalysisEntity?

    @Query("SELECT COUNT(*) FROM analysis_history")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT SUM(totalDetections) FROM analysis_history")
    fun getTotalDetections(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(analysis: AnalysisEntity): Long

    @Delete
    suspend fun delete(analysis: AnalysisEntity): Int

    @Query("DELETE FROM analysis_history")
    suspend fun deleteAll(): Int

    @Query("SELECT * FROM analysis_history WHERE dominantSpecies LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchBySpecies(query: String): Flow<List<AnalysisEntity>>
}
