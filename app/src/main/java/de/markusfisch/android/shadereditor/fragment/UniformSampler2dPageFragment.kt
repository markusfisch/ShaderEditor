package de.markusfisch.android.shadereditor.fragment

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.activity.AddUniformActivity
import de.markusfisch.android.shadereditor.adapter.TextureAdapter
import de.markusfisch.android.shadereditor.app.ShaderEditorApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

open class UniformSampler2dPageFragment : AddUniformPageFragment() {

    var searchQuery: String? = null
        private set
    private lateinit var listView: ListView
    private var texturesAdapter: TextureAdapter? = null
    private lateinit var progressBar: View
    private lateinit var noTexturesMessage: View
    private var samplerType = AbstractSamplerPropertiesFragment.SAMPLER_2D

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_uniform_sampler_2d_page, container, false)

        val fab = view.findViewById<View>(R.id.add_texture)
        fab.setOnClickListener { addTexture() }

        listView = view.findViewById(R.id.textures)
        initListView(view)

        progressBar = view.findViewById(R.id.progress_bar)
        noTexturesMessage = view.findViewById(R.id.no_textures_message)

        return view
    }

    override fun onResume() {
        super.onResume()
        loadTexturesAsync(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        texturesAdapter?.changeCursor(null)
    }

    fun setSamplerType(type: String) {
        samplerType = type
    }

    protected open fun addTexture() {
        (activity as? AddUniformActivity)?.startPickImage()
    }

    private fun showTexture(id: Long) {
        (activity as? AddUniformActivity)?.startPickTexture(id, samplerType)
    }

    private fun loadTexturesAsync(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val cursor = loadTextures()
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                updateAdapter(context, cursor)
            }
        }
    }

    protected open fun loadTextures(): Cursor {
        return ShaderEditorApp.db.getTextures(searchQuery)
    }

    private fun updateAdapter(context: Context, cursor: Cursor) {
        texturesAdapter?.apply {
            changeCursor(cursor)
            notifyDataSetChanged()
        } ?: run {
            texturesAdapter = TextureAdapter(context, cursor)
            listView.adapter = texturesAdapter
        }

        if (cursor.count < 1) {
            progressBar.visibility = View.GONE
            noTexturesMessage.visibility = View.VISIBLE
        }
    }

    private fun initListView(view: View) {
        listView.apply {
            emptyView = view.findViewById(R.id.no_textures)
            setOnItemClickListener { _, _, _, id -> showTexture(id) }
        }
    }

    override fun onSearch(query: String?) {
        searchQuery = query?.lowercase(Locale.getDefault())
        loadTexturesAsync(requireContext())
    }
}