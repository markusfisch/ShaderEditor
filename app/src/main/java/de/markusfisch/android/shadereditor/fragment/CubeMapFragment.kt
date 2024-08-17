package de.markusfisch.android.shadereditor.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import de.markusfisch.android.shadereditor.R
import de.markusfisch.android.shadereditor.activity.AbstractSubsequentActivity
import de.markusfisch.android.shadereditor.widget.CubeMapView

class CubeMapFragment : Fragment() {

    interface CubeMapViewProvider {
        fun getCubeMapView(): CubeMapView
    }

    private lateinit var cubeMapView: CubeMapView
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { imageUri ->
                cubeMapView.setSelectedFaceImage(imageUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val activity = requireActivity()
        activity.title = getString(R.string.compose_sampler_cube)

        cubeMapView = (activity as? CubeMapViewProvider)?.getCubeMapView()
            ?: throw IllegalArgumentException("$activity must implement CubeMapViewProvider")

        val view = inflater.inflate(R.layout.fragment_cube_map, container, false).apply {
            findViewById<View>(R.id.add_texture).setOnClickListener { addTexture() }
            findViewById<View>(R.id.crop).setOnClickListener { composeMap() }
        }

        cubeMapView.visibility = View.VISIBLE

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_crop_image, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.rotate_clockwise -> {
                        rotateClockwise()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return view
    }

    private fun composeMap() {
        val faces = cubeMapView.faces

        if (faces.any { it.uri == null }) {
            Toast.makeText(
                requireContext(), R.string.not_enough_faces, Toast.LENGTH_SHORT
            ).show()
            return
        }

        AbstractSubsequentActivity.addFragment(
            parentFragmentManager, SamplerCubePropertiesFragment.newInstance(faces)
        )

        cubeMapView.visibility = View.GONE
    }

    private fun rotateClockwise() {
        cubeMapView.imageRotation = (cubeMapView.imageRotation + 90) % 360
    }

    private fun addTexture() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }

        pickImageLauncher.launch(Intent.createChooser(intent, getString(R.string.choose_image)))
    }
}