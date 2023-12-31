package com.example.projemanag.firebase

import android.app.Activity
import android.provider.MediaStore.Audio.Genres.Members
import android.util.Log
import android.widget.Toast
import com.example.projemanag.activities.*
import com.example.projemanag.models.Board
import com.example.projemanag.models.Task
import com.example.projemanag.models.User
import com.example.projemanag.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass {

    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity, userInfo: User){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId()).set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }
    }

    fun createBoard(activity: CreateBoardActivity, boardInfo: Board){
        mFireStore.collection(Constants.BOARDS)
            .document().set(boardInfo, SetOptions.merge())
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "Board successful")
                Toast.makeText(activity, "Board successful", Toast.LENGTH_SHORT).show()
                activity.boardCreatedSuccessfully()
            }.addOnFailureListener {
                exception ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "Error", exception)
            }
    }

    fun getBoardsList(activity: MainActivity){
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO, getCurrentUserId())
            .get()
            .addOnSuccessListener {
                document ->
                Log.i(activity.javaClass.simpleName, document.documents.toString())
                val boardList: ArrayList<Board> = ArrayList()
                for(i in document.documents){
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardList.add(board)
                }

                activity.populateBoardsListToUI(boardList)
            }.addOnFailureListener { e->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "error", e)
            }
    }

    fun addUpdateTaskList(activity: Activity, board: Board){
        val taskListHashMap = HashMap<String, Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {
                Log.e(activity.javaClass.simpleName, "updated successfully")
                if(activity is TaskListActivity){
                    activity.addUpdateTaskListSuccess()}
                else if (activity is CardDetailsActivity){
                    activity.addUpdateTaskListSuccess()
                }
            }.addOnFailureListener {
                exception ->
                if(activity is TaskListActivity)
                    activity.hideProgressDialog()
                else if (activity is CardDetailsActivity)
                    activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "error", exception)
            }
    }

    fun updateUserData(activity: Activity, userHashMap: HashMap<String, Any>){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId()).update(userHashMap).addOnSuccessListener {
                Log.e("Firestore", "Successful")
                Toast.makeText(activity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                when(activity){
                    is MainActivity ->{
                        activity.tokenUpdateSuccess()
                    }
                    is ProfileActivity->{
                activity.profileUpdateSuccess()}
                }
            }.addOnFailureListener{
                e -> when(activity){
                is MainActivity -> {
                    activity.hideProgressDialog()
                }
                is ProfileActivity ->{
                    activity.hideProgressDialog()}
                }

            Log.e("Firestore", "Error", e)
            Toast.makeText(activity, "Profile update error", Toast.LENGTH_SHORT).show()
            }
    }


    fun updateBoard(activity: CreateBoardActivity, boardHashMap: HashMap<String, Any>){
        mFireStore.collection(Constants.BOARDS)
            .document().update(boardHashMap).addOnSuccessListener {
                Log.e("Firestore", "Successful")
                Toast.makeText(activity, "Board updated successfully", Toast.LENGTH_SHORT).show()
                activity.boardUpdateSuccess()
            }.addOnFailureListener{
                    e -> activity.hideProgressDialog()
                Log.e("Firestore", "Error", e)
                Toast.makeText(activity, "Board update error", Toast.LENGTH_SHORT).show()

            }
    }

    fun loadUserData(activity: Activity, readBoardsList: Boolean = false){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId()).get()
            .addOnSuccessListener {document ->
                Log.e("loadFirestore", "$readBoardsList")
                val loggedInUser = document.toObject(User::class.java)!!
                when(activity){
                    is SignInActivity ->{activity.signInSuccess(loggedInUser)}
                    is MainActivity ->{activity.updateNavigationUserDetails(loggedInUser, readBoardsList)}
                    is ProfileActivity -> {activity.setUserDataInUI(loggedInUser)}
                }

            }.addOnFailureListener {
                e ->
                when(activity){
                    is SignInActivity ->{activity.hideProgressDialog()}
                    is MainActivity ->{activity.hideProgressDialog()}
                    is ProfileActivity -> {activity.hideProgressDialog()}
                }
                Log.e("SignInUser", "error", e)
            }
    }

    fun getCurrentUserId(): String{
        var currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if (currentUser != null){
            currentUserID = currentUser.uid
        }
        return currentUserID
    }

    fun getBoardDetails(activity: TaskListActivity, documentId: String) {
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener { document ->
                Log.e(activity.javaClass.simpleName, document.toString())

                // TODO (Step 1: Assign the board document id to the Board Detail object)
                // START
                val board = document.toObject(Board::class.java)!!
                board.documentId = document.id

                // Send the result of board to the base activity.
                activity.boardDetails(board)
                // END
            }.addOnFailureListener { e->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName, "error", e)
            }
    }

    fun getAssignedMembersListDetails(activity: Activity, assignedTo: ArrayList<String>){
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID, assignedTo)
            .get()
            .addOnSuccessListener {
                document ->
                Log.e(activity.javaClass.simpleName, document.documents.toString())

                val usersList: ArrayList<User> = ArrayList()

                for(i in document.documents){
                    val user = i.toObject(User::class.java)!!
                    usersList.add(user)
                }
                if (activity is MembersActivity)
                    activity.setupMemberList(usersList)
                else if (activity is TaskListActivity)
                    activity.boardMembersDetailsList(usersList)
            }.addOnFailureListener { e->
                if (activity is MembersActivity)
                    activity.hideProgressDialog()
                else if (activity is TaskListActivity)
                    activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "error", e
                )
            }
    }

    fun getMemberDetails(activity: MembersActivity, email: String){
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL, email)
            .get()
            .addOnSuccessListener {
                document ->
                if(document.documents.size > 0){
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                }else{
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such member found")
                }
            }.addOnFailureListener { e->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "error", e
                )
            }
    }

    fun assignMemberToBoard(activity: MembersActivity, board: Board, user: User){
        val assignedToHashMap = HashMap<String, Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }.addOnFailureListener { e->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "error", e
                )
            }

    }
}
