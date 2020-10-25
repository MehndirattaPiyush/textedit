package com.easypeasy.textedit

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.easypeasy.textedit.db.AppDatabase
import com.easypeasy.textedit.db.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val database: AppDatabase) : ViewModel() {

    val wordsLiveData = MutableLiveData<String>().apply {
        value = "0 word"
    }

    val textLiveData = MutableLiveData<String>().apply {
        value = ""
    }

    val isEnabled = MutableLiveData<Boolean>().apply { value = false }

    // Function to count total number
    // of words in the string,
    // words has to have at least one english letter.
    private fun countWords(str: String?): Int {

        if (str == null || str.isEmpty()) return 0
        var wordCount = 0
        var isWord = false
        val endOfLine = str.length - 1

        val ch = str.toCharArray()
        for (i in ch.indices) {

            if (Character.isLetter(ch[i])
                && i != endOfLine
            ) isWord = true else if (!Character.isLetter(ch[i])
                && isWord
            ) {
                wordCount++
                isWord = false
            } else if (Character.isLetter(ch[i])
                && i == endOfLine
            ) wordCount++
        }

        return wordCount
    }

    // method to update database
    fun update(b: Boolean, str: String) {
        if (!b) {
            viewModelScope.launch(Dispatchers.IO) {
                val countText = getCountText(str)
                withContext(Dispatchers.Main) {
                    wordsLiveData.value = countText
                    isEnabled.value = true
                }
                database.entryDao().insert(
                    Entry(
                        text = str,
                        count = countText,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } else {
            isEnabled.value = false
        }
    }

    // utility method count text
    private fun getCountText(str: String): String {
        val i = countWords(str)
        return "$i word${shouldAppendS(i)}"
    }

    private fun shouldAppendS(i: Int): String {
        return if (i > 1) "s" else ""
    }

    fun performUndo(enabled: Boolean, curString: String) {

        if (!enabled) return
        viewModelScope.launch(Dispatchers.IO) {
            val pair = getLastCommit(curString)
            val s = pair.first

            withContext(Dispatchers.Main) {
                wordsLiveData.value = s?.count ?: "0 word"
                textLiveData.value = s?.text ?: ""
                isEnabled.value = pair.second
            }
        }
    }

    private fun getLastCommit(curString: String): Pair<Entry?, Boolean> {
        var entry: Entry? = null
        val list = database.entryDao().getLastEntry()
        if (list.isNotEmpty()) {
            for (element in list) {
                if (element != null) {
                    Log.d("vbn", element.text)
                    if (element.text != curString) {
                        entry = element
                        break
                    }
                    database.entryDao().delete(element)
                }

            }
        }

        return Pair(entry, database.entryDao().getLastEntry().isNotEmpty())
    }

    fun checkAndSetData() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = database.entryDao().getLastEntry()
            if (list.isNotEmpty()) {
                val entry = list[0]
                withContext(Dispatchers.Main) {
                    wordsLiveData.value = entry?.count ?: "0 word"
                    textLiveData.value = entry?.text ?: ""
                    isEnabled.value = entry != null
                }
            }
        }
    }
}

// Class needed to inject [AppDatabase] in [MainViewModel]
class MainViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}