package com.stho.mehere

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.stho.mehere.databinding.FragmentPointDialogBinding
import java.util.*


class EditPointDialogFragment(private val point: PointOfInterest) : DialogFragment() {

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

        binding.infoHeader.text = displayInfoString(point)
        binding.editName.setText(point.name);
        binding.editDescription.setText(point.description);
        binding.viewLatitude.setText(formatLatitude(point.location))
        binding.viewLongitude.setText(formatLongitude(point.location))
        binding.viewAltitude.setText(formatAltitude(point.location))
        binding.menuTypeDropDown.setAdapter(requireContext(), PointOfInterest.Type.values())
        binding.menuTypeDropDown.setValue(point.type);

        return binding.root
    }

    private fun displayInfoString(point: PointOfInterest): String =
        resources.getString(R.string.label_point_info_with_parameter,
            formatDateTime(point.createdAt),
            formatDateTime(point.lastModifiedAt))


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.also {
            it.setNavigationOnClickListener { dismiss() }
            it.title = "Edit Point"
            it.inflateMenu(R.menu.menu_edit_point_dialog)
            it.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_save -> save()
                    R.id.action_delete -> delete()
                    else -> dismiss()
                }
                true
            }
        }
    }

    private fun save() {
        try {
            toPoint().also {
                if (it.isDifferentFrom(point)) {
                    viewModel.update(point.id, it)
                }
                dismiss()
            }
        }
        catch(ex: Exception) {
            showError(ex)
        }
    }

    private fun delete() {
        try {
            viewModel.delete(point.id)
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

        return PointOfInterest.update(point,
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

        fun display(activity: FragmentActivity, point: PointOfInterest): EditPointDialogFragment =
            EditPointDialogFragment(point).also {
                    it.show(activity.supportFragmentManager, TAG)
            }
    }
}

private fun PointOfInterest.isDifferentFrom(otherPoint: PointOfInterest): Boolean =
    name != otherPoint.name || description != otherPoint.description || type != otherPoint.type



