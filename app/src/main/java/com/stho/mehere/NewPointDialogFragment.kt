package com.stho.mehere

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.stho.mehere.databinding.FragmentPointDialogBinding
import java.util.*


class NewPointDialogFragment(
    private val location: Location,
    private val defaultType: PointOfInterest.Type) : DialogFragment()
{
    private lateinit var binding: FragmentPointDialogBinding

    private val viewModel: NewPointDialogViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_MyApp_FullScreenDialog);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentPointDialogBinding.inflate(inflater, container, false)

        binding.infoHeader.setText(R.string.label_new_point_info)
        binding.viewLatitude.setText(formatLatitude(location))
        binding.viewLongitude.setText(formatLongitude(location))
        binding.viewAltitude.setText(formatAltitude(location))
        binding.menuTypeDropDown.setAdapter(requireContext(), PointOfInterest.Type.values())
        binding.menuTypeDropDown.setValue(defaultType);

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.also {
            it.setNavigationOnClickListener { dismiss() }
            it.title = "New Point"
            it.inflateMenu(R.menu.menu_new_point_dialog)
            it.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save -> save()
                    else -> dismiss()
                }
                true
            }
        }
    }

    private fun save() {
        try {
            viewModel.create(toPoint())
            dismiss()
        }
        catch(ex: Exception) {
            showError(ex)
        }
    }

    private fun toPoint(): PointOfInterest {
        val name: String = binding.editName.getText()
        val description: String = binding.editName.getText()
        val typeStr: String = binding.menuType.getText()

        if (typeStr.isBlank()) {
            throw InputMismatchException("Missing type")
        }

        return PointOfInterest.create(
            location,
            name,
            description,
            PointOfInterest.Type.parseString(typeStr))
    }


    override fun onStart() {
        super.onStart()
        dialog?.also {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            it.window?.apply {
                setLayout(width, height)
                setWindowAnimations(R.style.Theme_MyApp_Slide);
            }
        }
    }

    private fun showError(exception: java.lang.Exception) {
        requireActivity().showError(requireView(), exception)
    }

    companion object {
        private const val TAG = "PointDialog"

        fun display(activity: FragmentActivity, location: Location, defaultType: PointOfInterest.Type): NewPointDialogFragment =
            NewPointDialogFragment(location, defaultType).also {
                    it.show(activity.supportFragmentManager, TAG)
            }
    }
}

