package com.example.internshiptask

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.internshiptask.adapters.ChatAdapter
import com.example.internshiptask.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var sendBtn: Button
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messagesAdapter: ChatAdapter
    private lateinit var receiverNameText: TextView
    private lateinit var receiverImageView: ImageView

    private var receiverId: String? = null
    private lateinit var senderId: String
    private val messages = mutableListOf<Message>()

    private var receiverImageBase64: String = ""
    private var senderImageBase64: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // ✅ Get Receiver Details from Intent
        receiverId = intent.getStringExtra("receiverId")
        val receiverName = intent.getStringExtra("receiverName")
        receiverImageBase64 = intent.getStringExtra("receiverImage") ?: ""

        if (receiverId.isNullOrEmpty()) {
            Toast.makeText(this, "Receiver ID missing!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        senderId = FirebaseAuth.getInstance().uid ?: ""
        if (senderId.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ✅ Initialize Views
        receiverNameText = findViewById(R.id.receiverNameText)
        receiverImageView = findViewById(R.id.receiverImageView)
        editText = findViewById(R.id.messageEditText)
        sendBtn = findViewById(R.id.sendmsgBtn)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)

        listenForReceiverUpdates()


        // ✅ Fetch Sender Profile Image from Firestore
        fetchSenderImage()

        // ✅ Setup RecyclerView
        messagesAdapter = ChatAdapter(messages, senderId, senderImageBase64, receiverImageBase64)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messagesAdapter

        sendBtn.setOnClickListener {
            val text = editText.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                editText.setText("")
            }
        }

        listenForMessages()
    }

    private fun fetchSenderImage() {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(senderId)
            .get()
            .addOnSuccessListener { doc ->
                senderImageBase64 = doc.getString("profileImageBase64") ?: ""
                // ✅ Update adapter with sender image
                messagesAdapter = ChatAdapter(messages, senderId, senderImageBase64, receiverImageBase64)
                messagesRecyclerView.adapter = messagesAdapter
            }
            .addOnFailureListener {
                senderImageBase64 = ""
            }
    }

    private fun sendMessage(text: String) {
        val message = Message(
            senderId = senderId,
            receiverId = receiverId!!,
            messageText = text,
            timestamp = System.currentTimeMillis()
        )

        val chatId = getChatId(senderId, receiverId!!)
        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
    }

    private fun listenForMessages() {
        val chatId = getChatId(senderId, receiverId!!)
        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                messages.clear()
                for (doc in snapshot.documents) {
                    val message = doc.toObject(Message::class.java)
                    if (message != null) messages.add(message)
                }
                messagesAdapter.notifyDataSetChanged()
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }
    }

    private fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "$uid1-$uid2" else "$uid2-$uid1"
    }

    private fun setImageFromBase64(base64String: String, imageView: ImageView) {
        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            imageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            imageView.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }
    private fun listenForReceiverUpdates() {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(receiverId!!)
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener

                if (doc != null && doc.exists()) {
                    val updatedName = doc.getString("name") ?: "Chat"
                    receiverNameText.text = updatedName

                    val updatedImage = doc.getString("profileImageBase64") ?: ""
                    if (updatedImage.isNotEmpty()) {
                        receiverImageBase64 = updatedImage
                        setImageFromBase64(updatedImage, receiverImageView)
                    } else {
                        receiverImageView.setImageResource(R.drawable.ic_launcher_foreground)
                    }

                    // ✅ Update adapter to reflect new receiver image in messages
                    messagesAdapter = ChatAdapter(messages, senderId, senderImageBase64, receiverImageBase64)
                    messagesRecyclerView.adapter = messagesAdapter
                }
            }
    }

}
