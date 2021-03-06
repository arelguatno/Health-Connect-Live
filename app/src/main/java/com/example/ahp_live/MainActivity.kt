package com.example.ahp_live

import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthDataRequestPermissions
import androidx.health.connect.client.permission.Permission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.time.Instant
import java.time.ZoneOffset

//https://github.com/arelguatno/Health-Connect-Live
class MainActivity : AppCompatActivity() {
    private lateinit var healthConnectClient: HealthConnectClient
    private val TAG = "MainActivity";

    private var workoutName = "Afternoon Run"

    //30 mins run
    private var startTime = "2022-05-24T01:00:00.750Z"
    private var endTime = "2022-05-24T01:30:00.750Z"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var btnInsertHeartRate = findViewById<Button>(R.id.btnInsertHeartRate)
        var btnReadHeartRate = findViewById<Button>(R.id.btnReadHeartRate)
        var btnDeleteHeartRate = findViewById<Button>(R.id.btnDeleteHeartRate)

        if (HealthConnectClient.isAvailable(this)) {
            healthConnectClient = HealthConnectClient.getOrCreate(this)
            checkPermissionsAndRun(healthConnectClient, btnInsertHeartRate,btnReadHeartRate,btnDeleteHeartRate);
        } else {
            Log.d(TAG,"Not available")
        }

        btnInsertHeartRate.setOnClickListener{
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    insertSession();
                } catch(e: Exception) {
                    // Handle exception
                    showToast(e.message);
                }
            }
        }

        btnReadHeartRate.setOnClickListener{
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    readDataRange();
                } catch(e: Exception) {
                    // Handle exception
                    showToast(e.message);
                }
            }
        }

        btnDeleteHeartRate.setOnClickListener{
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    deleteDataByRange();
                } catch(e: Exception) {
                    // Handle exception
                    showToast(e.message);
                }
            }
        }
    }

    private suspend fun insertSession() {
        val records = listOf(
            ActivitySession(
                title = workoutName,
                startTime = Instant.parse(startTime),
                startZoneOffset = ZoneOffset.UTC,
                endTime = Instant.parse(endTime),
                endZoneOffset = ZoneOffset.UTC,
                activityType = ActivitySession.ActivityType.RUNNING,
            )
        )
        val response = healthConnectClient.insertRecords(records)
        Log.d(TAG, "Saved uid: " + response.recordUidsList[0])
        showDialog("$workoutName workout is saved")
    }

    private suspend fun readDataRange(){
        var recordCount = 0;
        val response =
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ActivitySession::class,
                    timeRangeFilter = TimeRangeFilter.between(Instant.parse(startTime), Instant.parse(endTime)),
                )
            )
        response.records.forEach {
            ++recordCount
            Log.d(TAG, it.title.toString())
        }

        if (recordCount >=1){
            showDialog("Yay! We found: " + recordCount.toString() + " workout(s)")
        }else{
            showToast("No record found")
        }
    }

    private suspend fun deleteDataByRange() {
        val response = healthConnectClient.deleteRecords(
            ActivitySession::class,
            uidsList = listOf("14d84a0a-37be-4d7a-8526-768a15a2b7da"),
            clientIdsList = emptyList()
        )
        Log.d(TAG, "Session deleted");
        showDialog("$workoutName workout is deleted")
    }

    private fun checkPermissionsAndRun(client: HealthConnectClient, btnInsertHeartRate: Button, btnReadHeartRate: Button, btnDeleteHeartRate: Button) {
        val PERMISSIONS =
            setOf(
                Permission.createReadPermission(HeartRateSeries::class),
                Permission.createWritePermission(HeartRateSeries::class),
                Permission.createReadPermission(Steps::class),
                Permission.createWritePermission(Steps::class),
                Permission.createWritePermission(Distance::class),
                Permission.createReadPermission(Distance::class),
                Permission.createReadPermission(ActivitySession::class),
                Permission.createWritePermission(ActivitySession::class),
            )
        val requestPermissions =
            registerForActivityResult(HealthDataRequestPermissions()) { granted ->
                if (granted.containsAll(PERMISSIONS)) {
                    btnInsertHeartRate.isEnabled = true
                    btnReadHeartRate.isEnabled = true
                    btnDeleteHeartRate.isEnabled = true
                    Log.d(TAG,"Health Connect is available and installed"); showToast("Health Connect is available and installed");
                } else {
                    // Lack of required permissions
                }
            }
        lifecycleScope.launch {
            val granted = client.permissionController.getGrantedPermissions(PERMISSIONS)
            if (granted.containsAll(PERMISSIONS)) {
                btnInsertHeartRate.isEnabled = true
                btnReadHeartRate.isEnabled  = true
                btnDeleteHeartRate.isEnabled = true
                Log.d(TAG,"Health Connect is available and installed"); showToast("Health Connect is available and installed");
            } else {
                requestPermissions.launch(PERMISSIONS)
            }
        }
    }

    private fun showDialog(message: String){
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
        }
        builder.show()
    }

    private fun showToast(message: String?){
       Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

}