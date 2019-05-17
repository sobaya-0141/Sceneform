package sobaya.app.sceneform

import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File.separator
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.graphics.Bitmap
import java.nio.file.Files.exists
import android.widget.Toast
import android.content.Intent
import androidx.core.content.FileProvider
import android.view.PixelCopy
import android.os.HandlerThread
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.os.Environment
import android.os.Handler
import com.google.ar.sceneform.ArSceneView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var fragment: ArFragment
    private val pointer = PointerDrawable()
    private var isTracking = false
    private var isHitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fragment = supportFragmentManager.findFragmentById(R.id.scene_fragment) as ArFragment

        fab.setOnClickListener { view ->
            takePhoto()
        }

        fragment.arSceneView.scene.addOnUpdateListener {
            onUpdate()
        }

        initializeGallery()
    }

    private fun initializeGallery() {
        val gallery = findViewById<LinearLayout>(R.id.gallery_layout)

        val andy = ImageView(this)
        with (andy) {
            setImageResource(R.drawable.droid_thumb)
            contentDescription = "andy"
            setOnClickListener {
                addObject(Uri.parse("andy.sfb"))
            }
            gallery.addView(this)
        }

        val cabin = ImageView(this)
        with (cabin) {
            setImageResource(R.drawable.cabin_thumb)
            contentDescription = "cabin"
            setOnClickListener {
                addObject(Uri.parse("Cabin.sfb"))
            }
            gallery.addView(this)
        }

        val house = ImageView(this)
        with (house) {
            setImageResource(R.drawable.house_thumb)
            contentDescription = "house"
            setOnClickListener {
                addObject(Uri.parse("House.sfb"))
            }
            gallery.addView(this)
        }

        val igloo = ImageView(this)
        with (igloo) {
            setImageResource(R.drawable.igloo_thumb)
            contentDescription = "igloo"
            setOnClickListener {
                addObject(Uri.parse("igloo.sfb"))
            }
            gallery.addView(this)
        }
    }

    private fun addObject(model: Uri) {
        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null) {
            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    placeObject(fragment, hit.createAnchor(), model)
                    break
                }
            }
        }
    }

    private fun placeObject(fragment: ArFragment, anchor: Anchor, model: Uri) {
        val renderableFuture = ModelRenderable.builder()
                .setSource(fragment.context!!, model)
                .build()
                .thenAccept { renderable -> addNodeToScene(fragment, anchor, renderable) }
                .exceptionally { throwable ->
                    val builder = AlertDialog.Builder(this)
                    builder.setMessage(throwable.message)
                            .setTitle("Codelab error!")
                    val dialog = builder.create()
                    dialog.show()
                    null
                }
    }

    private fun addNodeToScene(fragment: ArFragment, anchor: Anchor, renderable: Renderable) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(anchorNode)
        node.select()
    }

    private fun onUpdate() {
        val trackingChanged = updateTracking()
        val contentView = findViewById<View>(android.R.id.content)

        if (trackingChanged) {
            if (isTracking) {
                contentView.overlay.add(pointer)
            } else {
                contentView.overlay.remove(pointer)
            }
            contentView.invalidate()
        }

        if (isTracking) {
            val hitTestChanged = updateHitTest()
            if (hitTestChanged) {
                pointer.enabled = isHitting
                contentView.invalidate()
            }
        }
    }

    private fun updateTracking(): Boolean {
        val frame = fragment.arSceneView.arFrame
        val wasTracking = isTracking

        isTracking = frame?.camera?.trackingState == TrackingState.TRACKING

        return isTracking != wasTracking
    }

    private fun updateHitTest(): Boolean {
        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        val wasHitting = isHitting

        isHitting = false

        frame?.let { frame ->
            frame.hitTest(pt.x.toFloat(), pt.y.toFloat()).forEach { hit ->
                val trackable = hit.trackable
                if (trackable is Plane && (trackable as Plane).isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    return@forEach
                }
            }
        }
        return wasHitting != isHitting
    }

    private fun getScreenCenter(): Point {
        val view = findViewById<View>(android.R.id.content)
        return Point(view.width / 2, view.height / 2)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).path + File.separator + "Sceneform/" + date + "_screenshot.jpg"
    }

    @Throws(IOException::class)
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {

        val out = File(filename)
        if (!out.getParentFile().exists()) {
            out.getParentFile().mkdirs()
        }
        try {
            FileOutputStream(filename).use({ outputStream ->
                ByteArrayOutputStream().use({ outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                })
            })
        } catch (ex: IOException) {
            throw IOException("Failed to save bitmap to disk", ex)
        }

    }

    private fun takePhoto() {
        val filename = generateFilename()
        val view = fragment.arSceneView

        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(view.width, view.height,
                Bitmap.Config.ARGB_8888)

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult === PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename)
                    val snackbar = Snackbar.make(findViewById(android.R.id.content),
                            "Photo saved", Snackbar.LENGTH_LONG)
                    snackbar.setAction("Open in Photos") { v ->
                        val photoFile = File(filename)

                        val photoURI = FileProvider.getUriForFile(this@MainActivity,
                                this@MainActivity.packageName + ".ar.codelab.name.provider",
                                photoFile)
                        val intent = Intent(Intent.ACTION_VIEW, photoURI)
                        intent.setDataAndType(photoURI, "image/*")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(intent)

                    }
                    snackbar.show()
                } catch (e: IOException) {
                    val toast = Toast.makeText(this@MainActivity, e.toString(),
                            Toast.LENGTH_LONG)
                    toast.show()
                }
            } else {
                val toast = Toast.makeText(this@MainActivity,
                        "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }
}
