package de.markusfisch.android.shadereditor.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.fragment.app.Fragment
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.activity.LoadSampleActivity
import de.markusfisch.android.shadereditor.adapter.SamplesAdapter

class LoadSampleFragment : Fragment() {

    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_samples, container, false)
        listView = view.findViewById(R.id.samples)
        initListView(requireContext())
        return view
    }

    private fun initListView(context: Context) {
        val samplesAdapter = SamplesAdapter(context)
        listView.adapter = samplesAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            loadSample(samplesAdapter.getItem(position))
        }
    }

    private fun loadSample(sample: SamplesAdapter.Sample) {
        val activity = requireActivity()
        LoadSampleActivity.setSampleResult(
            activity, sample.name, sample.resId, sample.thumbId, sample.quality
        )
        activity.finish()
    }
}