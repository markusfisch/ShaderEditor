package de.markusfisch.android.shadereditor.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity
import de.markusfisch.android.shadereditor.activity.AddUniformActivity
import de.markusfisch.android.shadereditor.adapter.PresetUniformAdapter

class UniformPresetPageFragment : AddUniformPageFragment() {

    private lateinit var uniformsAdapter: PresetUniformAdapter
    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_uniform_preset_page, container, false)

        val activity = requireActivity()
        listView = view.findViewById(R.id.uniforms)
        initListView(activity)

        return view
    }

    override fun onSearch(query: String?) {
        uniformsAdapter.filter.filter(query) { uniformsAdapter.notifyDataSetChanged() }
    }

    private fun initListView(context: Context) {
        uniformsAdapter = PresetUniformAdapter(context)

        listView.adapter = uniformsAdapter
        listView.setOnItemClickListener { _, view, position, _ ->
            if (view.isEnabled) {
                addUniform(uniformsAdapter.getItem(position))
            }
        }
    }

    private fun addUniform(uniform: PresetUniformAdapter.Uniform) {
        if (uniform.isSampler) {
            AbstractSubsequentActivity.addFragment(
                requireParentFragment().parentFragmentManager,
                TextureParametersFragment.newInstance(uniform.type, uniform.name)
            )
        } else {
            val activity = activity ?: return
            AddUniformActivity.setAddUniformResult(
                activity,
                "uniform ${uniform.type} ${uniform.name};"
            )
            activity.finish()
        }
    }
}