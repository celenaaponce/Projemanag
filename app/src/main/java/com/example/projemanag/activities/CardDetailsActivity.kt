package com.example.projemanag.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projemanag.R
import com.example.projemanag.adapters.CardMemberListItemsAdapter
import com.example.projemanag.dialogs.LabelColorListDialog
import com.example.projemanag.dialogs.MemberListDialog
import com.example.projemanag.activities.CardDetailsActivity
import com.example.projemanag.firebase.FirestoreClass
import com.example.projemanag.models.*
import com.example.projemanag.utils.Constants
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class CardDetailsActivity : BaseActivity() {

    private lateinit var mBoardDetails: Board
    private var mTaskListPosition = -1
    private var mCardPosition = -1
    private var mSelectedColor = ""
    private lateinit var mSelectedUser : ArrayList<User>
    private lateinit var mMemberDetailList : ArrayList<User>
    private var mSelectedDueDateMilliSeconds: Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_details)
        getIntentData()
        setupActionBar()

        val etNameCardDetails : EditText = findViewById(R.id.et_name_card_details)
        etNameCardDetails.setText(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
        etNameCardDetails.setSelection(etNameCardDetails.text.toString().length)
        mSelectedColor = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].labelColor
        if(mSelectedColor.isNotEmpty()){
            setColor()
        }
        val btnUpdateCardDetails : Button = findViewById(R.id.btn_update_card_details)
        btnUpdateCardDetails.setOnClickListener {
            if(etNameCardDetails.text.toString().isNotEmpty())
                updateCardDetails()
            else{
                Toast.makeText(this@CardDetailsActivity, "enter name", Toast.LENGTH_SHORT).show()
            }
        }
        val tvSelectedColor : TextView = findViewById(R.id.tv_select_label_color)
        tvSelectedColor.setOnClickListener {
            labelColorsListDialog()
        }

        val tvSelectedUsers: TextView = findViewById(R.id.tv_select_members)
        tvSelectedUsers.setOnClickListener {
            memberListDialog()
        }
        setupSelectedMembersList()
        mSelectedDueDateMilliSeconds = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].dueDate
        if(mSelectedDueDateMilliSeconds > 0){
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate = simpleDateFormat.format(Date(mSelectedDueDateMilliSeconds))
            val tvSelectedDueDate: TextView = findViewById(R.id.tv_select_due_date)
            tvSelectedDueDate.text = selectedDate
        }
        val tvSelectDueDate: TextView = findViewById(R.id.tv_select_due_date)
        tvSelectDueDate.setOnClickListener {
            showDatePicker()
        }
    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun setupActionBar(){
        val toolbarMainActivity : Toolbar = findViewById(R.id.toolbar_card_details_activity)
        setSupportActionBar(toolbarMainActivity)
        val actionBar = supportActionBar
        if(actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_black_color_back_24dp)
            actionBar.title = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name
        }
        toolbarMainActivity.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card, menu)
        return super.onCreateOptionsMenu(menu)
    }


    private fun colorsList(): ArrayList<String>{
        val colorsList: ArrayList<String> = ArrayList()
        colorsList.add("#43C86F")
        colorsList.add("#0C90F1")
        colorsList.add("#F72400")
        colorsList.add("#7A8089")
        colorsList.add("#D57C1D")
        colorsList.add("#770000")
        colorsList.add("#0022F8")
        return colorsList
    }

    private fun setColor(){
        val tvSelectedColor : TextView = findViewById(R.id.tv_select_label_color)
        tvSelectedColor.text = ""
        tvSelectedColor.setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_delete_card ->{
                alertDialogForDeleteCard(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getIntentData(){
        if (intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails = intent.getParcelableExtra(Constants.BOARD_DETAIL)!!
        }
        if (intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)){
            mTaskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION, -1)!!
        }
        if (intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)){
            mCardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION, -1)!!
        }
        if (intent.hasExtra(Constants.BOARD_MEMBERS_LIST)){
            mMemberDetailList = intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
        }

    }

    private fun updateCardDetails(){
        val etNameCardDetails: EditText = findViewById(R.id.et_name_card_details)
        val card = Card(
            etNameCardDetails.text.toString(),
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].createdBy,
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo,
            mSelectedColor,
            mSelectedDueDateMilliSeconds
        )
        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size-1)
        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition] = card
        showProgressDialog("Please wait")
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    private fun deleteCard(){
        val cardsList: ArrayList<Card> = mBoardDetails.taskList[mTaskListPosition].cards
        cardsList.removeAt(mCardPosition)
        val taskList: ArrayList<Task> = mBoardDetails.taskList
        taskList.removeAt(taskList.size -1)
        taskList[mTaskListPosition].cards = cardsList
        showProgressDialog("Please wait")
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity, mBoardDetails)
    }

    private fun alertDialogForDeleteCard(cardName: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Are you sure you want to delete $cardName.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton("Yes") {dialogInterface, which ->
            dialogInterface.dismiss()
            deleteCard()
        }
        builder.setNegativeButton("No") { dialogInterface, which ->
            dialogInterface.dismiss()
        }
        val alertDialog: AlertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun labelColorsListDialog(){
        val colorsList: ArrayList<String> = colorsList()
        val listDialog = object: LabelColorListDialog(
            this,
            colorsList,
            "Select Label Color",
            mSelectedColor
        ){
            override fun onItemSelected(color: String) {
                mSelectedColor = color
                setColor()
            }

        }
        listDialog.show()
    }

    private fun memberListDialog(){
        val cardAssignedMembersList = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo
        if(cardAssignedMembersList.size > 0){
            for(i in mMemberDetailList.indices){
                for(j in cardAssignedMembersList){
                    if(mMemberDetailList[i].id == j){
                        mMemberDetailList[i].selected = true
                    }
                }
            }
        }else{
            for(i in mMemberDetailList.indices){
                for(j in cardAssignedMembersList){
                    if(mMemberDetailList[i].id == j){
                        mMemberDetailList[i].selected = false
                    }
                }
            }
        }

        val listDialog = object: MemberListDialog(
            this,
            mMemberDetailList,
            "Select Member"
        ){
            override fun onItemSelected(user: User, action: String) {
                if(action == Constants.SELECT) {
                    if (!mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.contains(
                            user.id
                        )
                    ) {
                        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.add(
                            user.id
                        )
                    }
                }
                else{
                    mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo.remove(user.id)

                    for(i in mMemberDetailList.indices){
                        if(mMemberDetailList[i].id == user.id){
                            mMemberDetailList[i].selected = false
                        }
                    }
                }
                setupSelectedMembersList()
            }

        }.show()
    }

    private fun setupSelectedMembersList(){
        val cardAssignedMemberList = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo
        val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()

        for(i in mMemberDetailList.indices){
            for(j in cardAssignedMemberList){
                if(mMemberDetailList[i].id == j){
                    val selectedMember = SelectedMembers(
                        mMemberDetailList[i].id,
                        mMemberDetailList[i].image
                    )
                    selectedMembersList.add(selectedMember)
                }
            }
        }
        val tvSelectMembers: TextView = findViewById(R.id.tv_select_members)
        val rvSelectedMembers: RecyclerView = findViewById(R.id.rv_selected_members_list)
        if(selectedMembersList.size > 0){
            selectedMembersList.add(SelectedMembers("", ""))
            tvSelectMembers.visibility = View.GONE
            rvSelectedMembers.visibility = View.VISIBLE
            rvSelectedMembers.layoutManager = GridLayoutManager(
                this,
                6
            )
            val adapter = CardMemberListItemsAdapter(this, selectedMembersList, true)
            rvSelectedMembers.adapter = adapter

            adapter.setOnClickListener(
                object: CardMemberListItemsAdapter.OnClickListener{
                    override fun onClick(){
                        memberListDialog()}
                }
            )
        }else{
            tvSelectMembers.visibility = View.VISIBLE
            rvSelectedMembers.visibility = View.GONE
        }

    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val dpd = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener{ view, year, monthOfYear, dayOfMonth ->
                val sDayOfMonth = if(dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                val sMonthOfYear =
                    if((monthOfYear + 1) < 10) "0${monthOfYear +1}" else "${monthOfYear + 1}"
                val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"
                val tvSelectedDueDate : TextView = findViewById(R.id.tv_select_due_date)
                tvSelectedDueDate.text = selectedDate

                val sdf = SimpleDateFormat("dd/MM/yyy", Locale.ENGLISH)
                val theDate = sdf.parse(selectedDate)
                mSelectedDueDateMilliSeconds = theDate!!.time
            },
            year,
            month,
            day
        )
        dpd.show()
    }

}