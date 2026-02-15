package com.github.logviewer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.logviewer.databinding.LogcatViewerFragmentLogcatBinding
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class LogcatFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    companion object {
        @JvmStatic
        fun newInstance(excludeList: List<Pattern> = emptyList()): LogcatFragment {
            val args = Bundle()
            args.putStringArrayList(LogcatReader.EXCLUDE_LIST_KEY, ArrayList(excludeList.map { it.pattern() }))
            val fragment = LogcatFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private val logcatReader = LogcatReader()
    private val exportLogUtils = ExportLogFileUtils()
    private lateinit var binding: LogcatViewerFragmentLogcatBinding
    private val excludeList: MutableList<Pattern> = ArrayList()
    private val adapter = LogcatAdapter()
    private lateinit var launcher: ActivityResultLauncher<Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getStringArrayList(LogcatReader.EXCLUDE_LIST_KEY)?.let {
            for (pattern in it) {
                excludeList.add(Pattern.compile(pattern))
            }
        }
        launcher = registerForActivityResult(RequestOverlayPermission(requireContext())) { result ->
            if (result) {
                FloatingLogcatService.launch(requireContext(), excludeList)
                activity?.finish()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LogcatViewerFragmentLogcatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarHeight: Int
            val navigationBarHeight: Int
            insets.getInsets(WindowInsetsCompat.Type.systemBars()).apply {
                statusBarHeight = top
                navigationBarHeight = bottom
            }
            binding.toolbar.updateLayoutParams<LinearLayout.LayoutParams> {
                topMargin = statusBarHeight
            }
            binding.list.updatePadding(bottom = navigationBarHeight)
            insets
        }
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        binding.toolbar.setOnMenuItemClickListener(this)
        val spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.logcat_viewer_logcat_spinner, R.layout.logcat_viewer_item_logcat_dropdown
        )
        spinnerAdapter.setDropDownViewResource(R.layout.logcat_viewer_item_logcat_dropdown)
        binding.spinner.adapter = spinnerAdapter
        binding.spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val filter = resources
                        .getStringArray(R.array.logcat_viewer_logcat_spinner)[position]
                    adapter.filter.filter(filter)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }

        binding.list.transcriptMode = ListView.TRANSCRIPT_MODE_NORMAL
        binding.list.isStackFromBottom = true
        binding.list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        logcatReader.startReadLogcat(adapter, excludeList, lifecycleScope)
    }

    override fun onPause() {
        super.onPause()
        logcatReader.stopReadLogcat()
    }

    override fun onMenuItemClick(item: MenuItem) = when (item.itemId) {
        R.id.clear -> {
            adapter.clear()
            true
        }
        R.id.export -> {
            lifecycleScope.launch {
                try {
                    exportLogUtils.exportLog(requireContext(), adapter.data ,binding.root)
                } catch (e: Exception){
                    Log.d("LogcatFragment", e.message, e)
                    e.printStackTrace()
                }
            }
            true
        }
        R.id.floating -> {
            launcher.launch(Unit)
            true
        }
        else -> {
            false
        }
    }
}
