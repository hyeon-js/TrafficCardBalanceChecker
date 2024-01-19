package com.hyeonjs.trafficcardreader

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast


class MainActivity : Activity() {

    private var adapter: NfcAdapter? = null
    private var intent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = 1

        val txt = TextView(this)
        txt.text = "1. Enable NFC on your Smartphone\n" +
                "2. Tag IC Card to Smartphone"
        txt.layoutParams = LinearLayout.LayoutParams(-1, -1)
        txt.gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
        layout.addView(txt)

        setContentView(layout)

        adapter = NfcAdapter.getDefaultAdapter(this);
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        this.intent = PendingIntent.getActivity(this, 0, intent, 0)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return
            val id = IsoDep.get(tag) ?: return
            val card = ICCard(id)
            toast("${card.balance}원")
        } catch (e: Exception) {
            toast(e.toString())
        }
    }


    override fun onResume() {
        super.onResume()
        if (adapter != null) adapter!!.enableForegroundDispatch(this, intent, null, null)
    }

    override fun onPause() {
        super.onPause()
        if (adapter != null) adapter!!.disableForegroundDispatch(this)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

}