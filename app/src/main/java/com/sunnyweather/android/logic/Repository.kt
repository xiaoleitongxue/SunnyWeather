package com.sunnyweather.android.logic

import androidx.lifecycle.liveData
import com.sunnyweather.android.logic.dao.PlaceDao
import com.sunnyweather.android.logic.model.Place
import com.sunnyweather.android.logic.model.Weather
import com.sunnyweather.android.logic.network.SunnyWeatherNetWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.Exception



object Repository {
    fun searchPlaces(query:String) = liveData(Dispatchers.IO){
       val result = try{
           val placeResponse = SunnyWeatherNetWork.searchPlaces(query)
           if(placeResponse.status == "ok"){
               val places = placeResponse.places
               Result.success(places)
           }else{
               Result.failure(RuntimeException("response status is ${placeResponse.status}"))
           }
       }catch(e:Exception){
           Result.failure<List<Place>>(e)
       }
        emit(result)

    }

    fun refreshWeather(lng :String, lat:String) = liveData(Dispatchers.IO){
        val result = try{
            coroutineScope {
                val deferredRealTime = async {
                    SunnyWeatherNetWork.getRealtimeWeather(lng,lat)
                }
                val deferredDaily = async{
                    SunnyWeatherNetWork.getDailyWeather(lng,lat)
                }
                val realtimeResponse = deferredRealTime.await()
                val dailyResponse = deferredDaily.await()
                if(realtimeResponse.status == "ok" && dailyResponse.status=="ok"){
                    val weather = Weather(realtimeResponse.result.realtime,dailyResponse.result.daily)
                    Result.success(weather)
                }else{
                    Result.failure(
                        RuntimeException("realtime response status is ${realtimeResponse.status}" +
                        "daily response status is ${dailyResponse.status}")
                    )
                }
            }

        }catch(e:Exception){
            Result.failure<Weather>(e)
        }
        emit(result)
    }
    /*
    private fun <T> fire(context: CoroutineContext,block:suspend()->Result<T>) =
        liveData<Result<T>>(context){
        val result = try{
            block()
        }catch(e:Exception){
            Result.failure<T>(e)
        }
            emit(result)
    }
     */

    fun savePlace(place : Place) = PlaceDao.savePace(place)
    fun getPlace() : Place = PlaceDao.getSavedPlace()
    fun isPlaceSaved() = PlaceDao.isPlaceSaved()
}