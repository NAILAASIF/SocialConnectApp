📱** Social Connect App**

A simple social networking Android application where users can create posts, follow other users, send messages, and manage their profiles. Built using Android Studio, Kotlin, and Firebase.

🚀** Features**

✅ User Authentication (Sign Up, Login, Forgot Password)
✅ Create, View, Edit, and Delete Posts
✅ Upload Images in Posts
✅ User Profiles (Update Name, Bio, Profile Picture)
✅ Follow / Unfollow Users
✅ Basic Chat Messaging System
✅ Search Users
✅ Comment on Posts
✅ Delete Comment
✅ Firebase Firestore & Storage Integration
✅ Smooth UI with RecyclerView & Glide

📂** Project Structure**

SocialConnectApp/
│
├── app/src/main/java/com/example/internshiptask/   # All Activities & Adapters

├── app/src/main/res/                               # Layouts & Drawables

├── screenshots/                                    # App screenshots



🛠 **Tech Stack**

Language: Kotlin

IDE: Android Studio

Database: Firebase Firestore

Storage: Firebase Storage

Authentication: Firebase Auth

⚙️ **Setup Instructions**

1. Clone this Repository
   git clone: [(https://github.com/NAILAASIF/SocialConnectApp)](https://github.com/NAILAASIF/SocialConnectApp.git)
2. Open in Android Studio
   Open Android Studio
   Click Open an existing project
   Select the SocialConnectApp folder

4. Connect Firebase
   Go to Tools > Firebase in Android Studio
   Connect to Firebase project
    **Enable:**
   -> Authentication
   ->Firestore Database
   
  ** Firebase rules:**
   
   rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    //  Users Collection
    match /users/{userId} {
      // Signed-in users can read user profiles
      allow read: if request.auth != null;

      // Users can only update their own profile
      allow write: if request.auth != null && request.auth.uid == userId;

      //  Followers of this user
      match /followers/{followerId} {
        allow read, write: if request.auth != null;
      }

      // Users that this user is following
      match /following/{followingId} {
        allow read, write: if request.auth != null;
      }
    }

    //  Posts Collection
    match /posts/{postId} {
      // All signed-in users can read posts
      allow read: if request.auth != null;

      // Only the post owner can update or delete
      allow create: if request.auth != null;
       allow update: if request.auth != null && request.resource.data.userId == request.auth.uid;
      allow delete: if request.auth != null && resource.data.userId == request.auth.uid;

      // Comments under each post
      match /comments/{commentId} {
        allow read, write: if request.auth != null;
      }
    }
    //  Chats Collection
    match /chats/{chatId} {
      allow read, write: if request.auth != null;

      //  Messages inside each chat
      match /messages/{messageId} {
        allow read, write: if request.auth != null;
      }
    }
  }
}

   

4. Add Your google-services.json
   Download google-services.json from Firebase Console
   Place it in:
   app/
5. Sync & Run
   Click Sync Now

Run the app on an emulator or a physical device

▶️** Video Demonstration**

[Add your demo video link here – [Google Drive or YouTube](https://drive.google.com/file/d/1MC24zA9UM5C0ox-8SKazLfvh5xrhxbV9/view?t=13)]

📥 **APK Download**

[Download APK](apk/app-release.apk)

👩‍💻 Author

->Naila Asif
🌐 GitHub Profile:https://github.com/NAILAASIF
