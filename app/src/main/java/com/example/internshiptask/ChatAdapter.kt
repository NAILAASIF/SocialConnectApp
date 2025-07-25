package com.example.internshiptask.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.internshiptask.R
import com.example.internshiptask.models.Message

class ChatAdapter(
    private val messages: List<Message>,
    private val currentUserId: String,
    private val senderImageBase64: String,
    private val receiverImageBase64: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == VIEW_TYPE_SENT)
            R.layout.item_message_sender
        else
            R.layout.item_message_reciever

        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view, viewType)
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as MessageViewHolder).bind(messages[position])
    }

    inner class MessageViewHolder(itemView: View, private val viewType: Int) :
        RecyclerView.ViewHolder(itemView) {

        private val messageText: TextView = itemView.findViewById(R.id.textMessage)
        private val userImage: ImageView = itemView.findViewById(R.id.messageUserImage)

        fun bind(message: Message) {
            messageText.text = message.messageText

            val imageBase64 = if (viewType == VIEW_TYPE_SENT) senderImageBase64 else receiverImageBase64
            if (imageBase64.isNotEmpty()) {
                try {
                    val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    userImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    userImage.setImageResource(R.drawable.ic_launcher_foreground)
                }
            } else {
                userImage.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
    }
}
