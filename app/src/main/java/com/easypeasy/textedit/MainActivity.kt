package com.easypeasy.textedit

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.easypeasy.textedit.databinding.ActivityMainBinding
import com.easypeasy.textedit.db.AppDatabase
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    interface OnClickListeners {
        fun onUndoCLick(v: View, isEnabled: Boolean)
        fun onRemoveFocusClick(v: View)
    }

    private val TEXT = "text"
    private val COUNT = "count"
    private val CURSOR = "cursor"

    lateinit var binding: ActivityMainBinding

    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "entry-database"
        ).build()

        viewModel =
            ViewModelProvider(this, MainViewModelFactory(db)).get(
                MainViewModel::class.java
            )

        savedInstanceState?.let {
            val text = it.getString(TEXT)
            val count = it.getString(COUNT)
            val cursor = it.getInt(CURSOR)
            etInput.setText(text)
            tvWordCount.text = count
            etInput.setSelection(cursor)
        } ?: viewModel.checkAndSetData()

        etInput.setOnFocusChangeListener { _, b ->
            viewModel.update(b, etInput.text.toString())
        }

        binding.listener = object : OnClickListeners {
            override fun onUndoCLick(v: View, isEnabled: Boolean) {
                if (!isEnabled) Toast.makeText(
                    this@MainActivity,
                    "Nothing to undo or view in focus",
                    Toast.LENGTH_LONG
                ).show()

                viewModel.performUndo(isEnabled, etInput.text.toString())
            }

            override fun onRemoveFocusClick(v: View) {
                etInput.clearFocus()
                hideKeyBoard(etInput)
            }
        }

        viewModel.wordsLiveData.observe(this, Observer {
            tvWordCount.text = it
        })

        viewModel.textLiveData.observe(this, Observer {
            etInput.setText(it)
            if (etInput.hasFocus() && it.isNotEmpty())
                etInput.setSelection(it.length)
        })

        viewModel.isEnabled.observe(this, Observer {
            binding.isEnabled = it && !etInput.hasFocus()
            binding.executePendingBindings()
        })
    }

    override fun onStop() {
        super.onStop()
        if (etInput.text.toString().isNotEmpty())
            viewModel.update(false, etInput.text.toString())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putString(TEXT, etInput.text.toString())
            putString(COUNT, tvWordCount.text.toString())
            putInt(CURSOR, etInput.selectionStart)
        }
    }

    fun hideKeyBoard(myView: View?) {
        // Check if no view has focus:
        try {
            var view: View? = null
            try {
                view = currentFocus
            } catch (e: Exception) {
                view = myView
            } finally {
                if (view == null) {
                    view = myView
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view!!.windowToken, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}