package com.mobichill.justconcentration.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.material.datepicker.MaterialDatePicker
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentNewOrEditTaskBinding
import com.mobichill.justconcentration.factory.TaskViewModelFactory
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.util.Constants.INTENT_EXTRA.TASK_KEY
import com.mobichill.justconcentration.util.OnSingleClickListener
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.util.Utils.persistUriPermission
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NewOrEditTaskFragment : BaseViewBindingFragment<FragmentNewOrEditTaskBinding>() {
    private var timeString: String = ""
    private var dateTime: Long = 0
    private var task: TaskModel? = null
    private var isEdit: Boolean = false
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var pickAudioLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedUri: Uri

    override fun initViewBinding(): FragmentNewOrEditTaskBinding =
        FragmentNewOrEditTaskBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //must be called before onCreated() finishes, does not work in bg service
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Notifications enabled!")
            } else {
                Log.e(TAG, "User denied notifications.")
                Utils.showCustomPermissionDialog(requireContext(), requestPermissionLauncher)
            }
        }
        pickAudioLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val audioUri: Uri? = result.data?.data
                    if (audioUri != null) {
                        binding.tvSelectedAlarm.text =
                            Utils.getAudioNameFromUri(requireContext(), audioUri)
                        selectedUri = audioUri
                        checkAudioFile(audioUri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            persistUriPermission(requireActivity(), audioUri)
                        }
                    }
                }
            }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    if (activity is TaskActivity)
                        (activity as TaskActivity).showExitConfirmation {
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                } else {
                    // Let the system handle the back press
                    isEnabled = false
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        //run ads
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }

    override fun onResume() {
        super.onResume()
        if (activity is TaskActivity)
            (activity as TaskActivity).setupToolbar(
                if (isEdit) getString(R.string.edit_task_title)
                else getString(R.string.new_task_title)
            )
    }

    override fun initViewModel() {
        taskViewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(requireContext())
        )[TaskViewModel::class.java]
        super.initViewModel()
    }

    override fun initData() {
        task = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(TASK_KEY, TaskModel::class.java) // API 33+
        } else {
            arguments?.getParcelable(TASK_KEY) // API 24-32
        }
        isEdit = task != null
    }

    override fun initView() {
        resetData()
        //scroll
        binding.etTaskTitle.movementMethod = ScrollingMovementMethod.getInstance()
        binding.etTaskTitle.isVerticalScrollBarEnabled = true

        binding.btnSave.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                lifecycleScope.launch {
                    if (isEdit) updateExistedTask(task!!)
                    else createNewTask()
                }
            }
        })

        binding.btnChooseAlarm.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "audio/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                pickAudioLauncher.launch(intent)
            }
        })
        binding.btnPickDateTime.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                setDateTime()
            }
        })
        binding.btnReset.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                resetData()
            }
        })
    }

    private fun resetData() {
        if (isEdit) {
            binding.etTaskTitle.setText(task?.taskText)

            val date = Utils.convertTimeMillisIntoText(task?.alarmTimeMillis ?: 0L).toString()
            binding.tvSelectedDateTime.text = if (date.isNotEmpty()) date
            else getString(R.string.no_date_selected)

            binding.tvSelectedAlarm.text = if (task!!.alarmSoundUri.isNotEmpty())
                Utils.getAudioNameFromUri(requireContext(), task!!.alarmSoundUri.toUri())
            else getString(R.string.default_alarm_sound)
        } else {
            binding.etTaskTitle.setText("")
            binding.tvSelectedDateTime.text = getString(R.string.no_date_selected)
            binding.tvSelectedAlarm.text = getString(R.string.default_alarm_sound)
        }
    }

    private fun createNewTask() {
        if (binding.etTaskTitle.text.isNullOrEmpty())
            Utils.showToast(
                requireContext(),
                requireContext().getString(R.string.please_enter_a_task)
            )
        else {
            val newTask = TaskModel(
                taskText = binding.etTaskTitle.text.toString(),
                alarmTimeMillis = dateTime,
                requestCode = Utils.getNextRequestCode(requireContext()),
                alarmSoundUri = (if (::selectedUri.isInitialized) selectedUri.toString() else "")
            )
            if (Utils.isNetworkAvailable(requireContext())) {
                //save to fireStore
                taskViewModel.saveTaskToFireStore(newTask) { complete, error ->
                    if (complete) {
                        Log.d(TAG, getString(R.string.task_created_success))
                    } else {
                        Log.e(TAG, "Create task:" + error?.message.toString())
                    }
                }
            }
            //save to room
            taskViewModel.saveTaskToRoom(newTask)
            setAlarm(newTask.requestCode, newTask.alarmSoundUri)
        }
    }

    private fun updateExistedTask(taskModel: TaskModel) {
        if (binding.etTaskTitle.text.isNullOrEmpty())
            Utils.showToast(
                requireContext(),
                requireContext().getString(R.string.please_enter_a_task)
            )
        else {
            val updatedTask = TaskModel(
                id = taskModel.id,
                taskText = binding.etTaskTitle.text.toString(),
                alarmTimeMillis = dateTime,
                requestCode = taskModel.requestCode,
                alarmSoundUri = (if (::selectedUri.isInitialized) selectedUri.toString() else "")
            )
            if (Utils.isNetworkAvailable(requireContext())) {
                //update to fireStore
                taskViewModel.updateTaskToFireStore(taskModel) { onComplete, error ->
                    if (onComplete) {
                        Log.d(TAG, getString(R.string.task_created_success))
                    } else {
                        Log.e(TAG, "Update task:" + error?.message.toString())
                    }
                }
            }
            //update to room
            taskViewModel.updateTaskToRoom(updatedTask)
            if (task?.alarmTimeMillis != dateTime && task?.alarmTimeMillis != 0L) {
                cancelAlarm(taskModel.requestCode)
                setAlarm(updatedTask.requestCode, updatedTask.alarmSoundUri)
            }
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        AlarmHelper().cancelAlarm(requireContext(), requestCode)
    }

    private fun setAlarm(requestCode: Int, alarmUri: String) {
        if (timeString.isNotEmpty() && timeString != getString(R.string.no_date_selected)) {
            AlarmHelper().setAlarm(requireContext(), dateTime, requestCode, alarmUri)
            Utils.showToast(
                requireContext(), getString(R.string.task_created_with_alarm, timeString)
            )
        } else {
            Utils.showToast(
                requireContext(), getString(R.string.task_without_alarm)
            )
        }
    }

    private fun setDateTime() {
        val locale = Locale("en") // Use the language code you prefer, e.g., "en" for English
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        val datePicker = MaterialDatePicker.Builder.datePicker().build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat(getString(R.string.time_format), Locale.getDefault())
            timeString = sdf.format(Date(selection))
            if (isPastDate(timeString)) {
                Utils.showToast(
                    requireContext(), getString(R.string.time_choosen_has_passed),
                )
            } else {
                val date: Date? = sdf.parse(timeString)
                //save dateTime as Long to set alarm
                dateTime = date?.time ?: 0L
            }
        }
        datePicker.show(requireActivity().supportFragmentManager, "DATE_PICKER")
    }

    //check if date time already passed
    private fun isPastDate(selectedDate: String): Boolean {
        val dateFormat = SimpleDateFormat(getString(R.string.time_format), Locale.getDefault())
        val date: Date? = dateFormat.parse(selectedDate)

        return (date?.time ?: 0L) <= System.currentTimeMillis() // Compare with current time
    }

    private fun hasUnsavedChanges(): Boolean = with(binding) {
        return if (task != null) {
            etTaskTitle.text.toString() != task!!.taskText ||
                    tvSelectedDateTime.text.toString() != Utils.convertTimeMillisIntoText(task!!.alarmTimeMillis) ||
                    tvSelectedAlarm.text.toString() != Utils.getAudioNameFromUri(
                requireContext(),
                task!!.alarmSoundUri.toUri()
            )
        } else {
            etTaskTitle.text.toString().isNotEmpty() ||
                    tvSelectedDateTime.text.toString() != getString(R.string.no_date_selected) ||
                    tvSelectedAlarm.text.toString() != getString(R.string.default_alarm_sound)
        }
    }

    private fun checkAudioFile(uri: Uri) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val isValid = Utils.isValidAudioFile(requireContext(), uri)
            // Hide loading
            binding.progressBar.visibility = View.GONE
            if (isValid) {
                // Proceed with valid file
                Log.d(TAG, "Audio is valid")
                binding.tvSelectedAlarm.error = null
            } else {
                binding.tvSelectedAlarm.error = "Invalid audio file. Please select another!"
                Log.e(TAG, "Invalid audio file")
            }
        }
    }
}