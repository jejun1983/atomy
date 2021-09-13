package com.idevel.momschoice.activity

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.idevel.momschoice.R
import com.idevel.momschoice.utils.SharedPreferencesUtil

class DevActivity : AppCompatActivity(), View.OnClickListener {
    private var buttonOk: Button? = null
    private var textviewServerUrl: TextView? = null
    private var buttonServerChange: Button? = null
    var urlList = ArrayList<String>()

    companion object {
        private const val DEV_SETTING_URL_1 = "http://d-app.bottleletter.co.kr/app/version.php"             // 개발
        private const val DEV_SETTING_URL_2 = "http://app.bottleletter.co.kr/app/version.php"               // 상용
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dev)

        initView()
        initURL()
    }

    private fun initView() {
        buttonOk = findViewById<View>(R.id.button) as Button
        buttonServerChange = findViewById<View>(R.id.btn_server_change) as Button
        textviewServerUrl = findViewById<View>(R.id.textview_server_url) as TextView

        buttonOk?.setOnClickListener(this)
        buttonServerChange?.setOnClickListener(this)
    }

    private fun initURL() {
        urlList.add(DEV_SETTING_URL_1)
        urlList.add(DEV_SETTING_URL_2)

        var url: String? = SharedPreferencesUtil.getString(this, SharedPreferencesUtil.Cmd.SETTING_URL)

        if (null == url || url.equals("", ignoreCase = true)) {
            url = DEV_SETTING_URL_1
        }

        textviewServerUrl?.text = url
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button -> {
                val url = textviewServerUrl?.text.toString()
                if (null == url || url.isEmpty()) {
                    Toast.makeText(this, "서버주소를선택하세요", Toast.LENGTH_SHORT).show()
                    return
                }

                SharedPreferencesUtil.setString(this, SharedPreferencesUtil.Cmd.SETTING_URL, textviewServerUrl?.text.toString())

                finish()
            }

            R.id.btn_server_change -> showSelectDialog("URL", urlList, textviewServerUrl)
        }
    }

    private fun showSelectDialog(title: String, list: ArrayList<String>, textView: TextView?) {
        val alertBuilder = AlertDialog.Builder(this@DevActivity)
        alertBuilder.setTitle("$title 선택")

        // List Adapter 생성
        val adapter = ArrayAdapter<String>(this@DevActivity, android.R.layout.select_dialog_singlechoice)

        for (s in list) {
            adapter.add(s)
        }

        adapter.add("직접입력")

        // 버튼 생성
        alertBuilder.setNegativeButton("취소") { dialog, _ -> dialog.dismiss() }

        // Adapter 셋팅
        alertBuilder.setAdapter(adapter) { _, id ->
            // AlertDialog 안에 있는 AlertDialog
            val strName = adapter.getItem(id)

            if ("직접입력".equals(strName, ignoreCase = true)) {
                showEtcDialog(title, textView)
            } else {
                textView?.text = strName
            }
        }

        alertBuilder.show()
    }

    private fun showEtcDialog(title: String, textView: TextView?) {
        val alertBuilder = AlertDialog.Builder(this@DevActivity)
        alertBuilder.setTitle("$title 입력")

        val edit = EditText(this)
        edit.setText(textView?.text)
        alertBuilder.setView(edit)

        alertBuilder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }
        alertBuilder.setPositiveButton("확인") { dialog, _ ->
            textView?.text = edit.text.toString()
            dialog.dismiss()
        }

        alertBuilder.show()
    }
}