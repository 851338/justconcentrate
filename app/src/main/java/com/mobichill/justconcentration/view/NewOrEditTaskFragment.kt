package com.mobichill.justconcentration.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
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
import com.mobichill.justconcentration.constants.Constants.INTENT_EXTRA.TASK_KEY
import com.mobichill.justconcentration.constants.Constants.OTHERS.TIME_FORMAT
import com.mobichill.justconcentration.databinding.FragmentNewOrEditTaskBinding
import com.mobichill.justconcentration.helper.AlarmHelper
import com.mobichill.justconcentration.listener.OnSingleClickListener
import com.mobichill.justconcentration.model.TaskModel
import com.mobichill.justconcentration.utils.AudioUtils
import com.mobichill.justconcentration.utils.ConvertUtils
import com.mobichill.justconcentration.utils.SharedPreferencesUtils
import com.mobichill.justconcentration.utils.Utils
import com.mobichill.justconcentration.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NewOrEditTaskFragment : BaseViewBindingFragment<FragmentNewOrEditTaskBinding>() {
    private var timeString: String = ""
    private var dateTime: Long = 0
    private var task: TaskModel? = null
    private var isEdit: Boolean = false
    private val taskViewModel: TaskViewModel by activityViewModels {
        (requireActivity() as TaskActivity).taskViewModelFactory
    }
    private val sfUtils: SharedPreferencesUtils by lazy {
        SharedPreferencesUtils(requireActivity())
    }

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var pickAudioLauncher: ActivityResultLauncher<Intent>
    private lateinit var selectedUri: Uri
    private var isPickingSystemRingtone = false

    override fun initViewBinding(): FragmentNewOrEditTaskBinding =
        FragmentNewOrEditTaskBinding.inflate(layoutInflater)

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) = with(binding) {
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
        // Use one register for both ringtone and audio picker
        pickAudioLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val resultData = result.data ?: return@registerForActivityResult

                    val audioUri: Uri? =
                        if (isPickingSystemRingtone) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                                resultData.getParcelableExtra(
                                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                                    Uri::class.java
                                )
                            else
                                @Suppress("DEPRECATION")
                                resultData.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                        } else resultData.data

                    if (audioUri != null) {
                        checkAndSaveAudioFile(audioUri)
                        selectedUri = audioUri
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            AudioUtils.persistUriPermission(requireActivity(), audioUri)
                        }
                    }
                }
            }

        //run ads
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
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

    override fun initView() = with(binding) {
        // Scroll task content
        etTaskTitle.movementMethod = ScrollingMovementMethod.getInstance()
        etTaskTitle.isVerticalScrollBarEnabled = true

        btnSave.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                // Make sure dateTime is always the new set one
                dateTime =
                    ConvertUtils.convertTextIntoTimeMillis(tvSelectedDateTime.text.toString())
                // Check if user selected an already passed alarm
                if (!isEdit && dateTime != 0L && dateTime <= System.currentTimeMillis())
                    Utils.showToast(
                        requireContext(), getString(R.string.time_choosen_has_passed),
                    )
                else
                    if (isEdit) updateExistedTask(task!!)
                    else createNewTask()
            }
        })

        btnChooseAlarm.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                AudioUtils.showSoundChoiceDialog(requireActivity(), pickAudioLauncher)
            }
        })
        btnPickDateTime.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(view: View) {
                Utils.showDateTimePicker(requireContext()) { calendar ->
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val format = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
                    val formattedString = format.format(calendar.time)
                    timeString = formattedString
                    tvSelectedDateTime.text = timeString
                    dateTime = calendar.timeInMillis
                }
            }
        })
        btnReset.setOnClickListener(
            object : OnSingleClickListener() {
                override fun onSingleClick(view: View) {
                    resetData()
                }
            })
    }

    private fun resetData() = with(binding) {
        if (isEdit) {
            etTaskTitle.setText(task?.taskText)
            tvSelectedDateTime.text =
                ConvertUtils.convertTimeMillisIntoText(requireContext(), task!!.alarmTimeMillis)

            tvSelectedAlarmSound.text = if (task!!.alarmSoundUri.isNotEmpty()) {
                selectedUri = task!!.alarmSoundUri.toUri()
                AudioUtils.getAudioNameFromUri(
                    R.string.unknown_audio_file,
                    requireContext(),
                    task!!.alarmSoundUri.toUri()
                )
            } else {
                selectedUri = AudioUtils.defaultAlarmUri(requireContext())
                AudioUtils.defaultAlarmName(requireContext())
            }
        } else {
            etTaskTitle.setText("")
            tvSelectedDateTime.text = getString(R.string.no_date_selected)
            tvSelectedAlarmSound.text = AudioUtils.defaultAlarmName(requireContext())
            selectedUri = AudioUtils.defaultAlarmUri(requireContext())
        }
    }

    private fun createNewTask() = with(binding) {
        if (etTaskTitle.text.isNullOrEmpty()) {
            etTaskTitle.error = requireContext().getString(R.string.please_enter_task_title)
            etTaskTitle.requestFocus()
            return
        }
        val newTask = TaskModel(
            taskText = etTaskTitle.text.toString(),
            alarmTimeMillis = dateTime,
            requestCode = Utils.getNextRequestCode(requireContext()),
            alarmSoundUri = (if (::selectedUri.isInitialized) selectedUri.toString()
            else AudioUtils.defaultAlarmUri(requireContext()).toString()),
            createdAt = System.currentTimeMillis()
        )
        var isSyncedSuccessfully = false
        if (sfUtils.isUserLoggedIn() && Utils.isNetworkAvailable(requireContext())) {
            try {
                Log.d(TAG, "Attempting FireStore sync for session ${newTask.id}")
                // Sync the potentially modified sessionToSave
                taskViewModel.saveTaskToFireStore(newTask.copy(isSynced = true))
                Log.d(TAG, "FireStore sync SUCCESS for session ${newTask.id}")
                isSyncedSuccessfully = true // Mark as synced ONLY if FireStore call succeeds
            } catch (e: Exception) {
                Log.e(TAG, "FireStore sync FAILED for session ${newTask.id}", e)
                isSyncedSuccessfully = false // Ensure it's false on FireStore failure
            }
        } else {
            Log.d(TAG, "Skipping FireStore sync (Conditions not met) for session ${newTask.id}")
            isSyncedSuccessfully = false // Explicitly false if conditions aren't met
        }
        try {
            Log.d(TAG, "Saving final state to Room ${newTask.id}, Synced: $isSyncedSuccessfully)")
            taskViewModel.saveTaskToRoom(newTask.copy(isSynced = isSyncedSuccessfully))
            setAlarm(newTask.requestCode, newTask.alarmSoundUri)
            Log.d(TAG, "Room save successful for task ${newTask.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Room save FAILED for session ${newTask.id}", e)
        }

        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun updateExistedTask(taskModel: TaskModel) = with(binding) {
        if (etTaskTitle.text.isNullOrEmpty()) {
            Utils.showToast(
                requireContext(),
                requireContext().getString(R.string.please_enter_task_title)
            )
            return
        }
        val updatedTask = taskModel.copy(
            id = taskModel.id,
            taskText = etTaskTitle.text.toString(),
            alarmTimeMillis = dateTime,
            requestCode = taskModel.requestCode,
            alarmSoundUri = (if (::selectedUri.isInitialized) selectedUri.toString()
            else AudioUtils.defaultAlarmUri(requireContext()).toString()),
        )
        var isSyncedSuccessfully = false
        if (sfUtils.isUserLoggedIn() && Utils.isNetworkAvailable(requireContext())) {
            try {
                Log.d(TAG, "Attempting FireStore sync for session ${taskModel.id}")
                // Sync the potentially modified sessionToSave
                taskViewModel.updateTaskToFireStore(updatedTask.copy(isSynced = true))
                Log.d(TAG, "FireStore sync SUCCESS for session ${taskModel.id}")
                isSyncedSuccessfully = true // Mark as synced ONLY if FireStore call succeeds
            } catch (e: Exception) {
                Log.e(TAG, "FireStore sync FAILED for session ${taskModel.id}", e)
                isSyncedSuccessfully = false // Ensure it's false on FireStore failure
            }
        } else {
            Log.d(TAG, "Skipping FireStore sync (Conditions not met) for session ${taskModel.id}")
            isSyncedSuccessfully = false // Explicitly false if conditions aren't met
        }
        try {
            Log.d(TAG, "Saving final state to Room ${taskModel.id}, Synced: $isSyncedSuccessfully)")
            taskViewModel.updateTaskToRoom(
                updatedTask.copy(
                    isSynced = isSyncedSuccessfully,
                    lastModified = System.currentTimeMillis()
                )
            )
            if (task?.alarmTimeMillis != dateTime) {
                AlarmHelper().cancelAlarm(requireContext(), taskModel.requestCode)
                setAlarm(updatedTask.requestCode, updatedTask.alarmSoundUri)
            }
            Log.d(TAG, "Room save successful for task ${taskModel.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Room save FAILED for session ${taskModel.id}", e)
        }

        requireActivity().supportFragmentManager.popBackStack()
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
        val defaultAlarmName = AudioUtils.defaultAlarmName(requireContext())
        return if (task != null) {
            val taskAlarm = ConvertUtils.convertTimeMillisIntoText(
                requireContext(), task!!.alarmTimeMillis
            )
            val taskAudio = AudioUtils.getAudioNameFromUri(
                R.string.unknown_audio_file,
                requireContext(),
                task!!.alarmSoundUri.toUri()
            )

            etTaskTitle.text.toString() != task!!.taskText ||
                    tvSelectedDateTime.text.toString() != taskAlarm ||
                    tvSelectedAlarmSound.text.toString() != taskAudio
        } else {
            etTaskTitle.text.toString().isNotEmpty() ||
                    tvSelectedDateTime.text.toString() != getString(R.string.no_date_selected) ||
                    tvSelectedAlarmSound.text.toString() != defaultAlarmName
        }
    }

    fun checkAndSaveAudioFile(uri: Uri) = with(binding) {
        // Show loading
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val isValid = withContext(Dispatchers.IO) {
                AudioUtils.isValidAudioFile(requireContext(), uri)
            }
            // Hide loading
            progressBar.visibility = View.GONE
            if (isValid) {
                selectedUri = uri
                Log.d(TAG, "Audio is valid")
                tvSelectedAlarmSound.text =
                    AudioUtils.getAudioNameFromUri(
                        R.string.unknown_audio_file,
                        requireContext(),
                        uri
                    )
                tvSelectedAlarmSound.error = null
            } else {
                Log.e(TAG, "Invalid audio file")
                tvSelectedAlarmSound.error = "Invalid audio file. Please select another!"
            }
        }
    }
}