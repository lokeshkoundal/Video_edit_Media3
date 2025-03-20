package com.lokesh.media3.ar

import android.graphics.SurfaceTexture
import android.view.Surface
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Stream
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.material.setExternalTexture
import io.github.sceneview.safeDestroyStream
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.texture.VideoTexture

/**
 * `VideoMaterial` handles the creation and management of resources needed to render video
 * content onto a Filament scene. It uses a `SurfaceTexture` and `Surface` to receive
 * video frames, a `Stream` to connect the video source to Filament, and a `VideoTexture`
 * to sample the stream within a Filament material.
 *
 * This class provides a convenient way to:
 *  - Create and manage the necessary Android and Filament objects for video rendering.
 *  - Apply a chroma key (if needed) to the video material.
 *  - Handle resource cleanup to prevent memory leaks.
 *
 * @param engine The Filament `Engine` instance.
 * @param materialLoader The `MaterialLoader` responsible for creating the video material instance.
 * @param chromaKeyColor Optional. An integer representing the chroma key color to be applied to the video.
 *                         If null, no chroma key effect will be applied.
 */
class VideoMaterial(
    val engine: Engine,
    materialLoader: MaterialLoader,
    chromaKeyColor: Int? = null
) {
    /**
     * Images drawn to the Surface will be made available to the Filament Stream.
     */
    val surfaceTexture = SurfaceTexture(0).apply {
        detachFromGLContext()
    }

    /**
     * The Android surface.
     */
    val surface = Surface(surfaceTexture)

    /**
     * The Filament Stream.
     */
    val stream = Stream.Builder()
        .stream(surfaceTexture)
        .build(engine)

    /**
     * The Filament Texture diffusing the stream.
     */
    val texture = VideoTexture.Builder()
        .stream(stream)
        .build(engine)

    val instance: MaterialInstance = materialLoader.createVideoInstance(texture, chromaKeyColor)

    init {
        instance.setExternalTexture(texture)
    }

    fun destroy() {
        engine.safeDestroyTexture(texture)
        engine.safeDestroyStream(stream)
        surface.release()
        surfaceTexture.release()
    }
}