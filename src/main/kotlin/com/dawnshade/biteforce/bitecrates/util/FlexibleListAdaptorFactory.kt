package com.dawnshade.biteforce.bitecrates.util

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.MalformedJsonException
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


internal class FlexibleListAdaptorFactory<E> private constructor() : TypeAdapterFactory {
    override fun <T> create(gson: Gson, typeToken: TypeToken<T>): TypeAdapter<T>? {
        
        if (!MutableList::class.java.isAssignableFrom(typeToken.getRawType())) {
            return null
        }
        
        val elementType: Type = resolveTypeArgument(typeToken.getType())
        val elementTypeAdapter: TypeAdapter<E> = gson.getAdapter(TypeToken.get(elementType)) as TypeAdapter<E>
        
        return ListLikeAdaptorFactory(elementTypeAdapter).nullSafe() as TypeAdapter<T>
    }

    companion object {
        private fun resolveTypeArgument(type: Type): Type {
            
            if (type !is ParameterizedType) {
                
                return Any::class.java
            }
            val parameterizedType = type as ParameterizedType
            return parameterizedType.actualTypeArguments[0]
        }

        private class ListLikeAdaptorFactory<E> constructor(elementTypeAdapter: TypeAdapter<E>) : TypeAdapter<List<E>?>() {
            private val elementTypeAdapter: TypeAdapter<E>

            init {
                this.elementTypeAdapter = elementTypeAdapter
            }

            override fun write(out: JsonWriter, list: List<E>?) {
                if (list == null) {
                    out.nullValue()
                    return
                }

                if (list.size == 1) {
                    elementTypeAdapter.write(out, list[0])
                } else {
                    out.beginArray()
                    for (element in list) {
                        elementTypeAdapter.write(out, element)
                    }
                    out.endArray()
                }
            }

            @Throws(IOException::class)
            override fun read(`in`: JsonReader): List<E> {
                
                val list: MutableList<E> = kotlin.collections.ArrayList()
                val token: JsonToken = `in`.peek()
                when (token) {
                    JsonToken.BEGIN_ARRAY -> {
                        
                        `in`.beginArray()
                        while (`in`.hasNext()) {
                            list.add(elementTypeAdapter.read(`in`))
                        }
                        `in`.endArray()
                    }

                    JsonToken.BEGIN_OBJECT, JsonToken.STRING, JsonToken.NUMBER, JsonToken.BOOLEAN ->
                        list.add(elementTypeAdapter.read(`in`))

                    JsonToken.NULL -> throw kotlin.AssertionError("Must never happen: check if the type adapter configured with .nullSafe()")
                    JsonToken.NAME, JsonToken.END_ARRAY, JsonToken.END_OBJECT, JsonToken.END_DOCUMENT -> throw MalformedJsonException("Unexpected token: $token")
                    else -> throw kotlin.AssertionError("Must never happen: $token")
                }
                return list
            }
        }
    }
}