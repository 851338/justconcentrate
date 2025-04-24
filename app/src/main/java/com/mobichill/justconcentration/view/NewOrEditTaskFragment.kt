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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.mobichill.justconcentration.R
import com.mobichill.justconcentration.base.BaseViewBindingFragment
import com.mobichill.justconcentration.databinding.FragmentNewOrEditTaskBinding
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.others.Constants.INTENT_EXTRA.TASK_KEY
import com.mobichill.justconcentration.others.Constants.OTHERS.TIME_FORMAT
import com.mobichill.justconcentration.util.AudioUtils
import com.mobichill.justconcentration.util.ConvertUtils
import com.mobichill.justconcentration.util.SFUtils
import com.mobichill.justconcentration.util.Utils
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class NewOrEditTaskFragment : BaseViewBindingFragment<FragmentNewOrEditTaskBinding>() {
    private var timeString: String = ""
    private var dateTime: Long = 0
    private var task: TaskModel? = null
    private var isEdit: Boolean = false
    private val taskViewModel: TaskViewModel by activityViewModels {
        (requireActivity() as TaskActivity).taskViewModelFactory
    }
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
                        checkAudioFile(audioUri)
                        binding.tvSelectedAlarmSound.text =
                            AudioUtils.getAudioNameFromUri(
                                R.string.default_alarm_sound,
                                requireContext(),
                                audioUri
                            )
                        selectedUri = audioUri
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            AudioUtils.persistUriPermission(requireActivity(), audioUri)
                        }
                    }
                }
            }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (hasUnsavedChanges()) {
                    Utils.showConfirmDialog(
                        requireActivity(),
                        getString(R.string.discard_changes),
                        getString(R.string.unsaved_changes_message),
                        getString(R.string.yes),
                        getString(R.string.cancel)
                    ) {
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

    override fun initData() {
        task = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(TASK_KEY, TaskModel::class.java) // API 33+
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(TASK_KEY) // API 24-32
        }
        isEdit = task != null
        resetData()
    }

    override fun initView() {
        // Scroll task content
        binding.etTaskTitle.movementMethod = ScrollingMovementMethod.getInstance()
        binding.etTaskTitle.isVerticalScrollBarEnabled = true

        binding.btnSave.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                // Check if user selected an already passed alarm
                if (dateTime != 0L && dateTime <= System.currentTimeMillis())
                    Utils.showToast(
                        requireContext(), getString(R.string.time_choosen_has_passed),
                    )
                else
                    if (isEdit) updateExistedTask(task!!)
                    else createNewTask()
            }
        })

        binding.btnChooseAlarm.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                AudioUtils.showSoundChoiceDialog(requireActivity(), pickAudioLauncher)
            }
        })
        binding.btnPickDateTime.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                Utils.showDateTimePicker(requireContext()) { calendar ->
                    val format = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
                    val formattedString = format.format(calendar.time)
                    timeString = formattedString
                    binding.tvSelectedDateTime.text = timeString
                    dateTime = calendar.timeInMillis
                }
            }
        })
        binding.btnReset.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    resetData()
                }
            })
    }

    private fun resetData() {
        if (isEdit) {
            binding.etTaskTitle.setText(task?.taskText)
            binding.tvSelectedDateTime.text =
                ConvertUtils.convertTimeMillisIntoText(requireContext(), task!!.alarmTimeMillis)
            binding.tvSelectedAlarmSound.text = if (task!!.alarmSoundUri.isNotEmpty())
                AudioUtils.getAudioNameFromUri(
                    R.string.default_alarm_sound,
                    requireContext(),
                    task!!.alarmSoundUri.toUri()
                )
            else getString(R.string.default_alarm_sound)
        } else {
            binding.etTaskTitle.setText("")
            binding.tvSelectedDateTime.text = getString(R.string.no_date_selected)
            binding.tvSelectedAlarmSound.text = getString(R.string.default_alarm_sound)
        }
    }

    private fun createNewTask() {
        if (binding.etTaskTitle.text.isNullOrEmpty()) {
            Utils.showToast(
                requireContext(),
                requireContext().getString(R.string.please_enter_a_task)
            )
            return
        }
        var isSynced = false
        val newTask = TaskModel(
            taskText = binding.etTaskTitle.text.toString(),
            alarmTimeMillis = dateTime,
            requestCode = Utils.getNextRequestCode(requireContext()),
            alarmSoundUri = (if (::selectedUri.isInitialized) selectedUri.toString() else ""),
            createdAt = System.currentTimeMillis()
        )
        if (Utils.isNetworkAvailable(requireContext()) && SFUtils.isUserLoggedIn(requireContext())) {
            isSynced = true
            //save to fireStore
            taskViewModel.saveTaskToFireStore(newTask.copy(isSynced = true)) { complete, error ->
                if (complete) {
                    Log.d(TAG, getString(R.string.task_created_success))
                } else {
                    Log.e(TAG, "Create task:" + error?.message.toString())
                }
            }
        } else isSynced = false
        //save to room
        taskViewModel.saveTaskToRoom(newTask.copy(isSynced = isSynced))

        setAlarm(newTask.requestCode, newTask.alarmSoundUri)

        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun updateExistedTask(taskModel: TaskModel) {
        if (binding.etTaskTitle.text.isNullOrEmpty()) {
            Utils.showToast(
                requireContext(),
                requireContext().getString(R.string.please_enter_a_task)
            )
            return
        }
        var isSynced = false
        val updatedTask = TaskModel(
            id = taskModel.id,
            taskText = binding.etTaskTitle.text.toString(),
            alarmTimeMillis = dateTime,
            requestCode = taskModel.requestCode,
            alarmSoundUri = (if (::selectedUri.isInitialized) selectedUri.toString() else "")
        )
        if (Utils.isNetworkAvailable(requireContext()) && SFUtils.isUserLoggedIn(requireContext())) {
            isSynced = true
            //update to fireStore
            taskViewModel.updateTaskToFireStore(taskModel.copy(isSynced = true)) { onComplete, error ->
                if (onComplete) {
                    Log.d(TAG, getString(R.string.task_created_success))
                } else {
                    Log.e(TAG, "Update task:" + error?.message.toString())
                }
            }
        } else isSynced = false
        //update to room
        taskViewModel.updateTaskToRoom(updatedTask.copy(isSynced = isSynced))
        if (task?.alarmTimeMillis != dateTime && task?.alarmTimeMillis != 0L) {
            cancelAlarm(taskModel.requestCode)
            setAlarm(updatedTask.requestCode, updatedTask.alarmSoundUri)
        }

        requireActivity().supportFragmentManager.popBackStack()
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

    private fun hasUnsavedChanges(): Boolean = with(binding) {
        return if (task != null) {
            val taskAlarm = ConvertUtils.convertTimeMillisIntoText(
                requireContext(), task!!.alarmTimeMillis
            )
            val taskAudio = AudioUtils.getAudioNameFromUri(
                R.string.default_alarm_sound,
                requireContext(),
                task!!.alarmSoundUri.toUri()
            )

            etTaskTitle.text.toString() != task!!.taskText ||
                    tvSelectedDateTime.text.toString() != taskAlarm ||
                    tvSelectedAlarmSound.text.toString() != taskAudio
        } else {
            etTaskTitle.text.toString().isNotEmpty() ||
                    tvSelectedDateTime.text.toString() != getString(R.string.no_date_selected) ||
                    tvSelectedAlarmSound.text.toString() != getString(R.string.default_alarm_sound)
        }
    }

    private fun checkAudioFile(uri: Uri) {
        // Show loading
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val isValid = withContext(Dispatchers.IO) {
                AudioUtils.isValidAudioFile(requireContext(), uri)
            }
            // Hide loading
            binding.progressBar.visibility = View.GONE
            if (isValid) {
                Log.d(TAG, "Audio is valid")
                binding.tvSelectedAlarmSound.error = null
            } else {
                Log.e(TAG, "Invalid audio file")
                binding.tvSelectedAlarmSound.error = "Invalid audio file. Please select another!"
            }
        }
    }

}