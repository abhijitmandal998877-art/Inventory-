package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShopOwner
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopOwnerDao {
    @Query("SELECT * FROM shop_owners WHERE email = :email LIMIT 1")
    suspend fun getOwnerByEmail(email: String): ShopOwner?

    @Query("SELECT * FROM shop_owners WHERE email = :email LIMIT 1")
    fun getOwnerFlowByEmail(email: String): Flow<ShopOwner?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOwner(owner: ShopOwner)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateOwner(owner: ShopOwner)
}
