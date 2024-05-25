package com.hyeonjs.trafficcardreader

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcF
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.darktornado.library.FeliCa


class MainActivity : Activity() {

    private var adapter: NfcAdapter? = null
    private var intent: PendingIntent? = null
    private var txt: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = 1

        val txt = TextView(this)
        txt.text = "1. Enable NFC on your Smartphone\n" +
                "2. Tag IC Card to Smartphone"
        txt!!.textSize = 18f
        txt.layoutParams = LinearLayout.LayoutParams(-1, -1)
        txt.gravity = Gravity.CENTER or Gravity.CENTER_VERTICAL
        layout.addView(txt)

        setContentView(layout)

        adapter = NfcAdapter.getDefaultAdapter(this);
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        this.intent = PendingIntent.getActivity(this, 0, intent, 0)
        this.txt = txt;
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) ?: return
            val id = IsoDep.get(tag)
            val nf = NfcF.get(tag)

            if (id != null) {
                val card = ICCard(id)
                txt!!.text = "${card.balance}원"
                txt!!.textSize = 22f
            }
            else if (nf != null) {
                val card = FeliCa(nf, tag.id)
                txt!!.text = "${card.balance}엔"
                txt!!.textSize = 22f
            }
            else {
                toast("Cannot read card")
            }
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