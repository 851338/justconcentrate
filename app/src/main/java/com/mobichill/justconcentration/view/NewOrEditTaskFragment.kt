package com.mobichill.justconcentration.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentNewOrEditTaskBinding
import com.mobichill.justconcentration.factory.TaskViewModelFactory
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.repository.FireStoreRepository
import com.mobichill.justconcentration.util.Constants.REQUEST_CODE_PICK_AUDIO
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.util.Utils.persistUriPermission
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    override fun initViewBinding(): FragmentNewOrEditTaskBinding =
        FragmentNewOrEditTaskBinding.inflate(layoutInflater)

    override fun initViewModel() {
        taskViewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(requireContext())
        )[TaskViewModel::class.java]
        super.initViewModel()
    }

    override fun initData() {
        task = arguments?.getParcelable("task_key")
        isEdit = task != null
    }

    override fun initView() {
        if (isEdit) {
            binding.etTaskTitle.setText(task?.taskText)
            binding.tvSelectedDateTime.text =
                Utils.convertTimeMillisIntoText(task?.alarmTimeMillis ?: 0L).toString()
        }
        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                if (isEdit) editExistedTask(task!!)
                else createNewTask()
            }
        }
        binding.btnPickDateTime.setOnClickListener { setDateTime() }
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
                requestCode = Utils.getNextRequestCode(requireContext())
            )
            if (Utils.isNetworkAvailable(requireContext())) {
                //save to fireStore
                FireStoreRepository().saveTaskToFireStore(newTask)
            }
            //save to room
            taskViewModel.saveTaskToRoom(newTask)
            setAlarm(newTask.requestCode, newTask.alarmSoundUri)
        }
    }

    private fun editExistedTask(taskModel: TaskModel) {
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
                requestCode = taskModel.requestCode
            )
            if (Utils.isNetworkAvailable(requireContext())) {
                //update to fireStore
                FireStoreRepository().updateTaskToFireStore(updatedTask)
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

    @SuppressLint("ScheduleExactAlarm")
    private fun setAlarm(requestCode: Int, alarmUri: String) {
        if (timeString.isNotEmpty()) {
            AlarmHelper().setAlarm(requireContext(), dateTime, requestCode, alarmUri)
            Utils.showToast(
                requireContext(), getString(R.string.task_created_with_alarm, timeString),
            )
        } else {
            Utils.showToast(
                requireContext(), getString(R.string.task_created_with_alarm, timeString),
            )
        }
    }

    private fun setDateTime() {
        val datePicker = MaterialDatePicker.Builder.datePicker().build()
        datePicker.show(requireActivity().supportFragmentManager, "DATE_PICKER")

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
    }

    //check if date time already passed
    private fun isPastDate(selectedDate: String): Boolean {
        val dateFormat = SimpleDateFormat(getString(R.string.time_format), Locale.getDefault())
        val date: Date? = dateFormat.parse(selectedDate)

        return (date?.time ?: 0L) <= System.currentTimeMillis() // Compare with current time
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_AUDIO && resultCode == Activity.RESULT_OK) {
            val audioUri: Uri? = data?.data
            if (audioUri != null) {
                updateTaskAlarmSound(audioUri) // Store URI in db
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    //For android 11+, without persistUriPermission(),
                    // app will lose access to the file when the app restarts.
                    persistUriPermission(requireActivity(), audioUri)
                }
            }
        }
    }

    private fun updateTaskAlarmSound(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            if (task != null) {
                val updatedTask = task!!.copy(alarmSoundUri = uri.toString())
                taskViewModel.updateTaskToRoom(updatedTask)
                FireStoreRepository().updateTaskToFireStore(updatedTask)
            }
        }
    }
}