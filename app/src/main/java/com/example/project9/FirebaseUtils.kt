package com.example.project9
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.util.concurrent.CompletableFuture

object FirebaseUtils {
    private val storageRef: StorageReference = FirebaseStorage.getInstance().reference

    fun uploadImage(bitmap: Bitmap, folderPath: String, fileName: String): CompletableFuture<Uri> {
        val future = CompletableFuture<Uri>()
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val imageRef = storageRef.child("$folderPath/$fileName.jpg")
        val uploadTask = imageRef.putBytes(data)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                future.complete(uri)
            }
        }.addOnFailureListener { exception ->
            Log.e("FirebaseUtils", "Upload failed", exception)
            future.completeExceptionally(exception)
        }

        return future
    }

    fun listImages(folderPath: String): CompletableFuture<List<String>> {
        val future = CompletableFuture<List<String>>()
        storageRef.child(folderPath).listAll().addOnSuccessListener { listResult ->
                val urls = mutableListOf<String>()
                listResult.items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        urls.add(uri.toString())
                        if (urls.size == listResult.items.size) {
                            future.complete(urls)
                        }
                    }.addOnFailureListener { exception ->
                        Log.e("FirebaseUtils", "Failed to get download URL", exception)
                        future.completeExceptionally(exception)
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("FirebaseUtils", "Failed to list images", exception)
                future.completeExceptionally(exception)
            }

        return future
    }
}