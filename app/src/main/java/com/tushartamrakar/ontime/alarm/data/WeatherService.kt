package com.tushartamrakar.ontime.alarm.data.weather

import org.json.JSONObject
import java.net.URL

data class WeatherData(
    val temp: Int,
    val tempMax: Int,
    val tempMin: Int,
    val condition: String,
    val description: String,
    val aqi: Int,
)

object WeatherService {

    private const val API_KEY = "29593664c23f55f1cfbf68cec9ef972f"

    fun getWeather(lat: Double, lon: Double): WeatherData? {
        return try {
            val weatherUrl = "https://api.openweathermap.org/data/2.5/weather" +
                    "?lat=$lat&lon=$lon&appid=$API_KEY&units=metric"
            val weatherJson = URL(weatherUrl).readText()
            val weatherObj = JSONObject(weatherJson)

            val main = weatherObj.getJSONObject("main")
            val temp = main.getDouble("temp").toInt()
            val tempMax = main.getDouble("temp_max").toInt()
            val tempMin = main.getDouble("temp_min").toInt()

            val weatherArr = weatherObj.getJSONArray("weather")
            val weatherItem = weatherArr.getJSONObject(0)
            val condition = weatherItem.getString("main")
            val description = weatherItem.getString("description")

            // ─── Get AQI ──────────────────────────────────────────────────────
            val aqiUrl = "https://api.openweathermap.org/data/2.5/air_pollution" +
                    "?lat=$lat&lon=$lon&appid=$API_KEY"
            val aqiJson = URL(aqiUrl).readText()
            val aqiObj = JSONObject(aqiJson)
            val aqi = aqiObj
                .getJSONArray("list")
                .getJSONObject(0)
                .getJSONObject("main")
                .getInt("aqi")

            WeatherData(
                temp = temp,
                tempMax = tempMax,
                tempMin = tempMin,
                condition = condition,
                description = description,
                aqi = aqi,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun buildWeatherAnnouncement(data: WeatherData): String {
        val moodPhrase = when (data.condition) {
            "Clear" -> "It's a beautiful sunny day outside!"
            "Clouds" -> "It's cloudy outside today."
            "Rain" -> "It's raining outside right now. Don't forget your umbrella!"
            "Drizzle" -> "Light drizzle outside. Carry an umbrella just in case!"
            "Thunderstorm" -> "Heavy thunderstorm today! Stay indoors if possible!"
            "Snow" -> "It's snowing outside! Roads may be slippery, drive carefully!"
            "Fog", "Mist", "Haze" -> "Foggy conditions outside. Low visibility today, drive carefully!"
            "Dust", "Sand" -> "Dusty and sandy conditions outside today!"
            "Smoke" -> "Smoky conditions outside. Air may be hazy!"
            else -> "Here are the current weather conditions outside."
        }

        val aqiPhrase = when (data.aqi) {
            1 -> "Air quality is excellent today!"
            2 -> "Air quality is good today."
            3 -> "Air quality is moderate today."
            4 -> "Air quality is poor today. Consider wearing a mask if going outside."
            5 -> "Air quality is very poor today. Avoid outdoor activities if possible!"
            else -> ""
        }

        return "Here is today's weather update. " +
                "$moodPhrase " +
                "Current temperature is ${data.temp} degrees Celsius. " +
                "Today's high is ${data.tempMax} degrees " +
                "and the low is ${data.tempMin} degrees. " +
                "$aqiPhrase"
    }
}