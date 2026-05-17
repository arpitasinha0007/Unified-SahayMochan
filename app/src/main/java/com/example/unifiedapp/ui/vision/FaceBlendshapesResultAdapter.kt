package com.example.unifiedapp.ui.vision
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mediapipe.tasks.components.containers.Category

class FaceBlendshapesFragment : Fragment() {

    // A state-backed list that Compose will observe
    private var blendshapesState = mutableStateOf<List<Category?>>(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                FaceBlendshapesScreen(categories = blendshapesState.value)
            }
        }
    }

    // This replaces your old 'updateResults' method
    fun updateResults(newCategories: MutableList<Category?>) {
        // Updating this value triggers an immediate, efficient UI refresh
        blendshapesState.value = newCategories
    }
}

@Composable
fun FaceBlendshapesScreen(
    categories: List<Category?>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Using categoryName as a key improves performance for dynamic updates
        items(
            items = categories,
            key = { it?.categoryName() ?: it.hashCode() }
        ) { category ->
            BlendshapeRow(
                label = category?.categoryName(),
                score = category?.score()
            )
        }
    }
}

@Composable
private fun BlendshapeRow(label: String?, score: Float?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label ?: "--",
            fontSize = 14.sp,
            color = Color.Black, // Map to your theme color
            modifier = Modifier.weight(1f)
        )
        Text(
            text = score?.let { String.format("%.2f", it) } ?: "--",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}