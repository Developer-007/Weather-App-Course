package com.myapps.weatherappcourse.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.myapps.weatherappcourse.DialogManager
import com.myapps.weatherappcourse.MainViewModel
import com.myapps.weatherappcourse.R
import com.myapps.weatherappcourse.adapters.WeatherAdapter
import com.myapps.weatherappcourse.adapters.WeatherModel
import com.myapps.weatherappcourse.databinding.FragmentMainBinding
import com.squareup.picasso.Picasso
import org.json.JSONArray
import org.json.JSONObject

const val API_KEY = "06eb378d679444ce9c2121411231405"
class MainFragment : Fragment() {
    private lateinit var fLocationClient: FusedLocationProviderClient
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding
    private lateinit var adapter: WeatherAdapter
    private val model: MainViewModel by activityViewModels()
    private val hModel: MainViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        initRecView()
        hModel.liveDataCurrent.observe(viewLifecycleOwner){
            adapter.submitList(getHoursList(it))
        }
        updateCurrentCard()

    }

    override fun onResume() {
        super.onResume()
        checkLocation()
    }

    private fun initRecView()=with(binding){
        fLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        recView.layoutManager = LinearLayoutManager(activity)
        adapter = WeatherAdapter()
        recView.adapter = adapter
        ibSync.setOnClickListener {
            checkLocation()
        }
        ibSearch.setOnClickListener {
            DialogManager.searchByNameDialog(requireContext(), object: DialogManager.Listener{
                override fun onClick(name: String?) {
                    if (name != null) {
                        requestWeatherData(name)
                    }
                }
            })
        }
    }

    private fun getLocation(){
        val ct=CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fLocationClient
            .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, ct.token)
            .addOnCompleteListener {
                requestWeatherData("${it.result.latitude},${it.result.longitude}")
            }
    }

    private fun checkLocation(){
        if(isLocationEnabled()){
            getLocation()
        }
        else {
            DialogManager.slocationSettingsDialog(requireContext(), object: DialogManager.Listener
            {
                override fun onClick(name: String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }

            })
        }
    }

    private fun isLocationEnabled(): Boolean{
        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getHoursList(wItem: WeatherModel): List<WeatherModel>{
        val hoursArray = JSONArray(wItem.hours)
        val list = ArrayList<WeatherModel>()
        for (i in 0 until hoursArray.length()){
            val item = WeatherModel(
                "",
                (hoursArray[i] as JSONObject).getString("time"),
                (hoursArray[i] as JSONObject).getJSONObject("condition").getString("text"),
                (hoursArray[i] as JSONObject).getString("temp_c"),
                "",
                "",
                (hoursArray[i] as JSONObject).getJSONObject("condition").getString("icon"),
                ""
            )
            list.add(item)
        }
        return list
    }

    private fun updateCurrentCard()=with(binding){
        model.liveDataCurrent.observe(viewLifecycleOwner){
            val maxMinTemp = "${it.maxTemp}C°/${it.minTemp}C°"
            tvData.text=it.time
            Picasso.get().load("https:" + it.imageUrl).into(imWeather)
            tvCity.text=it.city
            tvCurrentTemp.text=it.currentTemp
            tvCondition.text=it.condition
            tvMaxMin.text=maxMinTemp
        }
    }

    private fun permissionListener(){
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()){
            Toast.makeText(activity,"Permission is $it", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission(){
        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestWeatherData(city: String){
        val url = "https://api.weatherapi.com/v1/forecast.json?key=" +
                API_KEY +
                "&q=" +
                city +
                "&days=" +
                "3" +
                "&aqi=no&alerts=no"
        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,{
                result -> parseWeatherData(result)
            }, {
                error -> Log.d("MyLog","Error: $error")
            }
        )
        queue.add(request)
    }

    private fun parseWeatherData(result: String){
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
    }

    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel){
        val item =WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current").getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c"),
            weatherItem.maxTemp,
            weatherItem.minTemp,
            mainObject.getJSONObject("current").getJSONObject("condition").getString("icon"),
            weatherItem.hours
        )
        model.liveDataCurrent.value=item

        Log.d("MyLog","City: ${item.city}")
        Log.d("MyLog","Time: ${item.time}")
        Log.d("MyLog","Condition: ${item.condition}")
        Log.d("MyLog","Temp: ${item.currentTemp}")
        Log.d("MyLog","Image URL: ${item.imageUrl}")
        Log.d("MyLog","Max temp: ${item.maxTemp}")
        Log.d("MyLog","Min temp: ${item.minTemp}")
        Log.d("MyLog","Hours: ${item.hours}")
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel>{
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")
        for(i in 0 until daysArray.length()){
            val day = daysArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition").getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c"),
                day.getJSONObject("day").getString("mintemp_c"),
                day.getJSONObject("day").getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        return list
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}