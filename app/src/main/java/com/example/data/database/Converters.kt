package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.InvoiceItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    
    private val listType = Types.newParameterizedType(List::class.java, InvoiceItem::class.java)
    private val adapter = moshi.adapter<List<InvoiceItem>>(listType)

    @TypeConverter
    fun fromInvoiceItemList(value: List<InvoiceItem>?): String? {
        return value?.let { adapter.toJson(it) }
    }

    @TypeConverter
    fun toInvoiceItemList(value: String?): List<InvoiceItem>? {
        return value?.let { adapter.fromJson(it) }
    }
}
