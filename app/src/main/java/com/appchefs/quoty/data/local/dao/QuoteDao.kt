package com.appchefs.quoty.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appchefs.quoty.data.model.Quote

@Dao
interface QuoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addQuote(quote: Quote)

    @Query("SELECT * FROM ${Quote.TABLE_NAME} WHERE ID = :quoteId")
    fun getQuote(quoteId: String): kotlinx.coroutines.flow.Flow<Quote>

    @Query("SELECT * FROM ${Quote.TABLE_NAME}")
    suspend fun getAllQuotes() : List<Quote>
}